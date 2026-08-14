package com.orion.app.books.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.io.IOException

// cached_tags and book_series were both previously missing here: without the former,
// book.categories (genres) was always empty for every single book (no genre badges on the
// detail page, an always-empty genre picker in BooksLibraryScreen/SeeAllScreen, and an unusable
// "same genre" filter in getSimilarBooks); without the latter, book.seriesId/seriesName were
// always null (BookDetailScreen's "Series" tab could therefore never show, hasSeriesSignal
// always being false) and getSeriesBooks couldn't filter by series.
//
// The `editions(...)` sub-query targeting French (ISO code "fr") was also missing despite
// toHardcoverBook()'s comment claiming to rely on it ("see BOOK_FIELDS"): without it,
// `frenchOverride` was always null everywhere BOOK_FIELDS is used (search, trending, detail,
// similar, series...), so every displayed date/cover/title was systematically Hardcover's
// "default" edition (often the English or original one), never the French one.
// `book_series.compilation` and `books_count` (on the linked series) are added to make box-set
// filtering and picking the right series more reliable, respectively, when a book is linked to
// several groupings.
//
// IMPORTANT: the `language: {code2: {_eq: "fr"}}` filter below is intentional and must stay:
// French editions are prioritized on purpose throughout this app (see also
// preferFrenchDuplicates and hasFrenchEdition).
private const val BOOK_FIELDS = """
    id title subtitle description pages release_date release_year rating slug
    canonical_id users_count cached_tags compilation
    image { url }
    contributions { author { id name } }
    book_series { position compilation series { id name books_count } }
    editions(where: {language: {code2: {_eq: "fr"}}}, order_by: {release_date: asc}, limit: 1) {
        release_date release_year pages title subtitle
        image { url }
    }
"""

// Kept as a safety net in addition to the `compilation` field (reliable on Hardcover's side):
// some older or miscategorized box sets may not have this boolean up to date
// (e.g. "Berserk: Ultimate Edition", which bundles several volumes into a single book but
// wasn't flagged `compilation = true` on Hardcover's side).
private val BOX_SET_KEYWORDS = listOf(
    "box set", "boxset", "collector's edition", "collector", "deluxe edition",
    "ultimate edition", "omnibus", "anthology", "complete collection", "bundle"
)

/** Fallback keyword-based box-set detection, used alongside Hardcover's own `compilation` flag. */
private fun HardcoverBook.looksLikeBoxSet(): Boolean {
    val t = name.lowercase()
    return BOX_SET_KEYWORDS.any { t.contains(it) }
}

/** True if [reference] has no known authors (nothing to compare against) or shares at least one author (case-insensitive) with this book. */
private fun HardcoverBook.sharesAuthorWith(reference: HardcoverBook): Boolean {
    if (reference.authors.isEmpty() || authors.isEmpty()) return true
    return authors.any { a -> reference.authors.any { it.equals(a, ignoreCase = true) } }
}

/**
 * Deduplicates books that represent the same series volume (same seriesId + seriesOrderNumber),
 * keeping the one with a known French edition when one exists in the group. Books without a
 * series position pass through unchanged.
 */
private fun List<HardcoverBook>.preferFrenchDuplicates(): List<HardcoverBook> {
    fun key(b: HardcoverBook) = if (b.seriesId != null && b.seriesOrderNumber != null) b.seriesId to b.seriesOrderNumber else null
    val bestByKey = groupBy(::key)
        .filterKeys { it != null }
        .mapValues { (_, group) -> group.firstOrNull { it.hasFrenchEdition } ?: group.first() }
    val emitted = mutableSetOf<Pair<String, Int>>()
    return mapNotNull { b ->
        val k = key(b) ?: return@mapNotNull b
        if (!emitted.add(k)) return@mapNotNull null
        bestByKey[k]
    }
}

/**
 * Repository for the Books domain, backed by the Hardcover GraphQL API and a local Room
 * database (followed/favorite/read books). Mirrors CinemaRepository/GamesRepository's shape.
 *
 * Note on language: this repository intentionally prioritizes French editions (title, cover,
 * release date) wherever Hardcover exposes per-edition data, via the `editions(...)` sub-query
 * in [BOOK_FIELDS] and [preferFrenchDuplicates]. This is independent from the app's English UI
 * text and must be preserved.
 */
class BooksRepository(
    private val api: HardcoverApi,
    private val db: BooksDatabase
) {
    /** Runs a raw GraphQL query against Hardcover, turning API-level errors (incl. 401) into [IOException]. */
    private suspend fun runQuery(query: String): HardcoverData {
        try {
            val response = api.query(GraphQlRequest(query))
            val firstError = response.errors?.firstOrNull()
            if (firstError != null) throw IOException(firstError.message)
            return response.data ?: HardcoverData()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                throw IOException("Invalid or expired Hardcover token (401).")
            }
            throw IOException("Hardcover network error (${e.code()})")
        }
    }

    // ----- Connection test -----
    /** Verifies the currently stored token still works, by fetching the authenticated user. */
    suspend fun testApiKey() {
        val data = runQuery("query { me { id username } }")
        if (data.me.firstOrNull()?.id == null) throw IOException("Invalid Hardcover token.")
    }

    /** Verifies a candidate token (not yet persisted) before it gets saved by the caller. */
    suspend fun testApiKey(key: String) {
        val probe = BooksNetworkModule.provideRawApi(key)
        val response = probe.query(GraphQlRequest("query { me { id username } }"))
        val firstError = response.errors?.firstOrNull()
        if (firstError != null) throw IOException(firstError.message)
        if (response.data?.me?.firstOrNull()?.id == null) throw IOException("Invalid Hardcover token.")
    }

    /** Today's date as `yyyy-MM-dd`, in the Europe/Paris time zone, for use in release-date filters. */
    private fun todayIso(): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("Europe/Paris")
        return f.format(java.util.Date())
    }

    // ----- Working text search -----
    // Hardcover disabled _ilike (and friends: _like, _regex, _similar...) on its Hasura API
    // (see "Limitations" in the API docs, updated 2026-08-24): `books` can therefore no longer
    // be filtered with a plain _ilike like before. The dedicated `search` field (powered by
    // Typesense on Hardcover's side) is now the only documented text-search path; it only
    // returns ids, which are then resolved via a regular `books` query.
    /** Free-text book search: resolves matching ids via Hardcover's `search` field, then fetches full book data for them. */
    suspend fun search(query: String): List<HardcoverBook> {
        val cleanQuery = query.escapeGraphQl().trim()
        if (cleanQuery.isBlank()) return emptyList()

        val searchData = runQuery(
            """query {
                search(query: "$cleanQuery", query_type: "Book", per_page: 30, page: 1) { ids }
            }"""
        )
        val ids = searchData.search?.ids.orEmpty()
        if (ids.isEmpty()) return emptyList()

        val data = runQuery(
            """query {
                books(
                    where: {id: {_in: [${ids.joinToString(",")}]}},
                    order_by: {users_count: desc},
                    limit: 30
                ) { $BOOK_FIELDS }
            }"""
        )

        return data.books.orEmpty()
            .map { it.toHardcoverBook() }
            .filter { it.isDisplayable }
            .distinctBy { it.id }
            .preferFrenchDuplicates()
    }

    // ----- Trending -----
    /** Most-followed non-duplicate books on Hardcover (`canonical_id` null filters out edition duplicates). */
    suspend fun getPopular(): List<HardcoverBook> {
        val data = runQuery(
            """query { 
                books(
                    where: {users_count: {_gt: 10}, canonical_id: {_is_null: true}}, 
                    order_by: {users_count: desc}, 
                    limit: 30
                ) { $BOOK_FIELDS } 
            }"""
        )
        return data.books.orEmpty()
            .map { it.toHardcoverBook() }
            .filter { it.isDisplayable }
            .distinctBy { it.id }
            .preferFrenchDuplicates()
    }

    // ----- Upcoming releases -----
    // Now queries the `editions` entity itself (filtered to the French language), rather
    // than `books.release_date` (the ORIGINAL publication date, most often Japanese or English
    // for a translated manga/novel). With the previous query, a volume already released in
    // Japan but not yet in France (e.g. One Piece volume 113) could never show up here: its
    // original release_date was already in the past, even though the upcoming French edition
    // is exactly what this planning screen is supposed to announce.
    // The French-language filter below is intentional (see BOOK_FIELDS comment above).
    suspend fun getUpcoming(): List<HardcoverBook> {
        val data = runQuery(
            """query {
                editions(
                    where: {
                        language: {code2: {_eq: "fr"}},
                        release_date: {_gt: "${todayIso()}"},
                        compilation: {_eq: false},
                        book: {canonical_id: {_is_null: true}}
                    },
                    order_by: {release_date: asc},
                    limit: 30
                ) {
                    release_date release_year pages title subtitle
                    image { url }
                    book { $BOOK_FIELDS }
                }
            }"""
        )
        return data.editions.orEmpty()
            .mapNotNull { edition -> edition.book?.toHardcoverBook(frenchOverride = edition) }
            .filter { it.isDisplayable }
            .distinctBy { it.id }
    }

    // ----- Book detail -----
    /** Fetches a single book by id, resolving to its canonical edition if it is itself a duplicate. */
    suspend fun getBookDetail(volumeId: String): HardcoverBook? {
        val id = volumeId.toIntOrNull() ?: return null
        return try {
            val raw = fetchRawById(id) ?: return null
            val resolved = raw.canonicalId?.let { fetchRawById(it) } ?: raw
            resolved.toHardcoverBook()
        } catch (e: Exception) {
            null
        }
    }

    /** Low-level fetch of one book's raw GraphQL payload by numeric id. */
    private suspend fun fetchRawById(id: Int): HardcoverBookRaw? {
        val data = runQuery("""query { books(where: {id: {_eq: $id}}, limit: 1) { $BOOK_FIELDS } }""")
        return data.books.orEmpty().firstOrNull()
    }

    // ----- Similar books -----
    // Now filters on the viewed book's primary genre rather than its author (the previous
    // behavior listed "other books by the same author", not similar books). Hardcover doesn't
    // expose cached_tags as a server-filterable field (free-form jsonb, structure not
    // guaranteed by the formal schema — see extractGenres): a pool of popular titles is
    // fetched instead and filtered client-side on the primary genre, the only reliable
    // approach here. Result capped at 10, as requested.
    suspend fun getSimilarBooks(book: HardcoverBook): List<HardcoverBook> {
        val genre = book.primaryCategory ?: return emptyList()
        val bookId = book.id.toIntOrNull() ?: return emptyList()
        return try {
            val data = runQuery(
                """query {
                    books(
                        where: {
                            id: {_neq: $bookId}
                            canonical_id: {_is_null: true}
                        }
                        order_by: {users_count: desc}
                        limit: 200
                    ) { $BOOK_FIELDS }
                }"""
            )
            data.books.orEmpty()
                .map { it.toHardcoverBook() }
                .filter { it.isDisplayable }
                .filter { it.primaryCategory == genre }
                .distinctBy { it.id }
                .preferFrenchDuplicates()
                .take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ----- Series -----
    // Now queries the book's series directly (`series(where: {id: ...})`) rather than "other
    // books by the same author": the previous query didn't filter by series at all and could
    // therefore mix several different sagas by the same author. Result sorted by `position`
    // (volume number within the series), the only truly chronological order — release_date
    // can be misleading (re-editions, omnibus...).
    suspend fun getSeriesBooks(book: HardcoverBook): List<HardcoverBook> {
        val seriesId = book.seriesId?.toIntOrNull() ?: return emptyList()
        return try {
            val data = runQuery(
                """query {
                    series(where: {id: {_eq: $seriesId}}, limit: 1) {
                        book_series(order_by: {position: asc}) {
                            position
                            compilation
                            book { $BOOK_FIELDS }
                        }
                    }
                }"""
            )
            data.series.orEmpty()
                .flatMap { it.bookSeries }
                // Box sets/compilations excluded via Hardcover's dedicated field (reliable)
                // rather than title keyword detection alone (see looksLikeBoxSet, kept right
                // after as a safety net).
                .filterNot { it.compilation }
                .mapNotNull { entry -> entry.book?.let { raw -> entry.position to raw.toHardcoverBook() } }
                .filter { (_, b) -> b.isDisplayable }
                .filterNot { (_, b) -> b.looksLikeBoxSet() }
                .distinctBy { (_, b) -> b.id }
                // A series can link several Hardcover `book` records to the very same
                // volume: one per language (a separate French-titled record like
                // "Berserk, tome 03" next to the original "Berserk, Vol. 3") and separate
                // "special edition" records (collector's/deluxe re-releases not caught by
                // looksLikeBoxSet because they aren't compilations, just a single reprinted
                // volume). Both cases share the same series `position`, so only the base
                // volume is kept per position: NOT the French one — the goal here is the
                // canonical/original title (e.g. "Vol. 3"); the French *release date* is a
                // separate concern, already covered by frenchReleaseTimestamp regardless of
                // which record is picked. The record with the most readers (users_count) is
                // used as a proxy for the "main" one rather than a translated/niche edition.
                .groupBy { (position, _) -> position }
                .values
                .map { group -> group.maxBy { (_, b) -> b.usersCount ?: 0 } }
                .sortedWith(compareBy(nullsLast()) { (position, _) -> position })
                .map { (_, b) -> b }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Hardcover already returns absolute cover URLs, so this is a pass-through kept for API symmetry with the other domains. */
    fun coverUrl(url: String?): String? = url

    // ----- Local database & tracking -----
    /** Live list of books the user is following (for upcoming releases). */
    fun observeFollowed(): Flow<List<FollowedBook>> = db.followedBookDao().observeAll()

    /** Followed books that have already been released in France but not yet marked as read. */
    fun observeReleasedFollowedNotStarted(): Flow<List<FollowedBook>> =
        kotlinx.coroutines.flow.combine(observeFollowed(), observeReadBooks()) { followed, read ->
            val trackedIds = read.map { it.volumeId }.toSet()
            followed.filter { it.isReleased && it.volumeId !in trackedIds }
        }

    suspend fun isFollowed(volumeId: String): Boolean = db.followedBookDao().find(volumeId) != null
    suspend fun follow(item: FollowedBook) = db.followedBookDao().upsert(item)
    suspend fun unfollow(volumeId: String) = db.followedBookDao().remove(volumeId)

    suspend fun refreshFollowedIfStale(
        item: FollowedBook,
        staleAfterMillis: Long = 24 * 60 * 60 * 1000L,
        force: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!force && now - item.lastCheckedAt < staleAfterMillis) return

        val detail = getBookDetail(item.volumeId) ?: return
        db.followedBookDao().upsert(
            item.copy(
                title = detail.name,
                // French date only (no fallback to the original-language edition):
                // otherwise a volume already released abroad but not yet translated would
                // show an already-past date even though it isn't available in France (see
                // isReleasedInFrance). As long as no French edition is known, releaseTimestamp
                // stays null — the book then shows up in Planning without a precise date
                // rather than with a misleading foreign date.
                releaseTimestamp = detail.frenchReleaseTimestamp,
                isReleased = detail.isReleasedInFrance,
                lastCheckedAt = now,
                categories = detail.categories.joinToString(",").ifBlank { null }
            )
        )
    }

    // ----- Favorites -----
    /** Live list of the user's favorite books. */
    fun observeFavorites(): Flow<List<FavoriteBook>> = db.favoriteBookDao().observeAll()
    suspend fun isFavorite(volumeId: String): Boolean = db.favoriteBookDao().find(volumeId) != null
    suspend fun addFavorite(item: FavoriteBook) = db.favoriteBookDao().upsert(item)
    suspend fun removeFavorite(volumeId: String) = db.favoriteBookDao().remove(volumeId)

    // ----- Reading progress / status -----
    /** Live list of books with a reading-progress entry (to-read / reading / read). */
    fun observeReadBooks(): Flow<List<ReadBook>> = db.readBookDao().observeAll()
    suspend fun getReadStatus(volumeId: String): ReadBook? = db.readBookDao().find(volumeId)
    suspend fun setReadStatus(item: ReadBook) = db.readBookDao().upsert(item)
    suspend fun removeReadStatus(volumeId: String) = db.readBookDao().remove(volumeId)

    // ----- Export / Import -----
    /** Snapshots all local Books data (followed, read, favorites) for backup/export. */
    suspend fun exportSnapshot(): BooksExportBundle = BooksExportBundle(
        followed = db.followedBookDao().getAllOnce(),
        read = db.readBookDao().getAllOnce(),
        favorites = db.favoriteBookDao().getAllOnce()
    )

    /** Restores a previously exported snapshot, optionally wiping existing local data first. */
    suspend fun importSnapshot(bundle: BooksExportBundle, replaceExisting: Boolean) {
        if (replaceExisting) clearAllData()
        db.followedBookDao().upsertAll(bundle.followed)
        db.readBookDao().insertAll(bundle.read)
        db.favoriteBookDao().upsertAll(bundle.favorites)
    }

    /** Wipes all local Books data (followed, read, favorites). Irreversible. */
    suspend fun clearAllData() {
        db.followedBookDao().clearAll()
        db.readBookDao().clearAll()
        db.favoriteBookDao().clearAll()
    }
}

@Serializable
data class BooksExportBundle(
    val followed: List<FollowedBook> = emptyList(),
    val read: List<ReadBook> = emptyList(),
    val favorites: List<FavoriteBook> = emptyList()
)