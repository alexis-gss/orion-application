package com.orion.app.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.notificationDataStore by preferencesDataStore(name = "notification_prefs")

/** Notification time, defaults to 9:00 AM. */
data class NotificationTime(val hour: Int, val minute: Int) {
    /** "HH:mm" label for display. */
    val label: String get() = String.format("%02d:%02d", hour, minute)
}

/**
 * Persists release-notification preferences (enabled/disabled + send time), same
 * principle as ThemePreferenceStore: process-wide scope, readable both by the UI
 * (Settings) and by the background worker that schedules the send.
 */
class NotificationPreferenceStore(private val context: Context, private val scope: CoroutineScope) {

    private val KEY_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val KEY_HOUR = intPreferencesKey("notifications_hour")
    private val KEY_MINUTE = intPreferencesKey("notifications_minute")

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _time = MutableStateFlow(NotificationTime(9, 0))
    val time: StateFlow<NotificationTime> = _time

    /** Emits on every change (enabling or time), so the caller can reschedule the worker
     *  without having to observe two separate Flows. */
    private val _onChanged = MutableStateFlow(0L)
    val onChanged: StateFlow<Long> = _onChanged

    init {
        scope.launch {
            val stored = context.notificationDataStore.data.first()
            _isEnabled.value = stored[KEY_ENABLED] ?: false
            _time.value = NotificationTime(
                hour = stored[KEY_HOUR] ?: 9,
                minute = stored[KEY_MINUTE] ?: 0
            )
        }
    }

    /** Toggles the feature and notifies observers via [onChanged] so the worker gets (re)scheduled. */
    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        scope.launch {
            context.notificationDataStore.edit { prefs -> prefs[KEY_ENABLED] = enabled }
            _onChanged.value = System.currentTimeMillis()
        }
    }

    /** Updates the daily send time and notifies observers via [onChanged] so the worker gets rescheduled. */
    fun setTime(hour: Int, minute: Int) {
        _time.value = NotificationTime(hour, minute)
        scope.launch {
            context.notificationDataStore.edit { prefs ->
                prefs[KEY_HOUR] = hour
                prefs[KEY_MINUTE] = minute
            }
            _onChanged.value = System.currentTimeMillis()
        }
    }
}
