package com.orion.app.books.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orion.app.OrionApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Delay between two consecutive Hardcover calls made by this worker. A background job has
 * no reason to fire its calls as fast as possible: spacing them out leaves headroom on the
 * API's short-term rate limit for interactive usage (search, book pages) that may be
 * running concurrently on the user's side.
 */
private const val DELAY_BETWEEN_CALLS_MS = 400L

/** Daily background worker that refreshes stale followed books in the books domain. See [OrionApplication.schedulePeriodicRefresh]. */
class BooksRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as OrionApplication
        if (app.booksApiKeyStore.apiKey.value == null) {
            return Result.success()
        }
        val repo = app.booksRepository
        val snapshot = try {
            repo.observeFollowed().first()
        } catch (e: Exception) {
            return Result.retry()
        }

        snapshot.forEachIndexed { index, item ->
            try {
                repo.refreshFollowedIfStale(item)
            } catch (e: Exception) {
                // Move on to the next book rather than abandoning the whole batch.
            }
            if (index < snapshot.lastIndex) delay(DELAY_BETWEEN_CALLS_MS)
        }
        return Result.success()
    }
}