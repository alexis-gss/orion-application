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
    return try {
        openEncryptedPrefs(context, prefsName)
    } catch (e: Exception) {
        // Typically after a restore/migration where the prefs files came back but the Keystore
        // master key did not (AEADBadTagException, InvalidProtocolBufferException...). Left
        // unhandled this crashes the app at every launch. The stored secrets are unrecoverable
        // anyway: wipe them and start clean, the user just has to re-enter their API keys.
        SECURE_PREFS_FILES.forEach { context.deleteSharedPreferences(it) }
        context.deleteSharedPreferences(ANDROIDX_KEYSET_PREFS)
        openEncryptedPrefs(context, prefsName)
    }
}

/** Every encrypted prefs file used by the app's credential stores. Keep in sync with the backup rules XML. */
private val SECURE_PREFS_FILES = listOf(
    "orion_settings_secure",
    "orion_books_settings_secure",
    "orion_igdb_settings_secure",
    "orion_igdb_token_secure",
)

/** Prefs file in which androidx.security-crypto keeps its (Keystore-wrapped) keysets. */
private const val ANDROIDX_KEYSET_PREFS = "__androidx_security_crypto_encrypted_prefs__"

private fun openEncryptedPrefs(context: Context, prefsName: String): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val prefs = EncryptedSharedPreferences.create(
        context,
        prefsName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    // Force decryption of every entry now so an undecryptable file fails here (and is recovered
    // above) rather than later, inside a StateFlow initializer.
    prefs.all
    return prefs
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
        prefs.edit().putString(prefsKey, trimmed).apply()
    }

    /** Removes the stored value, e.g. after a 401 invalidates it. */
    fun clear() {
        _apiKey.value = null
        prefs.edit().remove(prefsKey).apply()
    }
}
