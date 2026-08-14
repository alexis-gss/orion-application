package com.orion.app.cinema.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orion.app.OrionApplication
import kotlinx.coroutines.flow.first

/** Daily background worker that refreshes stale followed movies/TV shows in the cinema domain. See [OrionApplication.schedulePeriodicRefresh]. */
class CinemaRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    /** Refreshes every followed item once (forced), skipping entirely if no TMDB API key is set yet. */
    override suspend fun doWork(): Result {
        val app = applicationContext as OrionApplication
        // Do nothing until an API key is set.
        if (app.apiKeyStore.apiKey.value.isNullOrBlank()) {
            return Result.success()
        }
        return try {
            val repo = app.repository
            val followed = repo.observeFollowed()
            // Only a single snapshot is read: no need to continuously collect the Flow here.
            val snapshot = followed.first()
            snapshot.forEach { item ->
                repo.refreshFollowedIfStale(item, staleAfterMillis = 0L) // forced, we're already in the daily job
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
