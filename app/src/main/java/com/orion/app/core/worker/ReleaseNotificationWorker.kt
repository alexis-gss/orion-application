package com.orion.app.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orion.app.OrionApplication
import com.orion.app.core.notification.ReleaseNotificationHelper
import com.orion.app.core.notification.TodayReleaseItem
import com.orion.app.core.util.DateUtils
import kotlinx.coroutines.flow.first

/**
 * Runs once a day, at the time chosen by the user (see NotificationPreferenceStore +
 * OrionApplication.scheduleReleaseNotifications), and groups into a single notification
 * everything releasing TODAY among the followed items across all three domains: movies,
 * TV episodes, video games, books.
 *
 * Makes no network call: relies only on data already refreshed in the local database by
 * RefreshWorker/GamesRefreshWorker/BooksRefreshWorker (which also run once every 24h), to
 * stay lightweight and avoid consuming any extra API quota.
 */
class ReleaseNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as OrionApplication
        if (!app.notificationPreferenceStore.isEnabled.value) {
            return Result.success()
        }

        return try {
            val releasedMovies = mutableListOf<TodayReleaseItem>()
            val releasedEpisodes = mutableListOf<TodayReleaseItem>()
            val releasedGames = mutableListOf<TodayReleaseItem>()
            val releasedBooks = mutableListOf<TodayReleaseItem>()

            val followedCinema = app.repository.observeFollowed().first()
            followedCinema.forEach { item ->
                when {
                    item.mediaType == "movie" && DateUtils.isIsoDateToday(item.releaseDate) ->
                        releasedMovies += TodayReleaseItem(item.title, "movie")
                    item.mediaType == "tv" && DateUtils.isIsoDateToday(item.nextAirDate) ->
                        releasedEpisodes += TodayReleaseItem(
                            item.nextEpisodeName?.let { "${item.title} — $it" } ?: item.title,
                            "episode"
                        )
                }
            }

            val followedGames = app.gamesRepository.observeFollowed().first()
            followedGames.forEach { item ->
                if (DateUtils.isEpochSecondsToday(item.releaseTimestamp)) {
                    releasedGames += TodayReleaseItem(item.title, "video game")
                }
            }

            val followedBooks = app.booksRepository.observeFollowed().first()
            followedBooks.forEach { item ->
                if (DateUtils.isEpochSecondsToday(item.releaseTimestamp)) {
                    releasedBooks += TodayReleaseItem(item.title, "book")
                }
            }

            val all = releasedMovies + releasedEpisodes + releasedGames + releasedBooks
            ReleaseNotificationHelper.notifyTodayReleases(
                context = applicationContext,
                movieCount = releasedMovies.size,
                episodeCount = releasedEpisodes.size,
                gameCount = releasedGames.size,
                bookCount = releasedBooks.size,
                singleItem = all.singleOrNull()
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
