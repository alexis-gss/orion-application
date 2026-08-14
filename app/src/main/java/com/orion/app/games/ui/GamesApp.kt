package com.orion.app.games.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.orion.app.R
import com.orion.app.core.data.IgdbCredentialsStore
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.games.data.GamesRepository
import com.orion.app.games.ui.account.GamesAccountScreen
import com.orion.app.games.ui.all.GamesSeeAllScreen
import com.orion.app.games.ui.components.GameCardData
import com.orion.app.games.ui.detail.GameDetailScreen
import com.orion.app.games.ui.gate.IgdbGateScreen
import com.orion.app.games.ui.library.GamesLibraryScreen
import com.orion.app.games.ui.planning.GamesPlanningScreen
import com.orion.app.games.ui.search.GamesSearchScreen
import com.orion.app.games.ui.stats.GamesStatsScreen

private sealed class GameTab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    data object Planning : GameTab("games_planning", R.string.nav_planning, Icons.Filled.CalendarMonth)
    data object Library : GameTab("games_library", R.string.nav_bookmark, Icons.Filled.Bookmark)
    data object Search : GameTab("games_search", R.string.nav_search, Icons.Filled.Search)
    data object Account : GameTab("games_account", R.string.nav_account, Icons.Filled.AccountCircle)
}

private val gameTabs = listOf(GameTab.Planning, GameTab.Library, GameTab.Search, GameTab.Account)
private const val FADE_MS = 300
private const val DETAIL_ROUTE = "games_detail/{igdbId}"
private const val SETTINGS_ROUTE = "games_settings"
private const val SEE_ALL_ROUTE = "games_all"
private const val STATS_ROUTE = "games_stats"
private fun detailRoute(igdbId: Int) = "games_detail/$igdbId"

/**
 * Equivalent of CinemaApp for the video games domain: as long as no valid IGDB
 * (Twitch) credentials are stored, only IgdbCredentialsGateScreen is shown.
 */
@Composable
fun GamesApp(
    repository: GamesRepository,
    credentialsStore: IgdbCredentialsStore,
    onOpenMenu: () -> Unit = {}
) {
    val credentials by credentialsStore.credentials.collectAsState()
    var verifiedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(credentials) {
        if (credentials != null) verifiedOnce = true
    }

    if (credentials == null) {
        IgdbGateScreen(repository = repository, credentialsStore = credentialsStore)
    } else {
        GamesMainScaffold(repository = repository, credentialsStore = credentialsStore, onOpenMenu = onOpenMenu)
    }
}

private data class SeeAllPayload(val title: String, val items: List<GameCardData>)

@Composable
private fun GamesMainScaffold(repository: GamesRepository, credentialsStore: IgdbCredentialsStore, onOpenMenu: () -> Unit = {}) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val hideBottomBar = currentDestination?.route == DETAIL_ROUTE ||
            currentDestination?.route == SETTINGS_ROUTE ||
            currentDestination?.route == SEE_ALL_ROUTE ||
            currentDestination?.route == STATS_ROUTE
    var seeAllPayload by remember { mutableStateOf<SeeAllPayload?>(null) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = navController,
            startDestination = GameTab.Planning.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(FADE_MS)) },
            exitTransition = { fadeOut(animationSpec = tween(FADE_MS)) },
            popEnterTransition = { fadeIn(animationSpec = tween(FADE_MS)) },
            popExitTransition = { fadeOut(animationSpec = tween(FADE_MS)) }
        ) {
            composable(GameTab.Planning.route) {
                GamesPlanningScreen(repository = repository, onOpenItem = { id -> navController.navigate(detailRoute(id)) }, onOpenMenu = onOpenMenu)
            }
            composable(GameTab.Library.route) {
                GamesLibraryScreen(
                    repository = repository,
                    onOpenItem = { id -> navController.navigate(detailRoute(id)) },
                    onOpenMenu = onOpenMenu
                )
            }
            composable(GameTab.Search.route) {
                GamesSearchScreen(repository = repository, onOpenItem = { id -> navController.navigate(detailRoute(id)) }, onOpenMenu = onOpenMenu)
            }
            composable(GameTab.Account.route) {
                GamesAccountScreen(
                    repository = repository,
                    onOpenItem = { id -> navController.navigate(detailRoute(id)) },
                    onOpenStats = { navController.navigate(STATS_ROUTE) },
                    onSeeAll = { title, items ->
                        seeAllPayload = SeeAllPayload(title, items)
                        navController.navigate(SEE_ALL_ROUTE)
                    },
                    onOpenMenu = onOpenMenu
                )
            }
            composable(STATS_ROUTE) {
                GamesStatsScreen(repository = repository, onBack = { navController.popBackStack() })
            }
            composable(DETAIL_ROUTE) { backStackEntry ->
                val igdbId = backStackEntry.arguments?.getString("igdbId")?.toIntOrNull() ?: 0
                GameDetailScreen(
                    repository = repository,
                    igdbId = igdbId,
                    onBack = { navController.popBackStack() },
                    onOpenItem = { newId -> navController.navigate(detailRoute(newId)) }
                )
            }
            composable(SEE_ALL_ROUTE) {
                seeAllPayload?.let { payload ->
                    GamesSeeAllScreen(
                        title = payload.title,
                        allItems = payload.items,
                        onOpenItem = { navController.navigate(detailRoute(it.igdbId)) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !hideBottomBar,
            enter = fadeIn(animationSpec = tween(FADE_MS)),
            exit = fadeOut(animationSpec = tween(FADE_MS)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            GamesFloatingNavBar(
                tabs = gameTabs,
                currentDestination = currentDestination,
                onTabSelected = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
private fun GamesFloatingNavBar(
    tabs: List<GameTab>,
    currentDestination: androidx.navigation.NavDestination?,
    onTabSelected: (GameTab) -> Unit,
) {
    val extended = OrionColors.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(33.dp))
                .background(extended.navBarContainer)
                .border(1.dp, extended.navBarBorder, RoundedCornerShape(33.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                GamesPillNavItem(tab = tab, selected = selected, onClick = { onTabSelected(tab) })
            }
        }
    }
}

@Composable
private fun RowScope.GamesPillNavItem(tab: GameTab, selected: Boolean, onClick: () -> Unit) {
    val extended = OrionColors.colors
    val shape = RoundedCornerShape(50)
    val weight by animateFloatAsState(
        targetValue = if (selected) 1.7f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navItemWeight"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) extended.navBarSelectedContainer else Color.Transparent,
        label = "navItemColor"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navIconScale"
    )
    Row(
        modifier = Modifier
            .weight(weight)
            .height(50.dp)
            .clip(shape)
            .background(if (selected) extended.navBarSelectedContainer else androidx.compose.ui.graphics.Color.Transparent)
            .then(
                if (selected) Modifier.background(
                    Brush.horizontalGradient(
                        listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)
                    )
                ) else Modifier.background(containerColor)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = stringResource(tab.label),
            tint = if (selected) extended.navBarSelectedIcon else extended.navBarIcon,
            modifier = Modifier.size(21.dp).scale(iconScale)
        )
        if (selected) {
            androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
            Text(stringResource(tab.label), color = extended.navBarSelectedIcon, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}
