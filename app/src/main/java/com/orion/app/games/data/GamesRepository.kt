package com.orion.app.games.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** IGDB entries that are commercial re-releases/compilations of a base game rather than an
 *  actual separate game in the saga — e.g. "Grand Theft Auto V: Premium Online Edition",
 *  "Grand Theft Auto PS Vita Collection". Kept as a safety net for the "Same franchise" tab
 *  (see getGamesInFranchise): IGDB's `category`/`parent_game` fields are frequently left
 *  unpopulated on exactly these entries, so filtering on them alone still let editions and
 *  compilations through. Mirrors BOX_SET_KEYWORDS on the books side. */
private val GAME_EDITION_KEYWORDS = listOf(
    "edition", "collection", "bundle", "compilation", "goty", "game of the year"
)

private fun IgdbGame.looksLikeEditionOrBundle(): Boolean {
    val n = name.lowercase()
    return GAME_EDITION_KEYWORDS.any { n.contains(it) }
}

/** Serializes a list of IGDB genres into the CSV format stored on PlayedGame.genres. */
fun List<IgdbGenre>.toGenresCsv(): String? = joinToString(",") { it.name }.ifBlank { null }

/**
 * Repository for the Video Games domain, backed by the IGDB API and a local Room database
 * (followed/favorite/played games). Mirrors CinemaRepository/BooksRepository's shape.
 */
class GamesRepository(
    private val api: IgdbApi,
    private val db: GamesDatabase
) {
    // ----- Connection test (IGDB credentials) -----
    suspend fun testConnection() {
        api.getPopularGames(IgdbApi.apicalypse("fields name; limit 1;"))
    }

    /**
     * Verifies Twitch credentials without going through NetworkModule (which always reads
     * the credentials already persisted in the store). Calls Twitch then IGDB directly with
     * a throwaway HTTP client, without touching the cache or preferences.
     */
    suspend fun testConnection(clientId: String, clientSecret: String, tokenStore: IgdbTokenStore) {
        val token = tokenStore.fetchTokenWithoutCaching(clientId, clientSecret)
        val probe = IgdbNetworkModule.provideRawApi(clientId, token)
        probe.getPopularGames(IgdbApi.apicalypse("fields name; limit 1;"))
    }

    // ----- Search -----
    suspend fun search(query: String): List<IgdbGame> {
        val escaped = query.replace("\"", "\\\"")
        return api.searchGames(
            IgdbApi.apicalypse(
                """
            search "$escaped";
            fields name,cover.url,first_release_date,aggregated_rating,rating,genres.name,category,parent_game,version_parent;
            limit 50;
            """
            )
        ).filter { it.parentGame == null && it.versionParent == null }
    }

    // ----- Trending (currently popular games, not all-time) -----
    /**
     * Same "Popular right now" results as igdb.com, powered by IGDB's own PopScore
     * (popularity_primitives, type 1 = "Visits"). Replaces the previous heuristic
     * (recent releases sorted by vote count), which stayed blind to a just-released game
     * still gathering its first reviews (e.g. a game launched days ago, with few ratings
     * yet but already spiking in visits).
     */
    suspend fun getPopular(): List<IgdbGame> {
        val primitives = api.getPopularityPrimitives(
            IgdbApi.apicalypse(
                """
                fields game_id,value;
                where popularity_type = 1;
                sort value desc;
                limit 30;
                """
            )
        )
        val orderedIds = primitives.mapNotNull { it.gameId }.distinct()
        if (orderedIds.isEmpty()) return emptyList()

        // getGamesByIds doesn't preserve the PopScore order (IGDB's `where id = (...)`
        // returns its own default ordering), so the original ranking is re-applied here.
        val rank = orderedIds.withIndex().associate { (index, id) -> id to index }
        return getGamesByIds(orderedIds)
            .filter { it.parentGame == null && it.versionParent == null }
            .sortedBy { rank[it.id] ?: Int.MAX_VALUE }
    }

    // ----- Upcoming releases -----
    suspend fun getUpcoming(): List<IgdbGame> {
        val now = System.currentTimeMillis() / 1000
        return api.getUpcomingGames(
            IgdbApi.apicalypse(
                """
                fields name,cover.url,first_release_date,genres.name;
                where first_release_date > $now;
                sort first_release_date asc;
                limit 30;
                """
            )
        )
    }

    // ----- Detail -----
    suspend fun getGameDetail(id: Int): IgdbGame? = api.getGameDetail(
        IgdbApi.apicalypse(
            """
        fields name,summary,storyline,cover.url,first_release_date,aggregated_rating,
               rating,total_rating,genres.name,platforms.name,platforms.abbreviation,
               involved_companies.company.name,involved_companies.developer,
               involved_companies.publisher,release_dates.date,release_dates.human,
               release_dates.platform,screenshots.url,videos.name,
               videos.video_id,similar_games,game_modes.name,game_engines.name,
               dlcs,expansions,bundles,standalone_expansions,parent_game,version_parent,
               remakes,remasters,franchises.name,collections.name;
        where id = $id;
        limit 1;
        """
        )
    ).firstOrNull()

    suspend fun getGamesByIds(ids: List<Int>): List<IgdbGame> {
        if (ids.isEmpty()) return emptyList()
        return api.getGamesByIds(
            IgdbApi.apicalypse(
                """
                fields name,cover.url,first_release_date,aggregated_rating,rating,genres.name,parent_game,version_parent;
                where id = (${ids.joinToString(",")});
                limit ${ids.size};
                """
            )
        )
    }

    /**
     * Other games in the same IGDB franchise(s) (e.g. other Zelda games when viewing Tears
     * of the Kingdom). Combines `franchises` AND `collections`: IGDB reserves `franchises`
     * for very large cross-cutting franchises (rarely populated), while `collections` is
     * the field actually used for a classic saga/series — filtering on `franchises` alone
     * left this tab empty for the vast majority of games.
     *
     * Important: `franchises` and `collections` are two IGDB tables with totally
     * independent id spaces. Their ids must therefore never be merged into a single shared
     * filter (e.g. `collections = (franchiseAndCollectionIds)`): a franchise id can
     * numerically collide with a completely unrelated collection's id, surfacing
     * completely off-topic games (e.g. Akira or Punch-Out on The Witcher 3's page). Each id
     * list is therefore filtered only against its own field. The ids themselves
     * (game.franchiseIds/collectionIds) are further restricted to entries whose name
     * overlaps the game's title (see matchingTitle in Igdb.kt), to ignore IGDB groupings
     * that are too broad (e.g. the mobile game "Roach Race" wrongly linked to a grouping
     * from the same studio rather than to the actual "The Witcher" franchise).
     *
     * The category filter keeps ONLY base games (0 = main_game): neither remasters (9),
     * remakes (8), DLC, expansions, updates, bundles, mods, nor editions show up here —
     * only actual games in the saga. A missing category is tolerated (many franchise
     * entries don't have this field populated on IGDB's side), to avoid artificially
     * emptying the results for legitimate but poorly-documented games.
     */
    suspend fun getGamesInFranchise(game: IgdbGame): List<IgdbGame> {
        val franchiseIds = game.franchiseIds
        val collectionIds = game.collectionIds
        if (franchiseIds.isEmpty() && collectionIds.isEmpty()) return emptyList()

        val licenseConditions = buildList {
            if (franchiseIds.isNotEmpty()) add("franchises = (${franchiseIds.joinToString(",")})")
            if (collectionIds.isNotEmpty()) add("collections = (${collectionIds.joinToString(",")})")
        }

        // `category = null | category = 0` alone isn't enough: commercial re-releases like
        // "Grand Theft Auto V: Premium Online Edition" or DLC-driven entries like
        // "Grand Theft Auto Online: The Doomsday Heist" are frequently left with `category`
        // unset on IGDB, so they slipped through as if they were base games. Excluding
        // anything with `parent_game`/`version_parent` set catches editions and DLC-style
        // "games" attached to another one (e.g. to "Grand Theft Auto Online"), which is most
        // of them; looksLikeEditionOrBundle then mops up the rest (bundles/compilations that
        // have no parent at all, like a PS Vita Collection) by name.
        return api.getGamesByFranchise(
            IgdbApi.apicalypse(
                """
                fields name,cover.url,first_release_date,aggregated_rating,rating,parent_game,version_parent;
                where (${licenseConditions.joinToString(" | ")}) & id != ${game.id}
                      & (category = null | category = 0)
                      & parent_game = null & version_parent = null;
                sort first_release_date asc;
                limit 40;
                """
            )
        ).filterNot { it.looksLikeEditionOrBundle() }
    }

    suspend fun getTimeToBeat(id: Int): IgdbTimeToBeat? = api.getTimeToBeat(
        IgdbApi.apicalypse(
            """
        fields game_id,hastily,normally,completely;
        where game_id = $id;
        limit 1;
        """
        )
    ).firstOrNull()

    fun coverUrl(url: String?): String? = url

    // ----- Planning (followed games) -----
    fun observeFollowed(): Flow<List<FollowedGame>> = db.followedGameDao().observeAll()

    /**
     * Followed games already released but absent from the progress library (neither
     * "playing", "completed", nor explicit "backlog"): an in-memory join between followed
     * and progress, the two tables having no foreign key toward each other (independent
     * domains).
     */
    fun observeReleasedFollowedNotStarted(): Flow<List<FollowedGame>> =
        kotlinx.coroutines.flow.combine(observeFollowed(), observePlayed()) { followed, played ->
            val trackedIds = played.map { it.igdbId }.toSet()
            followed.filter { it.isReleased && it.igdbId !in trackedIds }
        }

    suspend fun isFollowed(igdbId: Int): Boolean = db.followedGameDao().find(igdbId) != null

    suspend fun follow(item: FollowedGame) = db.followedGameDao().upsert(item)

    suspend fun unfollow(igdbId: Int) = db.followedGameDao().remove(igdbId)

    /** Refreshes a followed game's release date if the data is stale. */
    suspend fun refreshFollowedIfStale(
        item: FollowedGame,
        staleAfterMillis: Long = 24 * 60 * 60 * 1000L,
        force: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!force && now - item.lastCheckedAt < staleAfterMillis) return

        val detail = getGameDetail(item.igdbId) ?: return
        val nextTs = detail.nextReleaseTimestamp
        db.followedGameDao().upsert(
            item.copy(
                title = detail.name,
                releaseTimestamp = nextTs ?: detail.firstReleaseDate,
                isReleased = detail.firstReleaseDate?.let { it * 1000 < now } ?: false,
                lastCheckedAt = now,
                studio = detail.developerNames ?: item.studio
            )
        )
    }

    // ----- Favorites -----
    fun observeFavorites(): Flow<List<FavoriteGame>> = db.favoriteGameDao().observeAll()

    suspend fun isFavorite(igdbId: Int): Boolean = db.favoriteGameDao().find(igdbId) != null

    suspend fun addFavorite(item: FavoriteGame) = db.favoriteGameDao().upsert(item)

    suspend fun removeFavorite(igdbId: Int) = db.favoriteGameDao().remove(igdbId)

    // ----- Played games / progress -----
    fun observePlayed(): Flow<List<PlayedGame>> = db.playedGameDao().observeAll()

    suspend fun getPlayedStatus(igdbId: Int): PlayedGame? = db.playedGameDao().find(igdbId)

    suspend fun setPlayedStatus(item: PlayedGame) = db.playedGameDao().upsert(item)

    suspend fun removePlayedStatus(igdbId: Int) = db.playedGameDao().remove(igdbId)

    // ----- Export / Import (same schema as cinema, independent snapshot) -----
    suspend fun exportSnapshot(): GamesExportBundle = GamesExportBundle(
        followed = db.followedGameDao().getAllOnce(),
        played = db.playedGameDao().getAllOnce(),
        favorites = db.favoriteGameDao().getAllOnce()
    )

    suspend fun importSnapshot(bundle: GamesExportBundle, replaceExisting: Boolean) {
        if (replaceExisting) clearAllData()
        db.followedGameDao().upsertAll(bundle.followed)
        db.playedGameDao().insertAll(bundle.played)
        db.favoriteGameDao().upsertAll(bundle.favorites)
    }

    suspend fun clearAllData() {
        db.followedGameDao().clearAll()
        db.playedGameDao().clearAll()
        db.favoriteGameDao().clearAll()
    }
}

@Serializable
data class GamesExportBundle(
    val followed: List<FollowedGame> = emptyList(),
    val played: List<PlayedGame> = emptyList(),
    val favorites: List<FavoriteGame> = emptyList()
)