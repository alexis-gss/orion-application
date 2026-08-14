package com.orion.app.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Builds an [EncryptedSharedPreferences] instance backed by an Android-Keystore-generated
 * master key (AES256-GCM), used by every credential store in the app (TMDB key, Hardcover
 * key, IGDB client id/secret). Extracted here because all three stores used to
 * duplicate this exact construction.
 *
 * Values (and keys) are encrypted at rest; the master key itself is never readable outside
 * the Android Keystore, even on a rooted device without a Keystore exploit. This replaces
 * the previous plaintext storage (API keys readable directly from
 * /data/data/com.orion.app/shared_prefs/.xml).
 */
internal fun createEncryptedPrefs(context: Context, prefsName: String): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        prefsName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

/**
 * Generic "single secret string" encrypted store: a value can be saved, cleared, and
 * observed as a [StateFlow]. Used as the base for any domain that only needs one API key
 * (TMDB, Hardcover). Domains needing more than one field (e.g. IGDB's client id +
 * secret pair) do not fit this shape and keep their own store instead — see
 * [IgdbCredentialsStore].
 *
 * Subclasses are expected to expose their own `getInstance(context)` singleton accessor
 * (mirroring the previous per-domain stores) since callers reference stores by concrete
 * type throughout the app (DI is manual, via OrionApplication).
 */
abstract class SingleKeyStore protected constructor(
    context: Context,
    prefsName: String,
    private val prefsKey: String,
) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext, prefsName)

    private val _apiKey = MutableStateFlow(prefs.getString(prefsKey, null)?.takeIf { it.isNotBlank() })
    val apiKey: StateFlow<String?> = _apiKey

    /** Trims and persists the value, updating the in-memory state immediately. */
    fun save(key: String) {
        val trimmed = key.trim()
        _apiKey.value = trimmed.ifBlank { null }
        // apply() writes to memory immediately and persists in the background, unlike
        // commit() which blocks the calling thread until the disk write completes
        // (lint: ApplySharedPref).
        prefs.edit().putString(prefsKey, trimmed).apply()
    }

    /** Removes the stored value, e.g. after a 401 invalidates it. */
    fun clear() {
        _apiKey.value = null
        prefs.edit().remove(prefsKey).apply()
    }
}
