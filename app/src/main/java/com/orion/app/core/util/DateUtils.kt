package com.orion.app.core.util

import android.content.Context
import com.orion.app.R
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

/**
 * TMDB only provides a release DATE (no precise time — networks/platforms don't communicate
 * one reliably or consistently).
 *
 * IMPORTANT — language: day and month names produced here are intentionally always in English
 * (via [targetLocale]), independent of the app's UI language (English) or the device's system
 * locale. Only the surrounding labels (e.g. "Today", "Tomorrow") come from English string
 * resources.
 */
object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val targetZone: ZoneId = ZoneId.of("Europe/Paris")
    private val targetLocale: Locale = Locale.US

    /**
     * Short "d MMM yyyy" format (e.g. "14 août 2026") used for date pills (added on / watched
     * on / finished on...) on account pages. Shared by all three domains (cinema, games, books)
     * to keep this display consistent everywhere in the app.
     */
    fun formatShortDate(timestampMillis: Long): String =
        SimpleDateFormat("d MMM yyyy", targetLocale).format(Date(timestampMillis))

    /**
     * Formats an ISO date (yyyy-MM-dd) as a full English day label, e.g. "Vendredi 14 août 2026".
     * "Today" and "Tomorrow" come from localized (English) string resources; the day-of-week and
     * month names are always rendered in English (see the class-level note above), independent
     * of the app's UI locale.
     */
    fun formatDay(context: Context, isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return context.getString(R.string.date_unknown)
        return try {
            val date = LocalDate.parse(isoDate, isoFormatter)
            val today = LocalDate.now(targetZone)
            when {
                date.isEqual(today) -> context.getString(R.string.date_today)
                date.isEqual(today.plusDays(1)) -> context.getString(R.string.date_tomorrow)
                else -> {
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, targetLocale)
                        .replaceFirstChar { it.uppercase(targetLocale) }
                    val monthName = date.month.getDisplayName(TextStyle.FULL, targetLocale)
                    if (date.year == today.year) {
                        context.getString(R.string.date_format_same_year, dayName, date.dayOfMonth, monthName)
                    } else {
                        context.getString(R.string.date_format_other_year, dayName, date.dayOfMonth, monthName, date.year)
                    }
                }
            }
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Short label for the number of days remaining before release, shown on the right of each
     * Planning row (the full day is shown only once, in the group header, by PlanningScreen).
     */
    fun daysRemainingLabel(context: Context, isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return context.getString(R.string.days_remaining_unknown)
        return try {
            val date = LocalDate.parse(isoDate, isoFormatter)
            val today = LocalDate.now(targetZone)
            val days = ChronoUnit.DAYS.between(today, date)
            when {
                days < 0 -> context.getString(R.string.days_remaining_released)
                days == 0L -> context.getString(R.string.days_remaining_today)
                days == 1L -> context.getString(R.string.days_remaining_tomorrow)
                else -> context.getString(R.string.days_remaining_days, days)
            }
        } catch (e: Exception) {
            context.getString(R.string.days_remaining_unknown)
        }
    }

    /**
     * Check if element is already released.
     */
    fun isReleased(isoDate: String?): Boolean {
        if (isoDate.isNullOrBlank()) return false
        return try {
            LocalDate.parse(isoDate, isoFormatter).isBefore(LocalDate.now(targetZone))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if an episode can be marked as watched: already aired or airing today.
     * Unlike isReleased (strictly before today), this includes the air date itself.
     */
    fun isWatchable(isoDate: String?): Boolean {
        if (isoDate.isNullOrBlank()) return false
        return try {
            !LocalDate.parse(isoDate, isoFormatter).isAfter(LocalDate.now(targetZone))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * True if the ISO date (yyyy-MM-dd, TMDB format) falls exactly today (Europe/Paris time
     * zone). Used by the release-notification worker to spot followed movies/episodes
     * releasing on the current day.
     */
    fun isIsoDateToday(isoDate: String?): Boolean {
        if (isoDate.isNullOrBlank()) return false
        return try {
            LocalDate.parse(isoDate, isoFormatter).isEqual(LocalDate.now(targetZone))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Equivalent of [isIsoDateToday] for an epoch-seconds timestamp (games/books, which store
     * their release date this way rather than as an ISO string).
     */
    fun isEpochSecondsToday(epochSeconds: Long?): Boolean {
        if (epochSeconds == null) return false
        return try {
            val date = java.time.Instant.ofEpochSecond(epochSeconds).atZone(targetZone).toLocalDate()
            date.isEqual(LocalDate.now(targetZone))
        } catch (e: Exception) {
            false
        }
    }
}
