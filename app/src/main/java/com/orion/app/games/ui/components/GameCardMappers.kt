package com.orion.app.games.ui.components

import android.content.Context
import com.orion.app.R
import com.orion.app.core.util.DateUtils
import com.orion.app.games.data.FavoriteGame
import com.orion.app.games.data.FollowedGame
import com.orion.app.games.data.IgdbGame
import com.orion.app.games.data.PlayedGame

fun IgdbGame.toCardData(subtitle: String? = year): GameCardData = GameCardData(
    key = "igdb_$id",
    igdbId = id,
    title = name,
    coverUrl = coverUrl,
    subtitle = subtitle,
    rating = igdbRatingTo10(displayRating),
    genres = genres?.joinToString(",") { it.name }
)

fun FollowedGame.toCardData(context: Context): GameCardData = GameCardData(
    key = "followed_$igdbId",
    igdbId = igdbId,
    title = title,
    // Developer studio rather than the release date: on the Planning card, the date is
    // already shown separately (group header + "days remaining" badge), repeating it here
    // added nothing while the studio is info missing from the rest of the card.
    subtitle = studio,
    coverUrl = coverUrl,
    trailingText = if (isReleased) null else context.getString(R.string.game_status_upcoming),
    genres = genres,
    rating = voteAverage,
    releaseDate = releaseTimestamp?.let { it * 1000 },
    addedAt = null
)

fun PlayedGame.toCardData(context: Context, showStatusDateBadge: Boolean = false): GameCardData = GameCardData(
    key = "played_$id",
    igdbId = igdbId,
    title = title,
    coverUrl = coverUrl,
    subtitle = statusLabel(context, status),
    trailingText = when {
        showStatusDateBadge -> playedAt.toReadableDateMillis()
        else -> hoursPlayed?.let { context.getString(R.string.game_hours_played_short, it) }
    },
    genres = genres,
    rating = voteAverage,
    releaseDate = releaseDate,
    addedAt = playedAt
)

fun FavoriteGame.toCardData(context: Context, showAddedDateBadge: Boolean = false): GameCardData = GameCardData(
    key = "favorite_$igdbId",
    igdbId = igdbId,
    title = title,
    coverUrl = coverUrl,
    trailingText = if (showAddedDateBadge) addedAt.toReadableDateMillis() else null,
    genres = genres,
    rating = voteAverage,
    releaseDate = releaseDate,
    addedAt = addedAt
)

private fun statusLabel(context: Context, status: String): String = when (status) {
    "playing" -> context.getString(R.string.game_status_playing)
    "completed" -> context.getString(R.string.game_status_completed)
    "dropped" -> context.getString(R.string.game_status_dropped)
    "backlog" -> context.getString(R.string.game_status_backlog)
    else -> status
}

/**
 * Formats an epoch-millisecond timestamp (playedAt/addedAt), used by the "completed"/
 * "favorite" date badge shown on the account page. Delegates to DateUtils.formatShortDate,
 * the format shared with the cinema/books account pages.
 */
private fun Long.toReadableDateMillis(): String = DateUtils.formatShortDate(this)