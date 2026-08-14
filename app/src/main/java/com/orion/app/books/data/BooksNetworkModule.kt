package com.orion.app.books.data

import com.orion.app.core.data.BaseNetworkModule
import android.content.Context
import com.orion.app.core.data.BooksApiKeyStore
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.Semaphore
import kotlin.random.Random

/**
 * Network module for the books domain, now wired to Hardcover's GraphQL API
 * (https://api.hardcover.app/v1/graphql) rather than Google Books. A single POST entry
 * point for every request (search, detail, series...), unlike Google Books which had
 * separate REST paths — hence no per-path disk cache here: OkHttp never caches POST
 * responses anyway, regardless of the returned Cache-Control, so the old implementation's
 * CacheControlInterceptor logic wouldn't have been of any use on this domain.
 */
object BooksNetworkModule {

    private const val BASE_URL = "https://api.hardcover.app/"
    private const val CACHE_DIR_NAME = "books_http_cache"

    /** Authorization: Bearer <token>, as documented by Hardcover. */
    private class AuthInterceptor(private val apiKeyStore: BooksApiKeyStore) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = apiKeyStore.apiKey.value?.trim().orEmpty()
            val builder = chain.request().newBuilder()
                .header("User-Agent", "Orion-App (Android, personal use)")

            if (token.isNotEmpty()) {
                // Make sure "Bearer " isn't already present in the stored token
                val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
                builder.header("Authorization", authHeader)
            }

            return chain.proceed(builder.build())
        }
    }

    private class RawAuthInterceptor(private val token: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val newRequest = chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "Orion-App (Android, personal use)")
                .build()
            return chain.proceed(newRequest)
        }
    }

    /**
     * Hardcover documents strict rate limits (burst of 10, 60/min on the free plan): real
     * concurrent calls are spread out rather than all being allowed to fire at once (e.g.
     * a book page that triggers detail + similar books + series volumes almost
     * simultaneously).
     */
    private class ConcurrencyLimitInterceptor(maxConcurrent: Int = 3) : Interceptor {
        private val semaphore = Semaphore(maxConcurrent)
        override fun intercept(chain: Interceptor.Chain): Response {
            semaphore.acquire()
            try {
                return chain.proceed(chain.request())
            } finally {
                semaphore.release()
            }
        }
    }

    /** Retries 429 (rate-limit)/503/5xx with backoff + jitter, honors Retry-After
     *  (Hardcover explicitly returns it on 429, see its docs). */
    private class RetryInterceptor(
        private val maxRetries: Int = 4,
        private val baseDelayMillis: Long = 500L,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var attempt = 0
            var lastResponse: Response? = null
            var lastException: IOException? = null

            while (attempt <= maxRetries) {
                lastResponse?.close()
                try {
                    val response = chain.proceed(request)
                    if (response.isSuccessful || !shouldRetry(response)) {
                        return response
                    }
                    lastResponse = response
                    lastException = null
                } catch (e: IOException) {
                    lastResponse = null
                    lastException = e
                }

                attempt++
                if (attempt > maxRetries) break

                val retryAfterSeconds = lastResponse?.header("Retry-After")?.toLongOrNull()
                val backoffMs = retryAfterSeconds?.let { it * 1000L }
                    ?: (baseDelayMillis * (1L shl (attempt - 1)) + Random.nextLong(0, 150))
                try {
                    Thread.sleep(backoffMs)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }

            return lastResponse
                ?: throw (lastException ?: IOException("Hardcover: failed after $maxRetries attempts"))
        }

        private fun shouldRetry(response: Response): Boolean =
            response.code == 429 || response.code in 500..599
    }

    /** One-off client with a hardcoded token, for testing a key before it gets
     *  persisted (login screen / Settings), without ever saving an invalid token. */
    fun provideRawApi(apiKey: String): HardcoverApi {
        val retrofit = BaseNetworkModule.buildRawRetrofit(
            baseUrl = BASE_URL,
            interceptors = listOf(RawAuthInterceptor(apiKey)),
        )
        return retrofit.create(HardcoverApi::class.java)
    }

    /** Builds the Hardcover [okhttp3.OkHttpClient]-backed API client with auth, concurrency limiting, and retry logic. */
    fun provideApi(context: Context, apiKeyStore: BooksApiKeyStore): HardcoverApi {
        val retrofit = BaseNetworkModule.buildRetrofit(
            context = context,
            baseUrl = BASE_URL,
            cacheDirName = CACHE_DIR_NAME,
            // ConcurrencyLimitInterceptor and RetryInterceptor potentially call
            // chain.proceed() several times: not allowed for an OkHttp network interceptor,
            // hence application interceptors only here (see the same historical note in
            // the old Google Books implementation).
            interceptors = listOf(
                AuthInterceptor(apiKeyStore),
                ConcurrencyLimitInterceptor(),
                RetryInterceptor(),
            ),
        )
        return retrofit.create(HardcoverApi::class.java)
    }
}
