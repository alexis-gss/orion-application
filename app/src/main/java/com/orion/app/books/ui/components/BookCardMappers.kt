package com.orion.app.books.ui.components

import android.content.Context
import com.orion.app.R
import com.orion.app.books.data.FavoriteBook
import com.orion.app.books.data.FollowedBook
import com.orion.app.books.data.HardcoverBook
import com.orion.app.books.data.ReadBook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dates are always rendered in French (see [com.orion.app.core.util.DateUtils]'s class doc), independent of the app's English UI. */
private val targetLocale = Locale.US

fun HardcoverBook.toCardData(subtitle: String? = authorsLabel ?: year): BookCardData = BookCardData(
    key = "hardcover_$id",
    volumeId = id,
    title = name,
    coverUrl = coverUrl,
    subtitle = subtitle,
    rating = displayRating,
    genres = primaryCategory,
    releaseDate = publishedTimestamp?.let { it * 1000 }
)

fun FollowedBook.toCardData(context: Context): BookCardData = BookCardData(
    key = "followed_$volumeId",
    volumeId = volumeId,
    title = title,
    coverUrl = coverUrl,
    subtitle = releaseLabel ?: releaseTimestamp?.toReadableDate(),
    trailingText = if (isReleased) null else context.getString(R.string.book_status_upcoming),
    // Same convention as ReadBook.toCardData: the generic segment (before " / ") of each
    // CSV category, deduplicated — without this, BooksLibraryScreen's genre picker stayed
    // empty even though followed books did have known categories.
    genres = categories?.split(",")
        ?.map { it.trim().substringBefore(" / ") }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        ?.joinToString(","),
    releaseDate = releaseTimestamp?.let { it * 1000 }
)

fun ReadBook.toCardData(context: Context): BookCardData = BookCardData(
    key = "read_$id",
    volumeId = volumeId,
    title = title,
    coverUrl = coverUrl,
    subtitle = statusLabel(context, status),
    // Date the book was marked "read" (last status change), more useful here than the
    // page count, which adds nothing once the book has already been read.
    trailingText = updatedAt.toReadableDateFromMillis(),
    // Same convention as BooksStatsScreen.BooksCategoryBreakdownCard: the generic segment
    // (before " / ") of each CSV category, deduplicated.
    genres = categories?.split(",")
        ?.map { it.trim().substringBefore(" / ") }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        ?.joinToString(","),
    addedAt = updatedAt
)

fun FavoriteBook.toCardData(dateLabel: String? = null): BookCardData = BookCardData(
    key = "favorite_$volumeId",
    volumeId = volumeId,
    title = title,
    coverUrl = coverUrl,
    trailingText = dateLabel,
    genres = categories?.split(",")
        ?.map { it.trim().substringBefore(" / ") }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        ?.joinToString(","),
    addedAt = addedAt
)

private fun statusLabel(context: Context, status: String): String = when (status) {
    "reading" -> context.getString(R.string.book_status_reading)
    "read" -> context.getString(R.string.book_read_label)
    "dropped" -> context.getString(R.string.game_status_dropped)
    "to_read" -> context.getString(R.string.book_status_to_read)
    else -> status
}

private fun Long.toReadableDate(): String =
    SimpleDateFormat("d MMM yyyy", targetLocale).format(Date(this * 1000))

/** Same as [toReadableDate], but for a timestamp already in milliseconds (e.g. ReadBook.updatedAt), unlike releaseTimestamp/publishedTimestamp which are in seconds. */
private fun Long.toReadableDateFromMillis(): String =
    SimpleDateFormat("d MMM yyyy", targetLocale).format(Date(this))
