package com.orion.app.games.ui.gate

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
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.data.IgdbCredentialsStore
import com.orion.app.games.data.GamesRepository
import kotlinx.coroutines.launch

/**
 * Equivalent of ApiKeyGateScreen (cinema) for IGDB, which requires two Twitch credentials
 * (client_id + client_secret) rather than a single key. Obtainable for free at
 * https://dev.twitch.tv/console/apps after creating a Twitch application.
 */
@Composable
fun IgdbGateScreen(repository: GamesRepository, credentialsStore: IgdbCredentialsStore) {
    var clientId by remember { mutableStateOf("") }
    var clientSecret by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenStore = remember { com.orion.app.games.data.IgdbTokenStore.getInstance(context) }

    fun validate() {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            errorMessage = context.getString(R.string.game_gate_error_empty)
            return
        }
        errorMessage = null
        isChecking = true
        scope.launch {
            try {
                // Tested BEFORE persisting: credentialsStore.save() is only called on success.
                // Saving before the test would recompose GamesApp to the main screen as soon
                // as save() is called (credentials != null), so the gate screen — and its
                // errorMessage — would disappear before testConnection() could even fail.
                repository.testConnection(clientId.trim(), clientSecret.trim(), tokenStore)
                credentialsStore.save(clientId, clientSecret)
            } catch (e: Exception) {
                errorMessage = context.getString(R.string.game_gate_error_connection, e.message ?: "")
            } finally {
                isChecking = false
            }
        }
    }

    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.SportsEsports,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.game_gate_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.game_gate_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = clientId,
                onValueChange = { clientId = it; errorMessage = null },
                placeholder = { Text(stringResource(R.string.game_gate_client_id_label)) },
                singleLine = true,
                isError = errorMessage != null,
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = clientSecret,
                onValueChange = { clientSecret = it; errorMessage = null },
                placeholder = { Text(stringResource(R.string.game_gate_client_secret_label)) },
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
