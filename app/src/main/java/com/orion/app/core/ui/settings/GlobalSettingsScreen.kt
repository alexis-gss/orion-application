package com.orion.app.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.books.data.BooksRepository
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.core.ui.AppUniverse
import com.orion.app.core.data.ApiKeyStore
import com.orion.app.core.data.BooksApiKeyStore
import com.orion.app.core.data.IgdbCredentialsStore
import com.orion.app.core.data.NotificationPreferenceStore
import com.orion.app.core.data.ThemePreferenceStore
import com.orion.app.games.data.GamesRepository

/**
 * Single global settings screen, reachable from the home screen and the sidebar of all three
 * domains: appearance (theme), API keys/credentials (TMDB + IGDB + Hardcover), local data
 * management for each domain, and the about section. Replaces the former SettingsScreen
 * (cinema) and GamesSettingsScreen, now removed.
 *
 * Each tab's content lives in its own file (GlobalOptionsTab.kt, CinemaSettingsTab.kt,
 * GamesSettingsTab.kt, BooksSettingsTab.kt) as a LazyListScope extension, to keep this
 * main file short and the tab-selection logic readable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    initialUniverse: AppUniverse = AppUniverse.CINEMA,
    themeStore: ThemePreferenceStore,
    repository: CinemaRepository,
    apiKeyStore: ApiKeyStore,
    gamesRepository: GamesRepository,
    igdbCredentialsStore: IgdbCredentialsStore,
    booksRepository: BooksRepository,
    booksApiKeyStore: BooksApiKeyStore,
    notificationPreferenceStore: NotificationPreferenceStore,
    onBack: () -> Unit
) {
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { snackbarHostState.showSnackbar(it); snackbarMessage = null }
    }

    // Pills rather than a classic TabRow (the underline indicator would be too subtle for
    // only 4 short tabs) — same visual language as the filter chips used elsewhere in the app,
    // with one icon per domain so the active tab is recognizable at a glance.
    val settingsTabs = listOf(
        stringResource(R.string.domain_cinema),
        stringResource(R.string.domain_games),
        stringResource(R.string.domain_books),
    )
    var selectedTabIndex by remember {
        mutableIntStateOf(
            when (initialUniverse) {
                AppUniverse.CINEMA -> 0
                AppUniverse.GAMES -> 1
                AppUniverse.BOOKS -> 2
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GlobalOptions(
                themeStore = themeStore,
                notificationPreferenceStore = notificationPreferenceStore,
            )
            Spacer(Modifier.height(16.dp))
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                settingsTabs.forEachIndexed { index, tabLabel ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = tabLabel) }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                when (selectedTabIndex) {
                    0 -> cinemaSettingsTab(
                        repository = repository,
                        apiKeyStore = apiKeyStore,
                        onMessage = { snackbarMessage = it },
                    )
                    1 -> gamesSettingsTab(
                        repository = gamesRepository,
                        credentialsStore = igdbCredentialsStore,
                        onMessage = { snackbarMessage = it },
                    )
                    2 -> booksSettingsTab(
                        repository = booksRepository,
                        apiKeyStore = booksApiKeyStore,
                        onMessage = { snackbarMessage = it },
                    )
                }
            }
        }
    }
}