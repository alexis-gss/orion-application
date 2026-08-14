package com.orion.app.core.data

import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Shared Retrofit/OkHttp construction, factored out of the three domain network modules
 * (TMDB, IGDB, Hardcover), which used to duplicate this exact boilerplate with only the
 * base URL, auth interceptor(s), and cache policy differing between them.
 *
 * This does NOT try to unify the auth/cache-control interceptors themselves — those stay
 * domain-specific (TMDB uses a query-param key + language override, IGDB uses Bearer auth
 * with token refresh, Hardcover uses Bearer auth with a personal token) because forcing a
 * single interceptor shape across three very different APIs would hurt readability more
 * than it would save duplication. Only the "wire it all together" part is shared.
 */
object BaseNetworkModule {

    /** Default connect/read timeout applied to every domain client, in seconds. */
    private const val DEFAULT_TIMEOUT_SECONDS = 15L

    /** Default on-disk HTTP cache size applied to every domain client, in bytes (20 MB). */
    private const val DEFAULT_CACHE_SIZE_BYTES = 20L * 1024 * 1024

    /**
     * kotlinx.serialization Json instance shared by every domain: tolerant of fields the
     * API adds later (ignoreUnknownKeys) and does not emit explicit "field": null for
     * absent optional fields (explicitNulls = false), which keeps payloads/cache entries
     * smaller and matches how every domain's data classes declare their optional fields
     * with default values already.
     */
    @OptIn(ExperimentalSerializationApi::class)
    val defaultJson: Json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /**
     * Builds a Retrofit instance wired to a persistent disk cache under [cacheDirName]
     * inside the app's cache directory, with the given [interceptors] applied in order
     * (application interceptors, e.g. auth) and [networkInterceptors] applied after
     * (network interceptors, e.g. rewriting Cache-Control on the raw response — must run
     * as a network interceptor to see/modify the actual response before OkHttp's cache
     * layer persists it).
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun buildRetrofit(
        context: Context,
        baseUrl: String,
        cacheDirName: String,
        interceptors: List<Interceptor> = emptyList(),
        networkInterceptors: List<Interceptor> = emptyList(),
        cacheSizeBytes: Long = DEFAULT_CACHE_SIZE_BYTES,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    ): Retrofit {
        val cache = Cache(File(context.cacheDir, cacheDirName), cacheSizeBytes)
        val client = buildClient(interceptors, networkInterceptors, timeoutSeconds, cache)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(defaultJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    /**
     * Builds a Retrofit instance with no persistent cache. Used for the "test these
     * credentials before saving them" probes in each domain's connection gate screen,
     * where hitting the disk cache or reusing the app-wide client would be incorrect
     * (a probe must always hit the network with the candidate credentials, never serve a
     * cached response obtained with a previous, possibly different, key).
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun buildRawRetrofit(
        baseUrl: String,
        interceptors: List<Interceptor> = emptyList(),
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    ): Retrofit {
        val client = buildClient(interceptors, emptyList(), timeoutSeconds, cache = null)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(defaultJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private fun buildClient(
        interceptors: List<Interceptor>,
        networkInterceptors: List<Interceptor>,
        timeoutSeconds: Long,
        cache: Cache?,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        cache?.let { builder.cache(it) }
        interceptors.forEach { builder.addInterceptor(it) }
        networkInterceptors.forEach { builder.addNetworkInterceptor(it) }
        return builder.build()
    }
}
