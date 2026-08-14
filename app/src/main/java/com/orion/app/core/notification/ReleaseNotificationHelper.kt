package com.orion.app.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.orion.app.MainActivity
import com.orion.app.R

private const val CHANNEL_ID = "release_notifications"
private const val NOTIFICATION_ID = 4242

/**
 * A followed item releasing today, used to build either a grouped summary
 * ("3 movies, 1 episode...") or a detailed notification when only a single item releases
 * that day (across all categories).
 */
data class TodayReleaseItem(
    val name: String,
    /** Already-localized category label ("movie", "episode", "video game", "book"). */
    val categoryLabel: String
)

/**
 * Builds and shows the "today's releases" notification, grouped by category (movies, TV
 * episodes, video games, books) and omitting categories with a zero count. If only a single
 * item releases that day, the notification is detailed (its name included) rather than
 * reduced to a plain count.
 */
object ReleaseNotificationHelper {

    /** Creates the notification channel on Android 8+ (API 26), a no-op if it already exists. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    /** Builds the grouped or single-item notification text and posts it, if there is anything to release today. */
    fun notifyTodayReleases(
        context: Context,
        movieCount: Int,
        episodeCount: Int,
        gameCount: Int,
        bookCount: Int,
        singleItem: TodayReleaseItem?
    ) {
        val total = movieCount + episodeCount + gameCount + bookCount
        if (total <= 0) return

        // On Android 13+ (API 33), POST_NOTIFICATIONS is a runtime permission: bail out
        // early if it hasn't been granted instead of building a notification we can't post.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

        val title = context.getString(R.string.notification_title)
        val text: String
        if (total == 1 && singleItem != null) {
            text = context.getString(R.string.notification_single_item, singleItem.name, singleItem.categoryLabel)
        } else {
            val resources = context.resources
            val parts = mutableListOf<String>()
            if (movieCount > 0) parts += resources.getQuantityString(R.plurals.notif_movie_count, movieCount, movieCount)
            if (episodeCount > 0) parts += resources.getQuantityString(R.plurals.notif_episode_count, episodeCount, episodeCount)
            if (gameCount > 0) parts += resources.getQuantityString(R.plurals.notif_game_count, gameCount, gameCount)
            if (bookCount > 0) parts += resources.getQuantityString(R.plurals.notif_book_count, bookCount, bookCount)
            text = parts.joinToString(", ")
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = try {
            context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info
        } catch (e: Exception) {
            android.R.drawable.ic_dialog_info
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // The permission check above already guards this on API 33+; the catch is a
        // defensive fallback (e.g. OEM quirks or the permission being revoked mid-call)
        // so the background worker never crashes over a missed notification.
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted: nothing to notify, the worker finishes normally.
        }
    }
}