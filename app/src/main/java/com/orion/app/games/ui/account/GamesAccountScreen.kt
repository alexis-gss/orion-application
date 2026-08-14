package com.orion.app.games.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.games.data.GamesRepository
import com.orion.app.games.ui.components.GameCardData
import com.orion.app.games.ui.components.gameCarouselSection
import com.orion.app.games.ui.components.toCardData
import java.util.Calendar

/** Games Account page: 3 quick stat cards (clickable through to the dedicated page),
 * favorites, and completed games. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesAccountScreen(
    repository: GamesRepository,
    onOpenItem: (Int) -> Unit,
    onOpenStats: () -> Unit,
    onSeeAll: (String, List<GameCardData>) -> Unit,
    onOpenMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val favorites by repository.observeFavorites().collectAsState(initial = emptyList())
    // showAddedDateBadge/showStatusDateBadge: the date badge (added to favorites /
    // marked completed) only makes sense in this "account" context — see also
    // SeeAllScreen, which receives these same cards already carrying the badge via onSeeAll.
    val favoritesCards = remember(favorites) { favorites.map { it.toCardData(context, showAddedDateBadge = true) } }

    val played by repository.observePlayed().collectAsState(initial = emptyList())
    val completedCards = remember(played) {
        played.filter { it.status == "completed" }.map { it.toCardData(context, showStatusDateBadge = true) }
    }

    val favoritesTitle = stringResource(R.string.favorites_title)
    val favoritesEmpty = stringResource(R.string.games_favorites_empty)
    val completedTitle = stringResource(R.string.games_completed_title)
    val completedEmpty = stringResource(R.string.games_completed_empty)

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_account),
                onOpenMenu = onOpenMenu
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 95.dp)
        ) {
            item { GamesStatsSection(played, favoritesCards.size, onOpenStats = onOpenStats) }
            gameCarouselSection(
                title = favoritesTitle,
                items = favoritesCards,
                emptyLabel = favoritesEmpty,
                onItemClick = { onOpenItem(it.igdbId) },
                onSeeAllClick = { onSeeAll(favoritesTitle, favoritesCards) },
                sectionHorizontalPadding = 16.dp,
            )
            item { Spacer(Modifier.height(8.dp)) }
            gameCarouselSection(
                title = completedTitle,
                items = completedCards,
                emptyLabel = completedEmpty,
                onItemClick = { onOpenItem(it.igdbId) },
                onSeeAllClick = { onSeeAll(completedTitle, completedCards) },
                sectionHorizontalPadding = 16.dp,
            )
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ---------------- 3 quick stat cards (same principle as the cinema side) ----------------

private data class GameStatItem(val value: Int, val label: String, val accent: Boolean = false)

@Composable
private fun GamesStatsSection(played: List<com.orion.app.games.data.PlayedGame>, favoritesCount: Int, onOpenStats: () -> Unit) {
    val completedCount = remember(played) { played.count { it.status == "completed" } }
    val thisMonth = remember(played) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        played.count {
            it.status == "completed" &&
                run {
                    cal.timeInMillis = it.playedAt
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
        }
    }

    val stats = listOf(
        GameStatItem(completedCount, stringResource(R.string.games_completed_title)),
        GameStatItem(favoritesCount, stringResource(R.string.favorites_title)),
        GameStatItem(thisMonth, stringResource(R.string.stats_this_month), accent = true)
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle(stringResource(R.string.stats_title))
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenStats),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            stats.forEach { stat -> GamesStatCard(stat, modifier = Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GamesStatCard(stat: GameStatItem, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (stat.accent) {
                    Modifier.background(Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)))
                } else {
                    Modifier.background(extended.cardSurface)
                }
            )
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stat.value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (stat.accent) extended.badgeText else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stat.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (stat.accent) extended.badgeText.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
