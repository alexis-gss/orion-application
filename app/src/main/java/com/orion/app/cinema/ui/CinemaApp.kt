package com.orion.app.cinema.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.orion.app.core.data.ApiKeyStore
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.core.data.ThemePreferenceStore
import com.orion.app.cinema.ui.account.CinemaAccountScreen
import com.orion.app.cinema.ui.detail.CinemaDetailScreen
import com.orion.app.cinema.ui.gate.CinemaGateScreen
import com.orion.app.cinema.ui.planning.CinemaPlanningScreen
import com.orion.app.cinema.ui.bookmark.CinemaBookmarkScreen
import com.orion.app.cinema.ui.search.CinemaSearchScreen
import com.orion.app.cinema.ui.stats.CinemaStatsScreen
import com.orion.app.cinema.ui.all.CinemaSeeAllScreen
import com.orion.app.cinema.ui.components.CinemaCardData
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.ui.theme.OrionExtendedColors
import com.orion.app.R

private sealed class Tab(val route: String, @StringRes val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Planning : Tab("cinema_planning", R.string.nav_planning, Icons.Filled.CalendarMonth)
    data object Bookmark : Tab("cinema_bookmark", R.string.nav_bookmark, Icons.Filled.Bookmark)
    data object Search : Tab("cinema_search", R.string.nav_search, Icons.Filled.Search)
    data object Account : Tab("cinema_account", R.string.nav_account, Icons.Filled.AccountCircle)
}

private val tabs = listOf(Tab.Planning, Tab.Bookmark, Tab.Search, Tab.Account)

// Page transition fade duration, reused as-is by the floating nav bar so its
// appearance/disappearance stays visually synchronized.
private const val PAGE_FADE_DURATION_MS = 300

private const val DETAIL_ROUTE = "cinema_detail/{mediaType}/{tmdbId}"
private const val SETTINGS_ROUTE = "cinema_settings"
private const val STATS_ROUTE = "cinema_stats"
private const val SEE_ALL_ROUTE = "cinema_all"
fun detailRoute(mediaType: String, tmdbId: Int) = "cinema_detail/$mediaType/$tmdbId"

/**
 * UI entry point. As long as no valid TMDB API key is stored in apiKeyStore, only
 * ApiKeyGateScreen is shown: no tabs, no navigation to the rest of the app is possible.
 * As soon as the key is validated, apiKeyStore.apiKey's state changes and this function
 * automatically recomposes to switch over to MainScaffold.
 */
@Composable
fun CinemaApp(
    repository: CinemaRepository,
    apiKeyStore: ApiKeyStore,
    themeStore: ThemePreferenceStore,
    onOpenMenu: () -> Unit = {}
) {
    val apiKey by apiKeyStore.apiKey.collectAsState()

    if (apiKey.isNullOrBlank()) {
        CinemaGateScreen(repository = repository, apiKeyStore = apiKeyStore)
    } else {
        MainScaffold(repository = repository, onOpenMenu = onOpenMenu)
    }
}

private data class SeeAllPayload(
    val title: String,
    val items: List<CinemaCardData>
)

@Composable
private fun MainScaffold(
    repository: CinemaRepository,
    onOpenMenu: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val hideBottomBar = currentDestination?.route == DETAIL_ROUTE ||
            currentDestination?.route == SETTINGS_ROUTE ||
            currentDestination?.route == STATS_ROUTE ||
            currentDestination?.route == SEE_ALL_ROUTE
    var seeAllPayload by remember { mutableStateOf<SeeAllPayload?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = Tab.Planning.route,
            modifier = Modifier.fillMaxSize(), // no more innerPadding: full screen
            enterTransition = { fadeIn(animationSpec = tween(PAGE_FADE_DURATION_MS)) },
            exitTransition = { fadeOut(animationSpec = tween(PAGE_FADE_DURATION_MS)) },
            popEnterTransition = { fadeIn(animationSpec = tween(PAGE_FADE_DURATION_MS)) },
            popExitTransition = { fadeOut(animationSpec = tween(PAGE_FADE_DURATION_MS)) }
        ) {
            composable(Tab.Planning.route) {
                CinemaPlanningScreen(repository = repository, onOpenItem = { mediaType, id ->
                    navController.navigate(detailRoute(mediaType, id))
                }, onOpenMenu = onOpenMenu)
            }
            composable(Tab.Bookmark.route) {
                CinemaBookmarkScreen(
                    repository = repository,
                    onOpenItem = { mediaType, id ->
                        navController.navigate(detailRoute(mediaType, id))
                    },
                    onSeeAll = { title, items ->
                        seeAllPayload = SeeAllPayload(title, items)
                        navController.navigate(SEE_ALL_ROUTE)
                    },
                    onOpenMenu = onOpenMenu
                )
            }
            composable(Tab.Search.route) {
                CinemaSearchScreen(repository = repository, onOpenItem = { mediaType, id ->
                    navController.navigate(detailRoute(mediaType, id))
                }, onOpenMenu = onOpenMenu)
            }
            composable(Tab.Account.route) {
                CinemaAccountScreen(
                    repository = repository,
                    onOpenItem = { mediaType, id -> navController.navigate(detailRoute(mediaType, id)) },
                    onOpenStats = { navController.navigate(STATS_ROUTE) },
                    onSeeAll = { title, items ->
                        seeAllPayload = SeeAllPayload(title, items)
                        navController.navigate(SEE_ALL_ROUTE)
                    },
                    onOpenMenu = onOpenMenu
                )
            }
            composable(DETAIL_ROUTE) { backStackEntry ->
                val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                val tmdbId = backStackEntry.arguments?.getString("tmdbId")?.toIntOrNull() ?: 0
                CinemaDetailScreen(
                    repository = repository,
                    mediaType = mediaType,
                    tmdbId = tmdbId,
                    onBack = { navController.popBackStack() },
                    onOpenItem = { newMediaType, newId ->
                        navController.navigate(detailRoute(newMediaType, newId))
                    }
                )
            }
            composable(STATS_ROUTE) {
                CinemaStatsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(SEE_ALL_ROUTE) {
                seeAllPayload?.let { payload ->
                    CinemaSeeAllScreen(
                        title = payload.title,
                        allItems = payload.items,
                        repository = repository,
                        onOpenItem = { navController.navigate(detailRoute(it.mediaType, it.tmdbId)) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = !hideBottomBar,
            enter = fadeIn(animationSpec = tween(PAGE_FADE_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(PAGE_FADE_DURATION_MS)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingGlassNavBar(
                tabs = tabs,
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

/**
 * TV Time-style "frosted glass" floating navigation bar: a semi-transparent dark
 * capsule detached from the edges, a bright top border to simulate a glass reflection,
 * the active tab shown as a gold pill that smoothly stretches/shrinks (spring) on tab
 * change, with the icon "popping" slightly on selection.
 */
@Composable
private fun FloatingGlassNavBar(
    tabs: List<Tab>,
    currentDestination: androidx.navigation.NavDestination?,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = OrionColors.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(33.dp), clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(33.dp))
                .background(extended.navBarContainer)
                .border(1.dp, extended.navBarBorder, RoundedCornerShape(33.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                PillNavItem(
                    tab = tab,
                    selected = selected,
                    extended = extended,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.PillNavItem(
    tab: Tab,
    selected: Boolean,
    extended: OrionExtendedColors,
    onClick: () -> Unit
) {
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
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = stringResource(tab.label),
            tint = if (selected) extended.navBarSelectedIcon else extended.navBarIcon,
            modifier = Modifier.size(20.dp).scale(iconScale)
        )
        if (selected) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = stringResource(tab.label),
                color = extended.navBarSelectedIcon,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}
