package com.orion.app.core.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.orion.app.OrionApplication
import com.orion.app.cinema.ui.CinemaApp
import com.orion.app.core.data.ThemePreferenceStore
import com.orion.app.core.ui.nav.AppSidebarContent
import com.orion.app.core.ui.settings.GlobalSettingsScreen
import com.orion.app.core.ui.theme.ThemeUniverse
import com.orion.app.games.ui.GamesApp
import com.orion.app.books.ui.BooksApp
import kotlinx.coroutines.launch

private const val FADE_MS = 300
private const val ROUTE_HOME = "home"
private const val ROUTE_CINEMA = "cinema"
private const val ROUTE_GAMES = "games"
private const val ROUTE_BOOKS = "books"
private const val ROUTE_OPTIONS = "options"

/**
 * Navigation root: the home screen (with the app logo) lets the user pick a domain.
 * Once inside a domain, the header's back button is replaced by a hamburger menu that
 * opens this shared sidebar (a ModalNavigationDrawer layered on top of the NavHost) to
 * switch domains directly or open the settings, without going back through the home screen.
 */
@Composable
fun RootApp(
    app: OrionApplication,
    themeStore: ThemePreferenceStore,
    themeUniverseState: MutableState<ThemeUniverse>
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Domaine réellement en cours de consultation (cinéma/jeux/livres). Contrairement à
    // `currentRoute`, qui vaut aussi ROUTE_OPTIONS ou ROUTE_HOME (routes sans domaine
    // propre), cette valeur n'est mise à jour QUE sur une route de domaine — elle
    // "survit" donc à la navigation vers l'écran Options, pour que le thème et
    // l'onglet présélectionné dans les settings restent cohérents avec le domaine dont
    // on vient, plutôt que de retomber systématiquement sur Cinéma.
    var lastUniverse by remember { mutableStateOf(AppUniverse.CINEMA) }
    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            ROUTE_CINEMA -> lastUniverse = AppUniverse.CINEMA
            ROUTE_GAMES -> lastUniverse = AppUniverse.GAMES
            ROUTE_BOOKS -> lastUniverse = AppUniverse.BOOKS
        }
    }

    themeUniverseState.value = when (lastUniverse) {
        AppUniverse.GAMES -> ThemeUniverse.GAMES
        AppUniverse.BOOKS -> ThemeUniverse.BOOKS
        AppUniverse.CINEMA -> ThemeUniverse.CINEMA
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun routeFor(universe: AppUniverse): String = when (universe) {
        AppUniverse.CINEMA -> ROUTE_CINEMA
        AppUniverse.GAMES -> ROUTE_GAMES
        AppUniverse.BOOKS -> ROUTE_BOOKS
    }

    fun currentUniverse(): AppUniverse = lastUniverse

    fun navigateToUniverse(universe: AppUniverse) {
        val route = routeFor(universe)
        scope.launch { drawerState.close() }
        if (currentRoute != route) {
            navController.navigate(route) { popUpTo(ROUTE_HOME) }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == ROUTE_CINEMA || currentRoute == ROUTE_GAMES || currentRoute == ROUTE_BOOKS,
        drawerContent = {
            AppSidebarContent(
                currentUniverse = currentUniverse(),
                onSelectUniverse = { navigateToUniverse(it) },
                onOpenOptions = {
                    scope.launch { drawerState.close() }
                    navController.navigate(ROUTE_OPTIONS)
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(FADE_MS)) },
            exitTransition = { fadeOut(animationSpec = tween(FADE_MS)) },
            popEnterTransition = { fadeIn(animationSpec = tween(FADE_MS)) },
            popExitTransition = { fadeOut(animationSpec = tween(FADE_MS)) }
        ) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    onSelectUniverse = { universe -> navController.navigate(routeFor(universe)) },
                    onOpenOptions = { navController.navigate(ROUTE_OPTIONS) }
                )
            }
            composable(ROUTE_OPTIONS) {
                GlobalSettingsScreen(
                    initialUniverse = lastUniverse,
                    themeStore = themeStore,
                    repository = app.repository,
                    apiKeyStore = app.apiKeyStore,
                    gamesRepository = app.gamesRepository,
                    igdbCredentialsStore = app.igdbCredentialsStore,
                    booksRepository = app.booksRepository,
                    booksApiKeyStore = app.booksApiKeyStore,
                    notificationPreferenceStore = app.notificationPreferenceStore,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(ROUTE_CINEMA) {
                CinemaApp(
                    repository = app.repository,
                    apiKeyStore = app.apiKeyStore,
                    themeStore = themeStore,
                    onOpenMenu = { scope.launch { drawerState.open() } }
                )
            }
            composable(ROUTE_GAMES) {
                GamesApp(
                    repository = app.gamesRepository,
                    credentialsStore = app.igdbCredentialsStore,
                    onOpenMenu = { scope.launch { drawerState.open() } }
                )
            }
            composable(ROUTE_BOOKS) {
                BooksApp(
                    repository = app.booksRepository,
                    apiKeyStore = app.booksApiKeyStore,
                    onOpenMenu = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}