package com.orion.app.books.data

import com.orion.app.core.util.StringUtils.stripHtmlTags
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Hardcover (https://hardcover.app) — public GraphQL API replacing Google Books.
 * Unlike Google Books (REST + query parameters), Hardcover exposes a SINGLE POST endpoint
 * that accepts a raw GraphQL query. Rather than a formal GraphQL variables mechanism
 * ($x: Type), values are interpolated directly into the query text — the same approach as
 * IgdbApi (Apicalypse) on the games side — which avoids adding an extra abstraction layer
 * for a marginal benefit here.
 *
 * Docs: https://docs.hardcover.app/api/getting-started/
 */
interface HardcoverApi {
    @POST("v1/graphql")
    suspend fun query(@Body body: GraphQlRequest): GraphQlResponse
}

@Serializable
data class GraphQlRequest(val query: String)

@Serializable
data class GraphQlResponse(
    val data: HardcoverData? = null,
    val errors: List<GraphQlError>? = null
)

@Serializable
data class GraphQlError(val message: String = "Unknown error")

/**
 * Groups together EVERY possible field any of our queries can return. GraphQL only fills
 * in the fields actually requested by the query — the rest simply stay absent from the
 * JSON — so keeping them all nullable here, rather than defining a dedicated response type
 * per query, stays safe and avoids multiplying near-identical data classes.
 */
@Serializable
data class HardcoverData(
    val me: List<HardcoverMe> = emptyList(),
    val search: HardcoverSearchResult? = null,
    val books: List<HardcoverBookRaw>? = null,
    @SerialName("book_series") val bookSeries: List<HardcoverBookSeriesRaw>? = null,
    val series: List<HardcoverSeriesRaw>? = null,
    // Top-level `editions(...)` query used by getUpcoming: unlike `books`, an edition
    // carries its own release date and cover for a given language, which is required to
    // target French releases specifically.
    val editions: List<HardcoverEditionRaw>? = null,
)

@Serializable
data class HardcoverMe(val id: Int? = null, val username: String? = null)

@Serializable
data class HardcoverSearchResult(val ids: List<Int> = emptyList())

@Serializable
data class HardcoverImage(val url: String? = null)

@Serializable
data class HardcoverAuthor(val id: Int? = null, val name: String? = null)

@Serializable
data class HardcoverContribution(val author: HardcoverAuthor? = null)

@Serializable
data class HardcoverSeriesRef(
    val id: Int? = null,
    val name: String? = null,
    // Total number of books linked to this series on Hardcover's side: used to pick, among
    // several series a book might belong to (e.g. the actual numbered saga AND a separate
    // "box set"/companion grouping), the one with the most volumes — see
    // toHardcoverBook().featuredSeries.
    @SerialName("books_count") val booksCount: Int? = null,
)

@Serializable
data class HardcoverBookSeriesRaw(
    val position: Double? = null,
    // Indicates that THIS entry linking the book to the series is a box set/compilation of
    // several volumes rather than an individual volume (a dedicated Hardcover field, far
    // more reliable than keyword detection in the title) — see looksLikeBoxSet/getSeriesBooks.
    val compilation: Boolean = false,
    val series: HardcoverSeriesRef? = null,
    val book: HardcoverBookRaw? = null,
)

@Serializable
data class HardcoverBookRaw(
    val id: Int,
    val title: String = "",
    val subtitle: String? = null,
    val description: String? = null,
    val pages: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("release_year") val releaseYear: Int? = null,
    val rating: Double? = null,
    val slug: String? = null,
    val image: HardcoverImage? = null,
    val contributions: List<HardcoverContribution> = emptyList(),
    // Free-form jsonb on Hardcover's side, internal structure not guaranteed by the formal
    // schema (unlike every other field above): see extractGenres() below for the
    // intentionally defensive extraction of this one property.
    @SerialName("cached_tags") val cachedTags: JsonElement? = null,
    @SerialName("book_series") val bookSeries: List<HardcoverBookSeriesRaw> = emptyList(),
    // Present if this book is a duplicate merged into another "canonical" record: in that
    // case Hardcover deliberately empties its relations (book_series in particular), which
    // used to break "same series" filtering on the similar/series screens. See
    // toHardcoverBook + repository.
    @SerialName("canonical_id") val canonicalId: Int? = null,
    @SerialName("users_count") val usersCount: Int? = null,
    // Box set/compilation/anthology at the book level itself (dedicated Hardcover field).
    val compilation: Boolean = false,
    // Best available French edition for this book (see BOOK_FIELDS), prioritized for the
    // displayed release date / cover / title — falls back to the generic fields below
    // otherwise (usually the English edition, Hardcover's default).
    val editions: List<HardcoverEditionRaw> = emptyList(),
)

/**
 * A book edition (see the Hardcover docs at /api/graphql/schemas/editions). Reused in two
 * places: nested inside HardcoverBookRaw.editions (French override, `book` then absent),
 * and at the root of getUpcoming's `editions(...)` query (where `book` is populated).
 */
@Serializable
data class HardcoverEditionRaw(
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("release_year") val releaseYear: Int? = null,
    val pages: Int? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val image: HardcoverImage? = null,
    val book: HardcoverBookRaw? = null,
)

@Serializable
data class HardcoverSeriesRaw(
    val id: Int? = null,
    val name: String? = null,
    @SerialName("book_series") val bookSeries: List<HardcoverBookSeriesRaw> = emptyList(),
)

/**
 * Hardcover equivalent of the old GoogleVolume: same public surface (identical property
 * names) so the rest of the app (BookCardMappers, BookDetailScreen, BookInfoSection...)
 * needs almost no change beyond the imported type.
 */
data class HardcoverBook(
    val id: String,
    val name: String,
    val summary: String?,
    val authors: List<String>,
    val publisher: String?,
    val pageCount: Int?,
    val categories: List<String>,
    val coverUrl: String?,
    /** True if a French edition was found for this book (see BOOK_FIELDS.editions).
     *  A book with no French edition is still displayable (e.g. a volume not yet
     *  translated) — this field is only used to target/deduplicate the display, never to
     *  hide a book. */
    val hasFrenchEdition: Boolean = false,
    val publishedTimestamp: Long?,
    /** Release date of the French edition ONLY (no fallback to the original edition),
     *  unlike [publishedTimestamp]. Used to determine whether the book is genuinely
     *  available in France (see [isReleasedInFrance]): a volume already released in
     *  Japan/the US but not yet translated must not be considered "released" for a French
     *  reader, even though [publishedTimestamp] (which falls back to the original date) is
     *  already in the past. Null if no French edition is known yet, or its release date
     *  isn't known yet. */
    val frenchReleaseTimestamp: Long? = null,
    val year: String?,
    val seriesId: String?,
    val seriesName: String?,
    val seriesOrderNumber: Int?,
    val primaryAuthorId: Int?,
    val slug: String?,
    val ratingOn5: Double?,
    val volumeInfo: HardcoverVolumeInfo = HardcoverVolumeInfo(),
    // Used only to pick the "main" record among several Hardcover `book` entries sharing
    // the same series position (foreign-language/special editions) — see getSeriesBooks.
    val usersCount: Int? = null,
) {
    val authorsLabel: String? get() = authors.takeIf { it.isNotEmpty() }?.joinToString(", ")
    val primaryCategory: String? get() = categories.firstOrNull()
    val genreLabel: String? get() = categories.firstOrNull()
    val isbn13: String? get() = null
    val isbn10: String? get() = null
    val hasIsbn: Boolean get() = false

    /** Public Hardcover link for the book, used for the "View on Hardcover" action — far
     *  more reliable than the old Google Books webReaderLink (often missing or expired). */
    val previewUrl: String? get() = slug?.let { "https://hardcover.app/books/$it" }

    val isDisplayable: Boolean get() = name.isNotBlank()

    /** Hardcover rating out of 5 converted to out of 10, same convention as the previous implementation. */
    val displayRating: Double? get() = ratingOn5?.let { it * 2.0 }

    /**
     * True only if a French edition exists AND its release date is in the past. Use this
     * in place of `publishedTimestamp <= now` anywhere the question is whether THIS book
     * is available to a French reader (e.g. allowing "read"/"favorite", moving a followed
     * item from Planning to the library): `publishedTimestamp` falls back to the original
     * (non-French) release date when no French edition exists yet, which would wrongly
     * make an untranslated book (e.g. a volume released in Japan but not yet in France)
     * appear "released".
     */
    val isReleasedInFrance: Boolean get() =
        hasFrenchEdition && frenchReleaseTimestamp?.let { it <= System.currentTimeMillis() / 1000 } == true
}

/** Minimal container to stay compatible with `book.volumeInfo.subtitle` / `.language` used
 *  on the UI side — Hardcover doesn't expose a language at the book level (only per
 *  edition), so `language` always stays null here. */
data class HardcoverVolumeInfo(val subtitle: String? = null, val language: String? = null)

/**
 * Defensive extraction of genres from `cached_tags` (Hardcover's free-form jsonb, grouped
 * by category — e.g. `{"Genre": [{"tag": "Fantasy", ...}], "Mood": [...]}`). Unlike every
 * other field of HardcoverBookRaw, this structure isn't guaranteed by the formal GraphQL
 * schema (type `jsonb`): every read here is wrapped in safe casts (`as?`) and a global
 * try/catch, so an unexpected shape results in "no genre shown for this book" rather than
 * a crash — the same way BookCoverPlaceholder already handles a missing cover.
 */
private fun JsonElement?.extractGenres(): List<String> {
    if (this == null) return emptyList()
    return try {
        val obj = this as? JsonObject ?: return emptyList()
        val genreArray = (obj["Genre"] ?: obj["genre"] ?: obj["Genres"])?.jsonArray ?: return emptyList()
        genreArray.mapNotNull { entry ->
            val entryObj = entry as? JsonObject
            (entryObj?.get("tag") as? JsonPrimitive)?.contentOrNull()
                ?: (entryObj?.get("name") as? JsonPrimitive)?.contentOrNull()
        }.filter { it.isNotBlank() }.distinct()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun JsonPrimitive.contentOrNull(): String? = try { content } catch (e: Exception) { null }

/**
 * Lenient parsing of `release_date` ("yyyy-MM-dd") into epoch seconds (UTC midnight).
 * java.util.Calendar rather than java.time: see the same historical note in the old
 * GoogleBooks.kt — java.time crashes (NoClassDefFoundError, not an Exception so never
 * caught) on Android 8.0 (API 26) without desugaring enabled.
 */
private fun String.toEpochSecondsOrNull(): Long? {
    val parts = split("-")
    return try {
        val year = parts[0].toInt()
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.clear()
        calendar.set(year, month - 1, day, 0, 0, 0)
        calendar.timeInMillis / 1000L
    } catch (e: Exception) {
        null
    }
}

/**
 * Converts the raw GraphQL payload into a [HardcoverBook], the public surface used by the
 * rest of the app.
 *
 * [frenchOverride] lets the French edition be targeted by default (release date, cover,
 * page count): by default the best FR edition already included in the query is used
 * (`editions`, see BOOK_FIELDS), otherwise the book's generic fields are used instead —
 * usually the English edition, Hardcover's default one. The displayed title/subtitle, on
 * the other hand, ALWAYS remain the generic (English) book's, intentionally never replaced
 * by the French edition's: the app shows the English name everywhere, regardless of French
 * edition availability.
 */
fun HardcoverBookRaw.toHardcoverBook(frenchOverride: HardcoverEditionRaw? = editions.firstOrNull()): HardcoverBook {
    // Among the series this book is linked to, first prefer ones whose name overlaps the
    // title (the most direct signal), then, in case of a tie or no match, the one with the
    // most known volumes (books_count): a classic numbered saga always has far more entries
    // than a plain "box set"/companion grouping, which avoids mistakenly following the
    // latter (and thus showing an incomplete Series tab, cf. Claymore).
    val candidates = bookSeries.filter { it.series?.id != null && !it.series.name.isNullOrBlank() }
    val titleMatches = candidates.filter { entry ->
        val seriesName = entry.series!!.name!!
        title.contains(seriesName, ignoreCase = true) || seriesName.contains(title, ignoreCase = true)
    }
    val featuredSeries = (titleMatches.ifEmpty { candidates })
        .maxByOrNull { it.series?.booksCount ?: 0 }

    val effectiveReleaseDate = frenchOverride?.releaseDate ?: releaseDate
    val effectiveReleaseYear = frenchOverride?.releaseYear ?: releaseYear

    return HardcoverBook(
        id = id.toString(),
        // Name always in English (Hardcover's generic title, see this function's doc) —
        // no longer varies based on French edition availability: we want a stable,
        // consistent name throughout the app (search, trending, tracking, series...), not
        // a mix of English/French depending on the book.
        name = title,
        summary = description?.stripHtmlTags(),
        authors = contributions.mapNotNull { it.author?.name }.distinct(),
        publisher = null,
        pageCount = frenchOverride?.pages?.takeIf { it > 0 } ?: pages?.takeIf { it > 0 },
        categories = cachedTags.extractGenres(),
        coverUrl = frenchOverride?.image?.url ?: image?.url,
        // Fixes an existing bug: this field was never actually populated (always false),
        // which made preferFrenchDuplicates() a no-op (it never kept the French version in
        // case of a duplicate).
        hasFrenchEdition = frenchOverride != null,
        publishedTimestamp = effectiveReleaseDate?.toEpochSecondsOrNull(),
        frenchReleaseTimestamp = frenchOverride?.releaseDate?.toEpochSecondsOrNull(),
        year = effectiveReleaseYear?.toString() ?: effectiveReleaseDate?.take(4),
        seriesId = featuredSeries?.series?.id?.toString(),
        seriesName = featuredSeries?.series?.name,
        seriesOrderNumber = featuredSeries?.position?.toInt(),
        primaryAuthorId = contributions.firstOrNull()?.author?.id,
        slug = slug,
        ratingOn5 = rating,
        usersCount = usersCount,
        // Subtitle also always in English (generic), to stay consistent with the name —
        // avoids mixing an English name with a French subtitle.
        volumeInfo = HardcoverVolumeInfo(subtitle = subtitle)
    )
}

/** Minimal escaping to interpolate user text (search) into a raw GraphQL query without
 *  breaking the string literal — not a security boundary, just syntactic correctness (a
 *  quote or backslash typed by the user must not produce an invalid query). */
fun String.escapeGraphQl(): String = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")