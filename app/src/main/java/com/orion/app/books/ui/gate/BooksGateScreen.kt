package com.orion.app.books.ui.gate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.data.BooksApiKeyStore
import com.orion.app.books.data.BooksRepository
import kotlinx.coroutines.launch

/**
 * Equivalent of ApiKeyGateScreen (cinema) for Hardcover, which only requires a single
 * token (unlike IGDB and its two Twitch credentials). Free personal token, generated at
 * hardcover.app/account/api ("New API Key" button).
 */
@Composable
fun BooksGateScreen(repository: BooksRepository, apiKeyStore: BooksApiKeyStore) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    /** Validates the token against the Hardcover API before persisting it, so an invalid token never makes this screen dismiss. */
    fun validate() {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            errorMessage = context.getString(R.string.book_gate_error_empty)
            return
        }
        errorMessage = null
        isChecking = true
        scope.launch {
            try {
                // Tested BEFORE persisting, same as IGDB: avoids letting an invalid token
                // through the gate anyway (credentials != null as soon as save() runs).
                repository.testApiKey(trimmed)
                apiKeyStore.save(trimmed)
            } catch (e: Exception) {
                errorMessage = context.getString(
                    R.string.book_gate_error_connection,
                    e.message ?: context.getString(R.string.book_gate_error_check_token)
                )
            } finally {
                isChecking = false
            }
        }
    }

    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.book_gate_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.book_gate_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it; errorMessage = null },
                placeholder = { Text(stringResource(R.string.book_gate_key_label)) },
                singleLine = true,
                isError = errorMessage != null,
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = { validate() }, enabled = !isChecking, modifier = Modifier.fillMaxWidth()) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_key_verification))
                } else {
                    Text(stringResource(R.string.gate_validate))
                }
            }
        }
    }
}
