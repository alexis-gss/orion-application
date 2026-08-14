package com.orion.app

import android.app.Application
import androidx.work.*
import com.orion.app.core.data.ApiKeyStore
import com.orion.app.cinema.data.AppDatabase
import com.orion.app.core.data.BooksApiKeyStore
import com.orion.app.core.data.IgdbCredentialsStore
import com.orion.app.core.data.NetworkModule
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.cinema.worker.CinemaRefreshWorker
import com.orion.app.games.data.GamesDatabase
import com.orion.app.games.data.GamesRepository
import com.orion.app.games.data.IgdbNetworkModule
import com.orion.app.games.data.IgdbTokenStore
import com.orion.app.games.worker.GamesRefreshWorker
import com.orion.app.books.data.BooksDatabase
import com.orion.app.books.data.BooksNetworkModule
import com.orion.app.books.data.BooksRepository
import com.orion.app.books.worker.BooksRefreshWorker
import com.orion.app.core.data.NotificationPreferenceStore
import com.orion.app.core.worker.ReleaseNotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Application-wide dependency container. Builds and owns every repository, API-key/credential
 * store, and database instance for the three domains (Cinema/TMDB, Games/IGDB, Books/Hardcover),
 * plus the shared notification preferences. Everything lives here (instead of a DI framework)
 * so the same instances survive Activity/Composable recreation and stay reachable from
 * background Workers.
 */
class OrionApplication : Application() {

    // ---------- Cinema ----------
    lateinit var repository: CinemaRepository
        private set
    lateinit var apiKeyStore: ApiKeyStore
        private set

    // ---------- Video games ----------
    lateinit var gamesRepository: GamesRepository
        private set
    lateinit var igdbCredentialsStore: IgdbCredentialsStore
        private set

    // ---------- Books ----------
    lateinit var booksRepository: BooksRepository
        private set
    lateinit var booksApiKeyStore: BooksApiKeyStore
        private set

    // ---------- Release notifications ----------
    lateinit var notificationPreferenceStore: NotificationPreferenceStore
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Wires up every domain's storage + network + repository, then schedules the background workers. */
    override fun onCreate() {
        super.onCreate()

        // --- Cinema ---
        apiKeyStore = ApiKeyStore.getInstance(this)
        val db = AppDatabase.getInstance(this)
        // The API key isn't fixed at compile time: the network interceptor re-reads it on
        // every call, which lets it be changed on the fly from the Settings screen without
        // restarting the app.
        val api = NetworkModule.provideApi(this, apiKeyStore = apiKeyStore)
        repository = CinemaRepository(api, db)

        // --- Video games ---
        // Database, credentials, and token entirely separate from cinema: the two domains
        // share no state, only the infrastructure (theme, generic networking).
        igdbCredentialsStore = IgdbCredentialsStore.getInstance(this)
        val igdbTokenStore = IgdbTokenStore.getInstance(this)
        val gamesDb = GamesDatabase.getInstance(this)
        val igdbApi = IgdbNetworkModule.provideApi(this, credentialsStore = igdbCredentialsStore, tokenStore = igdbTokenStore)
        gamesRepository = GamesRepository(igdbApi, gamesDb)

        // --- Books ---
        // Same isolation principle: dedicated database and key, no state shared with cinema
        // or games. Hardcover only needs a single personal token (no OAuth flow to renew
        // like IGDB), so there is no equivalent to IgdbTokenStore here.
        booksApiKeyStore = BooksApiKeyStore.getInstance(this)
        val booksDb = BooksDatabase.getInstance(this)
        val booksApi = BooksNetworkModule.provideApi(this, apiKeyStore = booksApiKeyStore)
        booksRepository = BooksRepository(booksApi, booksDb)

        schedulePeriodicRefresh()

        // --- Release notifications ---
        notificationPreferenceStore = NotificationPreferenceStore(this, appScope)
        scheduleReleaseNotifications()
        // The store reads its values from DataStore asynchronously (see its init{} block):
        // we observe its change signal to (re)schedule as soon as the user enables/disables
        // the feature or changes the time from Settings, including the very first load once
        // the persisted values have been read.
        appScope.launch {
            notificationPreferenceStore.onChanged.collect {
                scheduleReleaseNotifications()
            }
        }
    }

    /**
     * (Re)schedules the release-notification worker to run once a day, as close as possible
     * to the time chosen by the user. WorkManager doesn't guarantee an exact time (system
     * constraints, Doze mode...) but setInitialDelay aligns the first run with the right hour,
     * after which the 24h interval keeps it there. ExistingPeriodicWorkPolicy.REPLACE (unlike
     * the other refresh jobs, which use KEEP) is required here: this job must be reschedulable
     * to a new time on every preference change, not scheduled only once.
     */
    fun scheduleReleaseNotifications() {
        if (!notificationPreferenceStore.isEnabled.value) {
            WorkManager.getInstance(this).cancelUniqueWork("release_notifications")
            return
        }
        val time = notificationPreferenceStore.time.value
        val initialDelayMillis = computeInitialDelayMillis(time.hour, time.minute)

        val request = PeriodicWorkRequestBuilder<ReleaseNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "release_notifications",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    /** Computes the delay (in ms) until the next occurrence of [targetHour]:[targetMinute], rolling over to tomorrow if that time has already passed today. */
    private fun computeInitialDelayMillis(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    /**
     * A single batched network call per tracked item, at most once a day, rather than on
     * every app launch: this is the main lever for staying within API quotas (TMDB, IGDB,
     * and Hardcover).
     */
    private fun schedulePeriodicRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val cinemaRequest = PeriodicWorkRequestBuilder<CinemaRefreshWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "refresh_followed_items",
            ExistingPeriodicWorkPolicy.KEEP,
            cinemaRequest
        )

        val gamesRequest = PeriodicWorkRequestBuilder<GamesRefreshWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "refresh_followed_games",
            ExistingPeriodicWorkPolicy.KEEP,
            gamesRequest
        )

        val booksRequest = PeriodicWorkRequestBuilder<BooksRefreshWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "refresh_followed_books",
            ExistingPeriodicWorkPolicy.KEEP,
            booksRequest
        )
    }
}
