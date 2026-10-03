package com.orion.app.core.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Interceptor
import okhttp3.Response
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tracks how many real network requests each domain (TMDB/cinema, IGDB/games,
 * Hardcover/books) has made "today", so Settings can show a simple quota counter. Counts
 * are wired in as a network interceptor (see [interceptorFor]) so only requests that
 * actually hit the network are counted — a response served from OkHttp's on-disk cache
 * never reaches a network interceptor, so it correctly doesn't count against the quota.
 *
 * Persisted in SharedPreferences, keyed by domain + today's date, so the counter resets
 * itself automatically at midnight without needing an explicit reset job: once the date
 * rolls over, that day's key has never been written and simply reads back as 0.
 */
object ApiQuotaTracker {
    const val DOMAIN_CINEMA = "cinema"
    const val DOMAIN_GAMES = "games"
    const val DOMAIN_BOOKS = "books"

    private const val PREFS_NAME = "api_quota_tracker"
    // Same time zone as the rest of the app (see DateUtils); DateTimeFormatter is thread-safe,
    // unlike the SimpleDateFormat it replaces.
    private val zone: ZoneId = ZoneId.of("Europe/Paris")
    private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    private lateinit var prefs: SharedPreferences
    private val counts = MutableStateFlow<Map<String, Int>>(emptyMap())

    @Synchronized
    private fun ensureInit(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            counts.value = mapOf(
                DOMAIN_CINEMA to readCount(DOMAIN_CINEMA),
                DOMAIN_GAMES to readCount(DOMAIN_GAMES),
                DOMAIN_BOOKS to readCount(DOMAIN_BOOKS),
            )
        }
    }

    private fun todayKey(domain: String) = "$domain:${LocalDate.now(zone).format(dayFormat)}"

    private fun readCount(domain: String): Int = prefs.getInt(todayKey(domain), 0)

    @Synchronized
    private fun record(context: Context, domain: String) {
        ensureInit(context)
        val key = todayKey(domain)
        val next = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, next).apply()
        counts.value = counts.value.toMutableMap().apply { put(domain, next) }
    }

    /** Observable "requests made today" per domain, for the Settings screen. */
    fun countsFlow(context: Context): StateFlow<Map<String, Int>> {
        ensureInit(context)
        return counts
    }

    /**
     * A network interceptor (must be registered as a *network* interceptor, not an
     * application interceptor) that records one request against [domain] every time it
     * actually goes out over the network.
     */
    fun interceptorFor(context: Context, domain: String): Interceptor = Interceptor { chain ->
        val response: Response = chain.proceed(chain.request())
        record(context, domain)
        response
    }
}
