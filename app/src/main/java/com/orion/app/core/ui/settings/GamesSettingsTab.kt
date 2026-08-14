package com.orion.app.core.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.data.IgdbCredentialsStore
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.games.data.GamesExportImportManager
import com.orion.app.games.data.GamesRepository
import kotlinx.coroutines.launch

/** Content of the "Video games" tab: IGDB credentials and local data management. */
fun LazyListScope.gamesSettingsTab(
    repository: GamesRepository,
    credentialsStore: IgdbCredentialsStore,
    onMessage: (String) -> Unit,
) {
    item {
        SectionTitle(stringResource(R.string.settings_igdb_section_title))
        IgdbCredentialsSection(repository = repository, credentialsStore = credentialsStore)

        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.settings_games_data_section_title))
        GamesDataSection(repository = repository, onMessage = onMessage)
    }
}

@Composable
private fun IgdbCredentialsSection(repository: GamesRepository, credentialsStore: IgdbCredentialsStore) {
    val credentials by credentialsStore.credentials.collectAsState()
    var clientId by remember(credentials) { mutableStateOf(credentials?.clientId.orEmpty()) }
    var clientSecret by remember(credentials) { mutableStateOf(credentials?.clientSecret.orEmpty()) }
    var isChecking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val tokenStore = remember { com.orion.app.games.data.IgdbTokenStore.getInstance(context) }

    val credentialsEmptyMsg = stringResource(R.string.game_gate_error_empty)
    val credentialsSavedMsg = stringResource(R.string.settings_credentials_saved)

    val igdbUrl = "https://dev.twitch.tv/console/apps"
    val hintText = stringResource(R.string.settings_hint, igdbUrl)
    val linkStart = hintText.indexOf(igdbUrl)
    val annotatedText = buildAnnotatedString {
        append(hintText)
        if (linkStart >= 0) {
            addStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline), linkStart, linkStart + igdbUrl.length)
            addStringAnnotation(tag = "URL", annotation = igdbUrl, start = linkStart, end = linkStart + igdbUrl.length)
        }
    }

    fun saveCredentials() {
        val trimmedClientId = clientId.trim()
        val trimmedClientSecret = clientSecret.trim()
        if (trimmedClientId.isBlank() || trimmedClientSecret.isBlank()) { messageIsError = true; message = credentialsEmptyMsg; return }
        isChecking = true
        message = null
        scope.launch {
            try {
                repository.testConnection(trimmedClientId.trim(), trimmedClientSecret.trim(), tokenStore)
                credentialsStore.save(trimmedClientId, trimmedClientSecret)
                messageIsError = false
                message = credentialsSavedMsg
            } catch (e: Exception) {
                messageIsError = true
                message = context.getString(R.string.game_gate_error_connection, e.message ?: "")
            } finally {
                isChecking = false
            }
        }
    }

    Column {
        ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            onClick = { offset ->
                annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
            }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it; message = null },
            placeholder = { Text(stringResource(R.string.game_gate_client_id_label)) },
            singleLine = true,
            enabled = !isChecking,
            isError = messageIsError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = clientSecret,
            onValueChange = { clientSecret = it; message = null },
            placeholder = { Text(stringResource(R.string.game_gate_client_secret_label)) },
            singleLine = true,
            enabled = !isChecking,
            isError = messageIsError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = if (messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { saveCredentials() }, enabled = !isChecking, modifier = Modifier.fillMaxWidth()) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_key_verification))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_key_save))
                }
            }
        }
    }
}

@Composable
private fun GamesDataSection(repository: GamesRepository, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gzip")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bundle = repository.exportSnapshot()
                GamesExportImportManager.writeToUri(context, uri, bundle)
                onMessage(context.getString(R.string.settings_games_export_success, bundle.played.size, bundle.followed.size, bundle.favorites.size))
            } catch (e: Exception) { onMessage(context.getString(R.string.settings_data_export_failed, e.message ?: "")) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bundle = GamesExportImportManager.readFromUri(context, uri)
                repository.importSnapshot(bundle, replaceExisting = false)
                onMessage(context.getString(R.string.settings_games_import_success, bundle.played.size, bundle.followed.size, bundle.favorites.size))
            } catch (e: Exception) { onMessage(context.getString(R.string.settings_data_import_failed, e.message ?: "")) }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { exportLauncher.launch(GamesExportImportManager.defaultFileName()) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.FileDownload, contentDescription = null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.settings_data_export))
        }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/gzip", "application/octet-stream", "*/*")) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.FileUpload, contentDescription = null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.settings_data_import))
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { showDeleteAllDialog = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(Icons.Filled.DeleteForever, contentDescription = null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.settings_delete_all_games_button))
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.settings_delete_all_games_button)) },
            text = { Text(stringResource(R.string.settings_delete_all_games_text)) },
            confirmButton = {
                TextButton(onClick = { scope.launch { repository.clearAllData(); showDeleteAllDialog = false; onMessage(context.getString(R.string.settings_games_data_cleared)) } }) {
                    Text(stringResource(R.string.delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
