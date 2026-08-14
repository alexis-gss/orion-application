package com.orion.app.core.data

import android.content.Context

/**
 * Hardcover API key store (personal token generated at hardcover.app/account/api).
 * Thin wrapper over [SingleKeyStore], kept in its own prefs file so this token is never
 * mixed up with the TMDB key.
 */
class BooksApiKeyStore private constructor(context: Context) :
    SingleKeyStore(context, PREFS_NAME, KEY_API_KEY) {

    companion object {
        private const val PREFS_NAME = "orion_books_settings_secure"
        private const val KEY_API_KEY = "hardcover_api_key"

        @Volatile
        private var INSTANCE: BooksApiKeyStore? = null

        /** Returns the process-wide singleton, creating it on first access (double-checked locking). */
        fun getInstance(context: Context): BooksApiKeyStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BooksApiKeyStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
