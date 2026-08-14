package com.orion.app.cinema.ui.components

import android.content.Context
import com.orion.app.R
import com.orion.app.cinema.data.FavoriteItem
import com.orion.app.cinema.data.FollowedItem
import com.orion.app.cinema.data.SearchResult
import com.orion.app.cinema.data.WatchedItem

/**
 * Extension functions converting each of the app's data models into the generic
 * CinemaCardData consumed by CinemaItemRow / CinemaCarouselRow. This is the only place that
 * needs to know both the domain models (WatchedItem, FollowedItem, SearchResult...) and
 * the display model.
 */

/** Watched movie (Account) — one row per movie. `dateLabel` is already formatted by the
 *  screen (SimpleDateFormat), to avoid duplicating date-formatting logic here. */
fun WatchedItem.toWatchedMovieCardData(context: Context, dateLabel: String): CinemaCardData = CinemaCardData(
    key = "movie_$id",
    tmdbId = tmdbId,
    mediaType = mediaType,
    title = title,
    posterPath = posterPath,
    subtitle = context.getString(R.string.label_movie),
    trailingText = dateLabel,
    rating = voteAverage,
    genres = genres,
    releaseDate = releaseDate,
    addedAt = watchedAt
)

/**
 * Followed item shown in Bookmark ("to watch"). For a TV show, `watchedEpisodesCount`
 * (number of already-aired episodes marked watched, computed by the screen from the local
 * database) drives a "remaining to watch" badge on the poster — same style as the date
 * badge seen on Account (trailingText -> gradient badge on CinemaCarouselRow). If the total
 * number of aired episodes isn't known yet (show not yet refreshed from TMDB), no badge is
 * shown rather than a potentially wrong number.
 */
fun FollowedItem.toBookmarkCardData(context: Context, watchedEpisodesCount: Int = 0): CinemaCardData {
    val subtitle = when {
        mediaType == "movie" -> context.getString(R.string.movie_released_on, nextAirDate ?: "?")
        nextEpisodeNumber != null ->
            context.getString(R.string.last_episode_aired, nextSeasonNumber, nextEpisodeNumber) +
                    (nextEpisodeName?.let { " · $it" } ?: "")
        else -> status ?: context.getString(R.string.label_series)
    }
    val remaining = airedEpisodesCount?.let { total -> (total - watchedEpisodesCount).coerceAtLeast(0) }
    val trailingText = remaining?.takeIf { mediaType == "tv" && it > 0 }?.let {
        if (it == 1) context.getString(R.string.episode_remaining_singular) else context.getString(R.string.episodes_remaining_plural, it)
    }
    return CinemaCardData(
        key = "${mediaType}_$tmdbId",
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        posterPath = posterPath,
        subtitle = subtitle,
        trailingText = trailingText,
        rating = voteAverage,
        genres = genres,
        releaseDate = releaseDate,
        addedAt = addedAt
    )
}

/** Followed item shown in Planning (the "days remaining" chip is handled by the screen
 *  via `trailingContent`, it isn't part of the display model itself). An item not yet
 *  released has by definition no votes: the rating is hidden (often still 0.0 on TMDB's
 *  side) rather than showing a misleading badge. */
fun FollowedItem.toPlanningCardData(context: Context): CinemaCardData {
    val subtitle = when {
        mediaType == "movie" -> context.getString(R.string.label_movie)
        nextEpisodeNumber != null ->
            "S$nextSeasonNumber E$nextEpisodeNumber" + (nextEpisodeName?.let { " · $it" } ?: "")
        else -> status ?: context.getString(R.string.label_series)
    }
    return CinemaCardData(
        key = "${mediaType}_$tmdbId",
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        posterPath = posterPath,
        subtitle = subtitle,
        rating = null,
        genres = genres,
        releaseDate = releaseDate,
        addedAt = addedAt
    )
}

/** Search result. The TMDB rating (when available and non-zero) drives the colored rating
 *  badge shown on the card — no point showing a "0.0" badge for a title that simply
 *  doesn't have votes yet. */
fun SearchResult.toCardData(context: Context): CinemaCardData = CinemaCardData(
    key = "${resolvedMediaType}_$id",
    tmdbId = id,
    mediaType = resolvedMediaType,
    title = displayTitle,
    posterPath = posterPath,
    subtitle = listOfNotNull(
        if (resolvedMediaType == "movie") context.getString(R.string.label_movie) else context.getString(R.string.label_series),
        year
    ).joinToString(" · "),
    rating = voteAverage?.takeIf { it > 0.0 },
    releaseDate = releaseDate ?: firstAirDate
)

/** Favorite item shown in Account. */
fun FavoriteItem.toFavoriteCardData(context: Context, dateLabel: String? = null): CinemaCardData = CinemaCardData(
    key = "${mediaType}_$tmdbId",
    tmdbId = tmdbId,
    mediaType = mediaType,
    title = title,
    posterPath = posterPath,
    subtitle = if (mediaType == "movie") context.getString(R.string.label_movie) else context.getString(R.string.label_series),
    trailingText = dateLabel,
    rating = voteAverage,
    genres = genres,
    releaseDate = releaseDate,
    addedAt = addedAt
)