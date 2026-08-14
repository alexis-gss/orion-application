package com.orion.app.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/**
 * Persists the user's theme choice (light/dark) to disk via DataStore, so it's restored
 * on every app launch. Defaults to the light theme (isDarkTheme = false).
 */
class ThemePreferenceStore(private val context: Context, private val scope: CoroutineScope) {

    private val KEY_DARK_THEME = booleanPreferencesKey("is_dark_theme")

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    init {
        scope.launch {
            val stored = context.themeDataStore.data.first()
            _isDarkTheme.value = stored[KEY_DARK_THEME] ?: false
        }
    }

    /** Updates the in-memory state immediately and persists the new value asynchronously. */
    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        scope.launch {
            context.themeDataStore.edit { prefs ->
                prefs[KEY_DARK_THEME] = enabled
            }
        }
    }
}