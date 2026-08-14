package com.orion.app.games.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orion.app.OrionApplication
import kotlinx.coroutines.flow.first

class GamesRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as OrionApplication
        if (app.igdbCredentialsStore.credentials.value == null) {
            return Result.success()
        }
        return try {
            val repo = app.gamesRepository
            val snapshot = repo.observeFollowed().first()
            snapshot.forEach { item ->
                repo.refreshFollowedIfStale(item, staleAfterMillis = 0L)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
