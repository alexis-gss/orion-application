package com.orion.app.games.data

import com.orion.app.core.data.BaseNetworkModule
import android.content.Context
import com.orion.app.core.data.IgdbCredentialsStore
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * IGDB (games domain) network module. Builds the [IgdbApi] Retrofit client, delegating the
 * generic OkHttp/Retrofit/Json wiring to [BaseNetworkModule] and keeping only the
 * IGDB-specific pieces here: Twitch Bearer auth with automatic token refresh on 401, and
 * the fixed cache duration policy.
 */
object IgdbNetworkModule {

    private const val BASE_URL = "https://api.igdb.com/v4/"
    private const val CACHE_DIR_NAME = "igdb_http_cache"

    /** Adds Client-ID and Authorization (Bearer <app token>) to every IGDB request. */
    private class IgdbAuthInterceptor(
        private val credentialsStore: IgdbCredentialsStore,
        private val tokenStore: IgdbTokenStore
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val creds = credentialsStore.credentials.value
                ?: return chain.proceed(original) // will let the call fail cleanly (401) if not configured yet

            // Intentionally blocking call: this interceptor already runs on OkHttp's
            // network thread (never the main thread), and the token is cached for 60 days,
            // so this network path is only taken once every ~60 days.
            val token = runBlocking { tokenStore.getValidToken(creds.clientId, creds.clientSecret) }

            val authed = original.newBuilder()
                .header("Client-ID", creds.clientId)
                .header("Authorization", "Bearer $token")
                .build()
            val response = chain.proceed(authed)

            // The local cache can lie (token revoked on Twitch's side, secret changed,
            // etc.): if IGDB returns 401 despite a "valid" token according to our cache,
            // force a renewal and retry exactly once with the new token.
            if (response.code == 401) {
                response.close()
                val freshToken = runBlocking { tokenStore.refreshToken(creds.clientId, creds.clientSecret) }
                val retried = original.newBuilder()
                    .header("Client-ID", creds.clientId)
                    .header("Authorization", "Bearer $freshToken")
                    .build()
                return chain.proceed(retried)
            }

            return response
        }
    }

    /** Fixed 6-hour cache lifetime for every IGDB response (no per-endpoint policy needed: unlike TMDB, IGDB doesn't mix near-static and highly volatile endpoints behind the same client). */
    private class CacheControlInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            if (!response.isSuccessful) return response
            return response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header(
                    "Cache-Control",
                    CacheControl.Builder().maxAge(TimeUnit.HOURS.toSeconds(6).toInt(), TimeUnit.SECONDS).build().toString()
                )
                .build()
        }
    }

    /**
     * One-off IGDB client with a hardcoded client_id and token, bypassing
     * IgdbCredentialsStore and IgdbTokenStore entirely. Used only to test credentials
     * before they get persisted (login screen), so invalid credentials are never saved.
     * No disk cache on purpose: a probe must always hit the network.
     */
    fun provideRawApi(clientId: String, accessToken: String): IgdbApi {
        val authInterceptor = Interceptor { chain ->
            val authed = chain.request().newBuilder()
                .header("Client-ID", clientId)
                .header("Authorization", "Bearer $accessToken")
                .build()
            chain.proceed(authed)
        }
        val retrofit = BaseNetworkModule.buildRawRetrofit(
            baseUrl = BASE_URL,
            interceptors = listOf(authInterceptor),
        )
        return retrofit.create(IgdbApi::class.java)
    }

    /** Builds the IGDB [okhttp3.OkHttpClient]-backed API client with Twitch auth (auto-refreshing on 401) and a fixed 6h cache. */
    fun provideApi(context: Context, credentialsStore: IgdbCredentialsStore, tokenStore: IgdbTokenStore): IgdbApi {
        // NOTE (maintainability): a verbose HttpLoggingInterceptor(Level.BODY) used to be
        // wired in here unconditionally, which logs full request/response bodies (and thus
        // credentials/tokens) to Logcat on every build, including release. It has been
        // removed; if request/response logging is needed again for debugging, gate it
        // behind BuildConfig.DEBUG so it never ships in a release build.
        val retrofit = BaseNetworkModule.buildRetrofit(
            context = context,
            baseUrl = BASE_URL,
            cacheDirName = CACHE_DIR_NAME,
            interceptors = listOf(IgdbAuthInterceptor(credentialsStore, tokenStore)),
            networkInterceptors = listOf(CacheControlInterceptor()),
        )
        return retrofit.create(IgdbApi::class.java)
    }
}
