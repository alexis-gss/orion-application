package com.orion.app.books.ui

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
import com.orion.app.core.data.BooksApiKeyStore
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.books.data.BooksRepository
import com.orion.app.books.ui.account.BooksAccountScreen
import com.orion.app.books.ui.all.BooksSeeAllScreen
import com.orion.app.books.ui.components.BookCardData
import com.orion.app.books.ui.detail.BookDetailScreen
import com.orion.app.books.ui.gate.BooksGateScreen
import com.orion.app.books.ui.library.BooksLibraryScreen
import com.orion.app.books.ui.planning.BooksPlanningScreen
import com.orion.app.books.ui.search.BooksSearchScreen
import com.orion.app.books.ui.stats.BooksStatsScreen

private sealed class BookTab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    data object Planning : BookTab("books_planning", R.string.nav_planning, Icons.Filled.CalendarMonth)
    data object Library : BookTab("books_library", R.string.nav_bookmark, Icons.Filled.Bookmark)
    data object Search : BookTab("books_search", R.string.nav_search, Icons.Filled.Search)
    data object Account : BookTab("books_account", R.string.nav_account, Icons.Filled.AccountCircle)
}

private val bookTabs = listOf(BookTab.Planning, BookTab.Library, BookTab.Search, BookTab.Account)
private const val FADE_MS = 300
private const val DETAIL_ROUTE = "books_detail/{volumeId}"
private const val SEE_ALL_ROUTE = "books_all"
private const val STATS_ROUTE = "books_stats"
private fun detailRoute(volumeId: String) = "books_detail/$volumeId"

/**
 * Equivalent of GamesApp for the books domain: as long as no valid Hardcover token
 * is stored, only BooksApiKeyGateScreen is shown.
 */
@Composable
fun BooksApp(
    repository: BooksRepository,
    apiKeyStore: BooksApiKeyStore,
    onOpenMenu: () -> Unit = {}
) {
    val apiKey by apiKeyStore.apiKey.collectAsState()
    var verifiedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(apiKey) {
        if (apiKey != null) verifiedOnce = true
    }

    if (apiKey == null) {
        BooksGateScreen(repository = repository, apiKeyStore = apiKeyStore)
    } else {
        BooksMainScaffold(repository = repository, onOpenMenu = onOpenMenu)
    }
}

private data class SeeAllPayload(val title: String, val items: List<BookCardData>)

@Composable
private fun BooksMainScaffold(repository: BooksRepository, onOpenMenu: () -> Unit = {}) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val hideBottomBar = currentDestination?.route == DETAIL_ROUTE ||
            currentDestination?.route == SEE_ALL_ROUTE ||
            currentDestination?.route == STATS_ROUTE
    var seeAllPayload by remember { mutableStateOf<SeeAllPayload?>(null) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = navController,
            startDestination = BookTab.Planning.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(FADE_MS)) },
            exitTransition = { fadeOut(animationSpec = tween(FADE_MS)) },
            popEnterTransition = { fadeIn(animationSpec = tween(FADE_MS)) },
            popExitTransition = { fadeOut(animationSpec = tween(FADE_MS)) }
        ) {
            composable(BookTab.Planning.route) {
                BooksPlanningScreen(repository = repository, onOpenItem = { id -> navController.navigate(detailRoute(id)) }, onOpenMenu = onOpenMenu)
            }
            composable(BookTab.Library.route) {
                BooksLibraryScreen(
                    repository = repository,
                    onOpenItem = { id -> navController.navigate(detailRoute(id)) },
                    onOpenMenu = onOpenMenu
                )
            }
            composable(BookTab.Search.route) {
                BooksSearchScreen(repository = repository, onOpenItem = { id -> navController.navigate(detailRoute(id)) }, onOpenMenu = onOpenMenu)
            }
            composable(BookTab.Account.route) {
                BooksAccountScreen(
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
                BooksStatsScreen(repository = repository, onBack = { navController.popBackStack() })
            }
            composable(DETAIL_ROUTE) { backStackEntry ->
                val volumeId = backStackEntry.arguments?.getString("volumeId") ?: ""
                BookDetailScreen(
                    repository = repository,
                    volumeId = volumeId,
                    onBack = { navController.popBackStack() },
                    onOpenItem = { newId -> navController.navigate(detailRoute(newId)) },
                )
            }
            composable(SEE_ALL_ROUTE) {
                seeAllPayload?.let { payload ->
                    BooksSeeAllScreen(
                        title = payload.title,
                        allItems = payload.items,
                        onOpenItem = { navController.navigate(detailRoute(it.volumeId)) },
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
            BooksFloatingNavBar(
                tabs = bookTabs,
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
private fun BooksFloatingNavBar(
    tabs: List<BookTab>,
    currentDestination: androidx.navigation.NavDestination?,
    onTabSelected: (BookTab) -> Unit,
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
                BooksPillNavItem(tab = tab, selected = selected, onClick = { onTabSelected(tab) })
            }
        }
    }
}

@Composable
private fun RowScope.BooksPillNavItem(tab: BookTab, selected: Boolean, onClick: () -> Unit) {
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
            .background(if (selected) extended.navBarSelectedContainer else Color.Transparent)
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
