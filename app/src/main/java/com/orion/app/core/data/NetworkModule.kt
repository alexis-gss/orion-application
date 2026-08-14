package com.orion.app.core.data

import android.content.Context
import com.orion.app.cinema.data.TmdbApi
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * TMDB (cinema domain) network module. Builds the [TmdbApi] Retrofit client, delegating
 * the generic OkHttp/Retrofit/Json wiring to [BaseNetworkModule] and keeping only the
 * TMDB-specific pieces here: the API-key query param, the language override, and the
 * per-endpoint cache duration policy.
 */
object NetworkModule {

    private const val BASE_URL = "https://api.themoviedb.org/3/"
    private const val API_KEY_PARAM = "api_key"
    private const val CACHE_DIR_NAME = "tmdb_http_cache"

    /** Appends `?api_key=<key>` to every request, unless the key is already present in the URL (e.g. a manual key-validation probe). */
    private class ApiKeyInterceptor(private val apiKeyStore: ApiKeyStore) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()

            // If the key is already in the URL (e.g. a validation probe), don't replace it
            if (original.url.queryParameter(API_KEY_PARAM) != null) {
                return chain.proceed(original)
            }

            val key = apiKeyStore.apiKey.value.orEmpty()
            val newUrl = original.url.newBuilder()
                .addQueryParameter(API_KEY_PARAM, key)
                .build()
            return chain.proceed(original.newBuilder().url(newUrl).build())
        }
    }

    /** Rewrites the response Cache-Control header per-endpoint so TMDB's own headers don't dictate our offline cache lifetime. Must run as a network interceptor to see the raw response before OkHttp persists it to disk. */
    private class CacheControlInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val path = request.url.encodedPath
            val response = chain.proceed(request)

            // Do NOT cache error responses (non-2xx HTTP codes such as 401)
            if (!response.isSuccessful) {
                return response
            }

            val maxAgeSeconds = when {
                path.contains("/configuration") -> TimeUnit.DAYS.toSeconds(30)
                path.contains("/search/") -> TimeUnit.HOURS.toSeconds(1)
                path.contains("/season/") -> TimeUnit.DAYS.toSeconds(3)
                else -> TimeUnit.DAYS.toSeconds(3)
            }

            return response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", CacheControl.Builder().maxAge(maxAgeSeconds.toInt(), TimeUnit.SECONDS).build().toString())
                .build()
        }
    }

    /** Builds the TMDB [Retrofit] client. Content language is fixed to English (see [Tmdb]'s default `language` query params) so no runtime language interceptor is needed. */
    fun provideApi(context: Context, apiKeyStore: ApiKeyStore): TmdbApi {
        val retrofit = BaseNetworkModule.buildRetrofit(
            context = context,
            baseUrl = BASE_URL,
            cacheDirName = CACHE_DIR_NAME,
            interceptors = listOf(ApiKeyInterceptor(apiKeyStore)),
            networkInterceptors = listOf(CacheControlInterceptor()),
        )
        return retrofit.create(TmdbApi::class.java)
    }
}
