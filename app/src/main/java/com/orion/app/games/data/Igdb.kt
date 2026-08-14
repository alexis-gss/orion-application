package com.orion.app.games.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * IGDB (a Twitch property) doesn't follow a classic REST style: every endpoint is queried
 * via POST with a query-language text body ("Apicalypse"), for example:
 *   fields name,cover.url,first_release_date; search "zelda"; limit 20;
 * Authentication uses a Twitch application token (client_credentials), renewed periodically
 * (~60 days); see IgdbAuthApi and IgdbTokenStore.
 */
interface IgdbApi {

    @POST("games")
    suspend fun searchGames(@Body query: RequestBody): List<IgdbGame>

    @POST("games")
    suspend fun getGamesByIds(@Body query: RequestBody): List<IgdbGame>

    @POST("games")
    suspend fun getPopularGames(@Body query: RequestBody): List<IgdbGame>

    // IGDB PopScore ("Popular right now" on igdb.com): a dedicated endpoint, separate from
    // `games`, returning only game_id + a popularity value per "primitive" (visits, want to
    // play, playing...). It must be queried first, then the actual game data fetched via
    // getGamesByIds with the returned ids, in that order — see GamesRepository.getPopular().
    @POST("popularity_primitives")
    suspend fun getPopularityPrimitives(@Body query: RequestBody): List<IgdbPopularityPrimitive>

    @POST("games")
    suspend fun getUpcomingGames(@Body query: RequestBody): List<IgdbGame>

    @POST("games")
    suspend fun getGameDetail(@Body query: RequestBody): List<IgdbGame>

    @POST("games")
    suspend fun getGamesByFranchise(@Body query: RequestBody): List<IgdbGame>

    @POST("game_time_to_beats")
    suspend fun getTimeToBeat(@Body query: RequestBody): List<IgdbTimeToBeat>

    companion object {
        fun apicalypse(body: String): RequestBody =
            body.trimIndent().toRequestBody("text/plain".toMediaType())
    }
}

/** Twitch application token, required to authenticate every IGDB call. */
interface IgdbAuthApi {
    @POST("oauth2/token")
    suspend fun getAppAccessToken(
        @retrofit2.http.Query("client_id") clientId: String,
        @retrofit2.http.Query("client_secret") clientSecret: String,
        @retrofit2.http.Query("grant_type") grantType: String = "client_credentials"
    ): TwitchTokenResponse
}

@Serializable
data class TwitchTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long
)

@Serializable
data class IgdbGame(
    val id: Int,
    val name: String = "",
    val summary: String? = null,
    val cover: IgdbCover? = null,
    @SerialName("first_release_date") val firstReleaseDate: Long? = null, // epoch seconds
    @SerialName("aggregated_rating") val aggregatedRating: Double? = null,

    @SerialName("category") val category: Int? = null,
    val rating: Double? = null,
    val genres: List<IgdbGenre> = emptyList(),
    val platforms: List<IgdbPlatform> = emptyList(),
    @SerialName("involved_companies") val involvedCompanies: List<IgdbInvolvedCompany> = emptyList(),
    @SerialName("release_dates") val releaseDates: List<IgdbReleaseDate> = emptyList(),
    val screenshots: List<IgdbScreenshot> = emptyList(),
    val videos: List<IgdbVideo> = emptyList(),
    @SerialName("similar_games") val similarGames: List<Int> = emptyList(),
    @SerialName("game_modes") val gameModes: List<IgdbGameMode> = emptyList(),
    @SerialName("total_rating") val totalRating: Double? = null,
    val storyline: String? = null,
    @SerialName("dlcs") val dlcs: List<Int> = emptyList(),
    @SerialName("expansions") val expansions: List<Int> = emptyList(),
    @SerialName("bundles") val bundles: List<Int> = emptyList(),
    @SerialName("standalone_expansions") val standaloneExpansions: List<Int> = emptyList(),
    @SerialName("parent_game") val parentGame: Int? = null,
    @SerialName("version_parent") val versionParent: Int? = null,
    @SerialName("game_engines") val gameEngines: List<IgdbGameEngine> = emptyList(),
    // Remasters/remakes of the base game (e.g. Demon's Souls PS5 is a "remake" of the
    // original PS3 game). Distinct from editions (parent_game/version_parent, same version
    // of the game): these are full standalone games, graphically/technically reworked.
    val remakes: List<Int> = emptyList(),
    val remasters: List<Int> = emptyList(),
    // IGDB franchise(s) (e.g. "Grand Theft Auto"). A dedicated IGDB field, but rarely
    // populated for most "normal" series (reserved for very large cross-cutting
    // franchises, e.g. "Mario" grouping Super Mario/Mario Kart/Mario Party...).
    // "collections" is the field IGDB actually uses for a classic saga/series (e.g. each
    // "Assassin's Creed", each "Zelda") — both are combined for the "Same franchise" tab,
    // otherwise most games would never get this tab, or would get an empty list
    // (franchises alone missed the vast majority of real cases).
    val franchises: List<IgdbFranchise> = emptyList(),
    val collections: List<IgdbCollection> = emptyList(),
) {
    val coverUrl: String? get() = cover?.url?.toHighRes()
    val year: String? get() = firstReleaseDate?.let {
        java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneOffset.UTC).year.toString()
    }
    val developerNames: String? get() = involvedCompanies
        .filter { it.developer == true }
        .mapNotNull { it.company?.name }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")

    /** Game publisher(s) (can differ from the developer, e.g. an indie studio + a major publisher). */
    val publisherNames: String? get() = involvedCompanies
        .filter { it.publisher == true }
        .mapNotNull { it.company?.name }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")

    /** Game engine(s) used (e.g. Unreal Engine 5, RE Engine). */
    val gameEngineNames: String? get() = gameEngines
        .map { it.name }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")

    /**
     * IGDB "user ratings" score (average of player ratings), prioritized over
     * aggregated_rating (average of press reviews, considered less objective). Falls back
     * to aggregated_rating/total_rating only if no player rating exists yet for this game.
     */
    val displayRating: Double? get() = rating ?: aggregatedRating ?: totalRating

    /** Editions/re-releases of the same base game (e.g. Game of the Year Edition). */
    val editionIds: List<Int> get() = listOfNotNull(parentGame, versionParent).distinct()

    /** Content shown in the "Related content" tab: only expansions, remasters and remakes
     *  (DLC, standalone packs, bundles and editions are no longer shown here). */
    val hasRelatedContent: Boolean get() =
        expansions.isNotEmpty() || remasters.isNotEmpty() || remakes.isNotEmpty()

    /** Combined IGDB series ids for the game (franchises + collections, deduplicated), used
     *  only to decide whether the "Same franchise" tab should be shown — see the comment on
     *  [collections]. For the query that fetches the franchise's own games, see
     *  [franchiseIds] and [collectionIds]: "franchises" and "collections" are two IGDB
     *  tables with independent id spaces, so mixing their ids into a single filter (e.g.
     *  `collections = (someFranchiseId)`) can surface a totally unrelated collection that
     *  happens to share the same numeric id. */
    val licenseIds: List<Int> get() = (franchiseIds + collectionIds).distinct()

    /** IGDB franchise ids for the game that are genuinely linked to its franchise, to be
     *  filtered only against the `franchises` field — see [matchingTitleFranchises]. */
    val franchiseIds: List<Int> get() = franchises.matchingTitleFranchises(name).map { it.id }

    /** IGDB collection/saga ids for the game that are genuinely linked to its franchise, to
     *  be filtered only against the `collections` field — see [matchingTitleCollections].
     *  Never combine these with [franchiseIds] in the same filter. */
    val collectionIds: List<Int> get() = collections.matchingTitleCollections(name).map { it.id }

    /** Next known future release date (base game or an edition), for Planning. */
    val nextReleaseTimestamp: Long? get() {
        val now = System.currentTimeMillis() / 1000
        return releaseDates.mapNotNull { it.date }.filter { it > now }.minOrNull()
            ?: firstReleaseDate?.takeIf { it > now }
    }
}

/**
 * Among the IGDB franchises/collections attached to a game, keeps only the ones whose name
 * overlaps the game's title (e.g. game "The Witcher 3: Wild Hunt" -> collection "The
 * Witcher"). IGDB sometimes links a game to a grouping far broader than its actual
 * franchise (e.g. a "promotions/spin-offs from the same studio" collection), which used to
 * surface completely unrelated games in the "Same franchise" tab (e.g. the mobile game
 * "Roach Race" showing up on The Witcher 3's page). If no franchise/collection overlaps the
 * title (games whose title differs a lot from their series name), the full list is used
 * instead of wrongly emptying the tab — the same fallback logic as featuredSeries on the
 * books side.
 */
private fun List<IgdbFranchise>.matchingTitleFranchises(gameTitle: String): List<IgdbFranchise> {
    val matches = filter { titleOverlaps(gameTitle, it.name) }
    return matches.ifEmpty { this }
}

private fun List<IgdbCollection>.matchingTitleCollections(gameTitle: String): List<IgdbCollection> {
    val matches = filter { titleOverlaps(gameTitle, it.name) }
    return matches.ifEmpty { this }
}

private fun titleOverlaps(gameTitle: String, seriesName: String): Boolean =
    gameTitle.contains(seriesName, ignoreCase = true) || seriesName.contains(gameTitle, ignoreCase = true)

private fun String.toHighRes(): String =
    // IGDB returns URLs as "t_thumb" by default; bumped to high quality to match the same
    // poster sizes used on the cinema side.
    this.replace("t_thumb", "t_cover_big").let { if (it.startsWith("//")) "https:$it" else it }

private fun String.toLogoRes(): String =
    // Platform logos are small icons: kept at a compact size (t_thumb, ~90x128) rather than
    // t_cover_big, which makes no sense for a logo.
    this.replace("t_thumb", "t_logo_med").let { if (it.startsWith("//")) "https:$it" else it }

private fun String.toScreenshotRes(): String =
    // t_cover_big (~264x374) is meant for portrait covers, not 16:9 screenshots: it
    // produced blurry/pixelated banners. t_1080p (1920x1080 max, aspect ratio kept) gives
    // a crisp result both as a banner and in the gallery.
    this.replace("t_thumb", "t_1080p").let { if (it.startsWith("//")) "https:$it" else it }

@Serializable
data class IgdbCover(val url: String? = null)

@Serializable
data class IgdbGenre(val id: Int, val name: String)

@Serializable
data class IgdbPlatform(
    val id: Int,
    val name: String,
    val abbreviation: String? = null,
    @SerialName("platform_logo") val platformLogo: IgdbPlatformLogo? = null
) {
    val logoUrl: String? get() = platformLogo?.url?.toLogoRes()
}

@Serializable
data class IgdbPlatformLogo(val url: String? = null)

@Serializable
data class IgdbInvolvedCompany(
    val company: IgdbCompany? = null,
    val developer: Boolean? = null,
    val publisher: Boolean? = null
)

@Serializable
data class IgdbCompany(val name: String)

@Serializable
data class IgdbGameEngine(val id: Int, val name: String)

@Serializable
data class IgdbReleaseDate(
    val date: Long? = null,
    @SerialName("human") val human: String? = null,
    val platform: Int? = null
)

@Serializable
data class IgdbScreenshot(val url: String? = null) {
    val fullUrl: String? get() = url?.toScreenshotRes()
}

@Serializable
data class IgdbVideo(
    val name: String? = null,
    @SerialName("video_id") val videoId: String? = null
) {
    /** Highest-quality YouTube thumbnail available for this video id. */
    val thumbnailUrl: String? get() = videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
    val youtubeUrl: String? get() = videoId?.let { "https://www.youtube.com/watch?v=$it" }
}

/** Completion times estimated by IGDB (aggregated from HowLongToBeat), in seconds. */
@Serializable
data class IgdbTimeToBeat(
    @SerialName("game_id") val gameId: Int? = null,
    val hastily: Long? = null,
    val normally: Long? = null,
    val completely: Long? = null
) {
    val hasAnyValue: Boolean get() = hastily != null || normally != null || completely != null
}

@Serializable
data class IgdbGameMode(val id: Int, val name: String)

/**
 * One row of IGDB's PopScore ("popularity_primitives" endpoint). `popularityType = 1` is
 * IGDB's own "Visits" primitive — the metric powering the "Popular right now" section on
 * igdb.com itself: it reacts within a day to a spike of interest (e.g. a brand new release
 * getting checked out by lots of people), unlike a rating-count-based sort which stays low
 * until reviews pile up over weeks.
 */
@Serializable
data class IgdbPopularityPrimitive(
    @SerialName("game_id") val gameId: Int? = null,
    @SerialName("popularity_type") val popularityType: Int? = null,
    val value: Double? = null
)

/** Large cross-cutting IGDB franchise (e.g. "Grand Theft Auto", "Mario"). Rarely populated
 *  for a "normal" series — see [IgdbGame.collections] for the common case. */
@Serializable
data class IgdbFranchise(val id: Int, val name: String)

/** IGDB series/saga in the usual sense (e.g. each "Assassin's Creed", each "Zelda") — the
 *  IGDB field actually populated for the vast majority of games that belong to a series. */
@Serializable
data class IgdbCollection(val id: Int, val name: String)