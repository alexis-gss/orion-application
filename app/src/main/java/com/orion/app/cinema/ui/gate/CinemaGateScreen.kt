package com.orion.app.cinema.ui.gate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.data.ApiKeyStore
import com.orion.app.cinema.data.CinemaRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@Composable
fun CinemaGateScreen(repository: CinemaRepository, apiKeyStore: ApiKeyStore) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun validate() {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            errorMessage = context.getString(R.string.gate_error_empty)
            return
        }
        errorMessage = null
        isChecking = true
        scope.launch {
            try {
                // 1. Test the key BEFORE saving it
                repository.testApiKey(trimmed)

                // 2. If the test passes (no exception), persist the key for good
                apiKeyStore.save(trimmed)

            } catch (e: HttpException) {
                errorMessage = if (e.code() == 401) context.getString(R.string.gate_error_invalid)
                else context.getString(R.string.gate_error_tmdb, e.code())
            } catch (e: Exception) {
                errorMessage = context.getString(R.string.gate_error_unverified)
            } finally {
                isChecking = false
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = stringResource(R.string.gate_logo_description, stringResource(R.string.app_name)),
                modifier = Modifier
                    .size(96.dp)
                    .padding(8.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.gate_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it; errorMessage = null },
                placeholder = { Text(stringResource(R.string.gate_key_label)) },
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
            Button(
                onClick = { validate() },
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
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
