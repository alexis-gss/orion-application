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
            // Each book is isolated in its own try/catch: a one-off failure (DB or network,
            // the latter already swallowed by getBookDetail) must not fail the whole batch
            // and force WorkManager to retry books that were already processed.
            try {
                // Default staleAfterMillis (not 0L): this worker already runs at most once
                // every 24h (see schedulePeriodicRefresh), so forcing a refresh every time
                // here would be redundant, and on a close re-run (restart, waking from
                // extended sleep, WorkManager retry) would needlessly redo calls for books
                // that were checked recently.
                repo.refreshFollowedIfStale(item)
            } catch (e: Exception) {
                // Move on to the next book rather than abandoning the whole batch.
            }
            if (index < snapshot.lastIndex) delay(DELAY_BETWEEN_CALLS_MS)
        }
        return Result.success()
    }
}