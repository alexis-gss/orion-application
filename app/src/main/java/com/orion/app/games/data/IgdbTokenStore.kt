package com.orion.app.games.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Fetches and caches the Twitch app token required to query IGDB. The token is valid
 * for ~60 days; it is renewed 1h before expiry so a user call is never blocked on an
 * avoidable synchronous auth round trip.
 */
class IgdbTokenStore private constructor(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext)

    private val authApi: IgdbAuthApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl("https://id.twitch.tv/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(IgdbAuthApi::class.java)
    }

    /** Returns the cached token, if any, regardless of whether it has expired. */
    fun getCachedToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** True if a cached token exists and won't expire within the next hour. */
    fun isTokenValid(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return getCachedToken() != null && System.currentTimeMillis() < expiresAt - TimeUnit.HOURS.toMillis(1)
    }

    /** Forces a fresh call to Twitch to obtain a token, and caches it. */
    suspend fun refreshToken(clientId: String, clientSecret: String): String {
        val response = authApi.getAppAccessToken(clientId, clientSecret)
        val expiresAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(response.expiresIn)
        prefs.edit()
            .putString(KEY_TOKEN, response.accessToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
        return response.accessToken
    }

    /**
     * Verifies credentials without persisting anything: used by the login screen to
     * test before saving. Touches neither the in-memory cache nor the preferences.
     */
    suspend fun fetchTokenWithoutCaching(clientId: String, clientSecret: String): String {
        return authApi.getAppAccessToken(clientId, clientSecret).accessToken
    }

    /** Returns a valid token, renewing it if needed. */
    suspend fun getValidToken(clientId: String, clientSecret: String): String {
        if (isTokenValid()) return getCachedToken()!!
        return refreshToken(clientId, clientSecret)
    }

    /** Wipes the cached token, forcing the next call to fetch a fresh one. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "orion_igdb_token_secure"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_EXPIRES_AT = "expires_at"

        @Volatile private var INSTANCE: IgdbTokenStore? = null

        /** Returns the process-wide singleton, creating it on first access (double-checked locking). */
        fun getInstance(context: Context): IgdbTokenStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: IgdbTokenStore(context.applicationContext).also { INSTANCE = it }
            }

        /** Builds the encrypted (AES256-GCM/SIV) SharedPreferences backing this store. */
        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
