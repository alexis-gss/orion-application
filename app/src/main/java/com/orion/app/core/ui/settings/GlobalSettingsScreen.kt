package com.orion.app.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
    initialUniverse: AppUniverse? = null,
    themeStore: ThemePreferenceStore,
    repository: CinemaRepository,
    apiKeyStore: ApiKeyStore,
    gamesRepository: GamesRepository,
    igdbCredentialsStore: IgdbCredentialsStore,
    booksRepository: BooksRepository,
    booksApiKeyStore: BooksApiKeyStore,
    notificationPreferenceStore: NotificationPreferenceStore,
    onBack: () -> Unit,
) {
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { snackbarHostState.showSnackbar(it); snackbarMessage = null }
    }

    val settingsTabs = listOf(
        stringResource(R.string.settings_general_tab),
        stringResource(R.string.domain_cinema),
        stringResource(R.string.domain_games),
        stringResource(R.string.domain_books),
    )
    var selectedTabIndex by remember {
        mutableIntStateOf(
            when (initialUniverse) {
                null -> 0
                AppUniverse.CINEMA -> 1
                AppUniverse.GAMES -> 2
                AppUniverse.BOOKS -> 3
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.background(Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                settingsTabs.forEachIndexed { index, tabLabel ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                    ) {
                        Text(
                            text = tabLabel,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                when (selectedTabIndex) {
                    0 -> globalOptionsTab(
                        themeStore = themeStore,
                        notificationPreferenceStore = notificationPreferenceStore,
                    )
                    1 -> cinemaSettingsTab(
                        repository = repository,
                        apiKeyStore = apiKeyStore,
                        onMessage = { snackbarMessage = it },
                    )
                    2 -> gamesSettingsTab(
                        repository = gamesRepository,
                        credentialsStore = igdbCredentialsStore,
                        onMessage = { snackbarMessage = it },
                    )
                    3 -> booksSettingsTab(
                        repository = booksRepository,
                        apiKeyStore = booksApiKeyStore,
                        onMessage = { snackbarMessage = it },
                    )
                }
            }
        }
    }
}