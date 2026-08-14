package com.orion.app.core.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.orion.app.R
import com.orion.app.core.data.NotificationPreferenceStore
import com.orion.app.core.data.ThemePreferenceStore
import com.orion.app.core.ui.components.SectionTitle

/**
 * Content of the "General" settings tab: appearance (dark theme), daily release notifications,
 * and the about section. Everything is grouped into a single `item {}` (like the rest of the
 * page) rather than several `items()`, since this fixed, small number of blocks doesn't need
 * per-item recomposition/scroll optimizations.
 */
@Composable
fun GlobalOptions(
    themeStore: ThemePreferenceStore,
    notificationPreferenceStore: NotificationPreferenceStore,
) {
    val isDarkTheme by themeStore.isDarkTheme.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionTitle(stringResource(R.string.settings_theme_title))
        SettingsToggleRow(
            title = stringResource(R.string.settings_dark_theme_label),
            checked = isDarkTheme,
            onCheckedChange = { themeStore.setDarkTheme(it) },
        )

        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.settings_notifications_title))
        NotificationSection(store = notificationPreferenceStore)

        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.settings_about_title))
        AboutSection()
    }
}

/** "About" section: app version plus a credit link opening the author's website in the default browser. */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "v?.?.?" }
        catch (e: Exception) { "v?.?.?" }
    }
    val uriHandler = LocalUriHandler.current
    val aboutPrefix = stringResource(R.string.settings_about_prefix, versionName) + " "
    val annotatedText = buildAnnotatedString {
        append(aboutPrefix)
        pushStringAnnotation(tag = "URL", annotation = "https://alexis-gousseau.com")
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
            append("Alexis Gousseau")
        }
        pop()
    }
    ClickableText(
        text = annotatedText,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { uriHandler.openUri(it.item) }
        }
    )
}

/**
 * "Notifications" section: toggle the feature on/off, then, if enabled, pick the daily time at
 * which the day's releases summary (movies, episodes, video games, tracked books) is sent.
 * The actual scheduling of the worker is handled reactively by OrionApplication (it observes
 * NotificationPreferenceStore), so this section only reads/writes the preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSection(store: NotificationPreferenceStore) {
    val isEnabled by store.isEnabled.collectAsState()
    val time by store.time.collectAsState()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        // Whether the permission is granted or not, we respect the user's choice for the
        // setting itself: if denied, the notification simply won't be shown
        // (see ReleaseNotificationHelper), without blocking the rest of the app.
        store.setEnabled(true)
    }

    /** Toggles the notification preference, requesting the POST_NOTIFICATIONS runtime permission first on Android 13+ if not already granted. */
    fun onToggle(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            store.setEnabled(enabled)
        }
    }

    Column {
        SettingsToggleRow(
            title = stringResource(R.string.settings_notifications_summary),
            checked = isEnabled,
            onCheckedChange = { onToggle(it) },
        )

        if (isEnabled) {
            Spacer(Modifier.height(8.dp))
            SettingsClickableRow(
                icon = Icons.Filled.Schedule,
                title = stringResource(R.string.settings_notification_time_label),
                value = time.label,
                onClick = { showTimePicker = true },
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.settings_notification_time_label)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    store.setTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
