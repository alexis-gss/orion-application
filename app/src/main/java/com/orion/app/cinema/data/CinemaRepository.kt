package com.orion.app.cinema.data

import com.orion.app.core.data.GzipJsonExportImport
import com.orion.app.core.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** A gallery image, with a thumbnail URL (grid) and a full-quality URL (fullscreen viewer). */
data class GalleryImage(
    val path: String,
    val thumbnailUrl: String,
    val fullQualityUrl: String
)

/** Combined posters + backdrops result for the Media tab. */
data class CinemaGallery(
    val posters: List<GalleryImage> = emptyList(),
    val backdrops: List<GalleryImage> = emptyList()
) {
    val isEmpty: Boolean get() = posters.isEmpty() && backdrops.isEmpty()
}

/** Serializes a list of TMDB genres into the CSV format stored on WatchedItem.genres. */
fun List<Genre>.toGenresCsv(): String? = joinToString(",") { it.name }.ifBlank { null }

/**
 * Repository for the Cinema domain, backed by the TMDB API and a local Room database
 * (followed/watched/favorite movies and TV shows).
 */
class CinemaRepository(
    private val api: TmdbApi,
    private val db: AppDatabase,
    val imageBaseUrl: String = "https://image.tmdb.org/t/p/w342"
) {

    // Tests a specific key without persisting it beforehand
    suspend fun testApiKey(keyToTest: String) {
        api.testApiKeyDirect(keyToTest)
    }

    suspend fun testApiKey() {
        api.getConfiguration()
    }

    // ----- Search -----
    suspend fun search(query: String) = api.searchMulti(query)
        .results
        .filter { it.resolvedMediaType == "movie" || it.resolvedMediaType == "tv" }

    // ----- Popular / Trending -----
    suspend fun getPopular() = api.getTrending()
        .results
        .filter { it.resolvedMediaType == "movie" || it.resolvedMediaType == "tv" }

    // ----- Details -----
    suspend fun getMovieDetail(id: Int) = api.getMovieDetail(id)
    suspend fun getTvDetail(id: Int) = api.getTvDetail(id)
    suspend fun getSeasonDetail(tvId: Int, seasonNumber: Int) = api.getSeasonDetail(tvId, seasonNumber)

    fun posterUrl(path: String?): String? = path?.let { "$imageBaseUrl$it" }

    // ----- Orion (planning) -----
    fun observeFollowed(): Flow<List<FollowedItem>> = db.followedItemDao().observeAll()

    suspend fun isFollowed(tmdbId: Int, mediaType: String): Boolean =
        db.followedItemDao().find(tmdbId, mediaType) != null

    suspend fun follow(item: FollowedItem) = db.followedItemDao().upsert(item)

    suspend fun unfollow(tmdbId: Int, mediaType: String) = db.followedItemDao().remove(tmdbId, mediaType)

    /**
     * Refreshes a followed show's info (next episode, status) if the data is stale. Two
     * cases for the staleness duration:
     *  - If no next-episode date is known yet and the show is neither ended nor canceled,
     *    retry more often (30 min), since TMDB can publish next_episode_to_air with a
     *    slight delay after the previous episode airs.
     *  - Otherwise, use the normal throttle (6h) to avoid spamming the API.
     *
     * @param force if true, completely ignores the throttle and forces the network call
     *              (used by Planning's manual refresh).
     */
    suspend fun refreshFollowedIfStale(
        item: FollowedItem,
        staleAfterMillis: Long = 6 * 60 * 60 * 1000L,
        force: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!force) {
            val effectiveStale = if (item.mediaType == "tv" && item.nextAirDate == null &&
                item.status != "Ended" && item.status != "Canceled"
            ) {
                30 * 60 * 1000L
            } else {
                staleAfterMillis
            }
            if (now - item.lastCheckedAt < effectiveStale) return
        }

        if (item.mediaType == "tv") {
            val detail = api.getTvDetail(item.tmdbId)
            val next = detail.nextEpisodeToAir
            val last = detail.lastEpisodeToAir
            val airedEpisodes = if (last?.seasonNumber != null && last.episodeNumber != null) {
                detail.seasons
                    .filter { it.seasonNumber in 1 until last.seasonNumber }
                    .sumOf { it.episodeCount } + last.episodeNumber
            } else {
                detail.seasons
                    .filter { DateUtils.isReleased(it.airDate) || DateUtils.isWatchable(it.airDate) }
                    .sumOf { it.episodeCount }
            }
            db.followedItemDao().upsert(
                item.copy(
                    title = detail.name,
                    nextAirDate = next?.airDate,
                    nextEpisodeName = next?.name,
                    nextSeasonNumber = next?.seasonNumber,
                    nextEpisodeNumber = next?.episodeNumber,
                    status = detail.status,
                    lastAiredSeasonNumber = last?.seasonNumber,
                    lastAiredEpisodeNumber = last?.episodeNumber,
                    airedEpisodesCount = airedEpisodes.takeIf { it > 0 },
                    lastCheckedAt = now,
                    genres = detail.genres.toGenresCsv(),
                    voteAverage = detail.voteAverage
                )
            )
        } else if (item.mediaType == "movie") {
            val detail = api.getMovieDetail(item.tmdbId)
            db.followedItemDao().upsert(
                item.copy(
                    title = detail.title,
                    releaseDate = detail.releaseDate,
                    // Same mapping as when following (DetailScreen): nextAirDate is the
                    // field read by PlanningScreen, it must stay in sync with releaseDate
                    // for a movie or it would disappear from Planning on the next refresh.
                    nextAirDate = detail.releaseDate,
                    lastCheckedAt = now,
                    genres = detail.genres.toGenresCsv(),
                    voteAverage = detail.voteAverage
                )
            )
        }
    }

    // ----- Favorites -----
    fun observeFavorites(): Flow<List<FavoriteItem>> = db.favoriteItemDao().observeAll()

    suspend fun isFavorite(tmdbId: Int, mediaType: String): Boolean =
        db.favoriteItemDao().find(tmdbId, mediaType) != null

    suspend fun addFavorite(item: FavoriteItem) = db.favoriteItemDao().upsert(item)

    suspend fun removeFavorite(tmdbId: Int, mediaType: String) = db.favoriteItemDao().remove(tmdbId, mediaType)

    // ----- Watched -----

    fun observeWatched(): Flow<List<WatchedItem>> = db.watchedItemDao().observeAll()

    /**
     * Check if element is watched.
     */
    suspend fun isWatched(tmdbId: Int, mediaType: String, seasonNumber: Int?, episodeNumber: Int?): Boolean =
        db.watchedItemDao().countMatching(tmdbId, mediaType, seasonNumber, episodeNumber) > 0

    /**
     * Mark element watched.
     */
    suspend fun markWatched(item: WatchedItem) = db.watchedItemDao().insert(item)

    /**
     * Unmark element watched.
     */
    suspend fun unmarkWatched(tmdbId: Int, mediaType: String, seasonNumber: Int?, episodeNumber: Int?) =
        db.watchedItemDao().deleteMatching(tmdbId, mediaType, seasonNumber, episodeNumber)

    suspend fun markSeasonWatched(
        tmdbId: Int,
        title: String,
        posterPath: String?,
        seasonNumber: Int,
        episodes: List<EpisodeInfo>,
        genres: List<Genre> = emptyList()
    ) {
        val genresCsv = genres.toGenresCsv()
        episodes.forEach { ep ->
            val epNum = ep.episodeNumber ?: return@forEach
            if (!isWatched(tmdbId, "tv", seasonNumber, epNum)) {
                markWatched(
                    WatchedItem(
                        tmdbId = tmdbId, mediaType = "tv", title = title, posterPath = posterPath,
                        seasonNumber = seasonNumber, episodeNumber = epNum, episodeName = ep.name,
                        watchedAt = System.currentTimeMillis(), genres = genresCsv, durationMinutes = ep.runtime
                    )
                )
            }
        }
    }

    suspend fun unmarkSeasonWatched(tmdbId: Int, seasonNumber: Int, episodes: List<EpisodeInfo>) {
        episodes.forEach { ep ->
            val epNum = ep.episodeNumber ?: return@forEach
            unmarkWatched(tmdbId, "tv", seasonNumber, epNum)
        }
    }

    // ----- Cinema gallery ("Media" tab) -----
    // Only loaded when the Media tab is clicked (see MovieDetailContent/TvDetailContent),
    // never during the page's initial load, to keep the first call lightweight.

    /** Gallery (posters + backdrops) for a movie, in original quality. */
    suspend fun getMovieGallery(tmdbId: Int, currentPosterPath: String?, currentBackdropPath: String?): CinemaGallery =
        api.getMovieImages(tmdbId).toGallery(currentPosterPath, currentBackdropPath)

    /** Gallery (posters + backdrops) for a TV show, in original quality. */
    suspend fun getTvGallery(tmdbId: Int, currentPosterPath: String?, currentBackdropPath: String?): CinemaGallery =
        api.getTvImages(tmdbId).toGallery(currentPosterPath, currentBackdropPath)

    /** Posters available for a specific season of a TV show. */
    suspend fun getSeasonPosters(tmdbId: Int, seasonNumber: Int, currentPosterPath: String?): List<GalleryImage> =
        api.getSeasonImages(tmdbId, seasonNumber).posters
            .map { it.filePath }
            .let { reorderWithCurrentFirst(it, currentPosterPath) }
            .map { it.toGalleryImage() }

    private fun ImagesResponse.toGallery(currentPoster: String?, currentBackdrop: String?): CinemaGallery = CinemaGallery(
        posters = reorderWithCurrentFirst(posters.map { it.filePath }, currentPoster).map { it.toGalleryImage() },
        backdrops = reorderWithCurrentFirst(backdrops.map { it.filePath }, currentBackdrop).map { it.toGalleryImage() }
    )

    private fun String.toGalleryImage(): GalleryImage = GalleryImage(
        path = this,
        thumbnailUrl = "$TMDB_IMAGE_CDN/w500$this",
        fullQualityUrl = "$TMDB_IMAGE_CDN/original$this"
    )

    private fun reorderWithCurrentFirst(paths: List<String>, current: String?): List<String> {
        if (current == null) return paths
        val distinct = paths.toMutableList()
        distinct.remove(current)
        distinct.add(0, current)
        return distinct
    }

    /**
     * Snapshots all local Cinema data (followed, watched, favorites) for backup/export.
     */
    suspend fun exportSnapshot(): CinemaExportBundle = CinemaExportBundle(
        followed = db.followedItemDao().getAllOnce(),
        watched = db.watchedItemDao().getAllOnce(),
        favorites = db.favoriteItemDao().getAllOnce()
    )

    /**
     * Restores a previously exported snapshot, optionally wiping existing local data first.
     */
    suspend fun importSnapshot(bundle: CinemaExportBundle, replaceExisting: Boolean) {
        if (replaceExisting) {
            clearAllData()
        }
        db.followedItemDao().upsertAll(bundle.followed)
        db.watchedItemDao().insertAll(bundle.watched)
        db.favoriteItemDao().upsertAll(bundle.favorites)
    }

    suspend fun clearAllData() {
        db.followedItemDao().clearAllFollowedItems()
        db.watchedItemDao().clearAllWatchedItems()
        db.favoriteItemDao().clearAllFavoriteItems()
    }

    companion object {
        private const val TMDB_IMAGE_CDN = "https://image.tmdb.org/t/p"
    }
}

@Serializable
data class CinemaExportBundle(
    val followed: List<FollowedItem> = emptyList(),
    val watched: List<WatchedItem> = emptyList(),
    val favorites: List<FavoriteItem> = emptyList()
)
