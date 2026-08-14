package com.orion.app.core.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The two Twitch credentials required to authenticate IGDB calls. */
data class IgdbCredentials(val clientId: String, val clientSecret: String)

/**
 * Symmetric to [ApiKeyStore] but for IGDB, which requires a Twitch client_id /
 * client_secret pair (not a single key like TMDB) — this is why it does NOT extend
 * [SingleKeyStore] (single-value shape doesn't fit a two-field credential pair), but it
 * does reuse the same [createEncryptedPrefs] builder as every other store. See
 * games/data/IgdbTokenStore for how these credentials get exchanged for an access token.
 */
class IgdbCredentialsStore private constructor(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext, PREFS_NAME)

    private val _credentials = MutableStateFlow(loadStored())
    val credentials: StateFlow<IgdbCredentials?> = _credentials

    /** Reads both credential fields from encrypted prefs; returns null unless both are present. */
    private fun loadStored(): IgdbCredentials? {
        val id = prefs.getString(KEY_CLIENT_ID, null)?.takeIf { it.isNotBlank() }
        val secret = prefs.getString(KEY_CLIENT_SECRET, null)?.takeIf { it.isNotBlank() }
        return if (id != null && secret != null) IgdbCredentials(id, secret) else null
    }

    /** Trims and persists both credential fields, updating the in-memory state immediately. */
    fun save(clientId: String, clientSecret: String) {
        val trimmedId = clientId.trim()
        val trimmedSecret = clientSecret.trim()
        _credentials.value = IgdbCredentials(trimmedId, trimmedSecret)
        prefs.edit()
            .putString(KEY_CLIENT_ID, trimmedId)
            .putString(KEY_CLIENT_SECRET, trimmedSecret)
            .apply()
    }

    /** Removes both stored credentials, e.g. after a 401 invalidates them. */
    fun clear() {
        _credentials.value = null
        prefs.edit().remove(KEY_CLIENT_ID).remove(KEY_CLIENT_SECRET).apply()
    }

    companion object {
        private const val PREFS_NAME = "orion_igdb_settings_secure"
        private const val KEY_CLIENT_ID = "igdb_client_id"
        private const val KEY_CLIENT_SECRET = "igdb_client_secret"

        @Volatile
        private var INSTANCE: IgdbCredentialsStore? = null

        /** Returns the process-wide singleton, creating it on first access (double-checked locking). */
        fun getInstance(context: Context): IgdbCredentialsStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: IgdbCredentialsStore(context.applicationContext).also { INSTANCE = it }
            }
    }
}
