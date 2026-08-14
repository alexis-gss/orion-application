package com.orion.app.core.data

import android.content.Context

/** TMDB API key store. Thin wrapper over [SingleKeyStore] providing the TMDB-specific prefs file/key names and the singleton accessor callers use throughout the app. */
class ApiKeyStore private constructor(context: Context) :
    SingleKeyStore(context, PREFS_NAME, KEY_API_KEY) {

    companion object {
        private const val PREFS_NAME = "orion_settings_secure"
        private const val KEY_API_KEY = "tmdb_api_key"

        @Volatile
        private var INSTANCE: ApiKeyStore? = null

        fun getInstance(context: Context): ApiKeyStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiKeyStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
