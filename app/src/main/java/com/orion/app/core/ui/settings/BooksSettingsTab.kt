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
import com.orion.app.books.data.BooksExportImportManager
import com.orion.app.books.data.BooksRepository
import com.orion.app.core.data.BooksApiKeyStore
import com.orion.app.core.ui.components.SectionTitle
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Content of the "Books" tab: Hardcover token and local data management. */
fun LazyListScope.booksSettingsTab(
    repository: BooksRepository,
    apiKeyStore: BooksApiKeyStore,
    onMessage: (String) -> Unit,
) {
    item {
        SectionTitle(stringResource(R.string.settings_books_section_title))
        HardcoverKeySection(repository = repository, apiKeyStore = apiKeyStore)

        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.settings_books_data_section_title))
        BooksDataSection(repository = repository, onMessage = onMessage)
    }
}

/**
 * Equivalent of TmdbKeySection for Hardcover: a single personal token, like TMDB, validated
 * via repository.testApiKey() (BooksRepository). Same rollback logic on failure.
 */
@Composable
private fun HardcoverKeySection(repository: BooksRepository, apiKeyStore: BooksApiKeyStore) {
    val currentKey by apiKeyStore.apiKey.collectAsState()
    var input by remember(currentKey) { mutableStateOf(currentKey.orEmpty()) }
    var isChecking by remember { mutableStateOf(false) }
    var keyMessage by remember { mutableStateOf<String?>(null) }
    var keyMessageIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val consoleUrl = "https://hardcover.app/account/api"
    val hintText = stringResource(R.string.settings_hint, consoleUrl)
    val linkStart = hintText.indexOf(consoleUrl)
    val annotatedText = buildAnnotatedString {
        append(hintText)
        if (linkStart >= 0) {
            addStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline), linkStart, linkStart + consoleUrl.length)
            addStringAnnotation(tag = "URL", annotation = consoleUrl, start = linkStart, end = linkStart + consoleUrl.length)
        }
    }

    val notEmptyMsg = stringResource(R.string.settings_key_not_empty)
    val alreadySavedMsg = stringResource(R.string.settings_key_already_saved)
    val savedMsg = stringResource(R.string.settings_key_saved)
    val invalidMsg = stringResource(R.string.settings_key_invalid)
    val unverifiedMsg = stringResource(R.string.settings_key_unverified)

    fun saveKey() {
        val trimmed = input.trim()
        if (trimmed.isBlank()) { keyMessageIsError = true; keyMessage = notEmptyMsg; return }
        if (trimmed == currentKey) { keyMessageIsError = false; keyMessage = alreadySavedMsg; return }
        isChecking = true
        keyMessage = null
        scope.launch {
            val previous = currentKey
            apiKeyStore.save(trimmed)
            try {
                repository.testApiKey()
                keyMessageIsError = false
                keyMessage = savedMsg
            } catch (e: HttpException) {
                previous?.let { apiKeyStore.save(it) } ?: apiKeyStore.clear()
                keyMessageIsError = true
                keyMessage = invalidMsg
            } catch (e: Exception) {
                previous?.let { apiKeyStore.save(it) } ?: apiKeyStore.clear()
                keyMessageIsError = true
                keyMessage = unverifiedMsg
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
            value = input,
            onValueChange = { input = it; keyMessage = null },
            placeholder = { Text(stringResource(R.string.book_gate_key_label)) },
            singleLine = true,
            enabled = !isChecking,
            isError = keyMessageIsError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        keyMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = if (keyMessageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { saveKey() }, enabled = !isChecking, modifier = Modifier.fillMaxWidth()) {
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

/** Equivalent of GamesDataSection for books (GZIP export/import + wipe). */
@Composable
private fun BooksDataSection(repository: BooksRepository, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gzip")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bundle = repository.exportSnapshot()
                BooksExportImportManager.writeToUri(context, uri, bundle)
                onMessage(context.getString(R.string.settings_books_export_success, bundle.read.size, bundle.followed.size, bundle.favorites.size))
            } catch (e: Exception) { onMessage(context.getString(R.string.settings_data_export_failed, e.message ?: "")) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bundle = BooksExportImportManager.readFromUri(context, uri)
                repository.importSnapshot(bundle, replaceExisting = false)
                onMessage(context.getString(R.string.settings_books_import_success, bundle.read.size, bundle.followed.size, bundle.favorites.size))
            } catch (e: Exception) { onMessage(context.getString(R.string.settings_data_import_failed, e.message ?: "")) }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { exportLauncher.launch(BooksExportImportManager.defaultFileName()) }, modifier = Modifier.weight(1f)) {
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
        Icon(Icons.Filled.DeleteForever, contentDescription = null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.settings_delete_all_books_button))
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.settings_delete_all_books_button)) },
            text = { Text(stringResource(R.string.settings_delete_all_books_text)) },
            confirmButton = {
                TextButton(onClick = { scope.launch { repository.clearAllData(); showDeleteAllDialog = false; onMessage(context.getString(R.string.settings_books_data_cleared)) } }) {
                    Text(stringResource(R.string.delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
