package com.orion.app.games.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.util.DateUtils
import com.orion.app.games.data.FollowedGame
import com.orion.app.games.data.GamesRepository
import com.orion.app.games.ui.components.GameItemRow
import com.orion.app.games.ui.components.toCardData
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Epoch seconds -> "yyyy-MM-dd" (Europe/Paris time zone) to reuse DateUtils as-is. */
private fun Long.toIsoDate(): String =
    Instant.ofEpochSecond(this).atZone(ZoneId.of("Europe/Paris")).toLocalDate().toString()

/** Equivalent of the cinema PlanningScreen: followed games whose release isn't yet
 * past, grouped by release date (like cinema) instead of a plain list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesPlanningScreen(repository: GamesRepository, onOpenItem: (Int) -> Unit, onOpenMenu: () -> Unit = {}) {
    val allFollowed by repository.observeFollowed().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val upcoming = remember(allFollowed) {
        allFollowed.filter { !it.isReleased }
            .sortedWith(compareBy({ it.releaseTimestamp == null }, { it.releaseTimestamp }))
    }

    LaunchedEffect(allFollowed.map { it.igdbId }) {
        allFollowed.forEach { item -> repository.refreshFollowedIfStale(item) }
    }

    fun refreshAllManually() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            try {
                allFollowed.forEach { item -> repository.refreshFollowedIfStale(item, force = true) }
            } finally {
                isRefreshing = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                onOpenMenu = onOpenMenu,
                title = stringResource(R.string.nav_planning),
                actions = {
                    IconButton(onClick = { refreshAllManually() }, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (upcoming.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.games_planning_empty),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                // Grouped by release date (same principle as PlanningScreen on the
                // cinema side): one header per distinct day, keeping the order already sorted.
                val groups: List<Pair<Long?, List<FollowedGame>>> = remember(upcoming) {
                    val result = mutableListOf<Pair<Long?, MutableList<FollowedGame>>>()
                    upcoming.forEach { game ->
                        val last = result.lastOrNull()
                        if (last != null && last.first == game.releaseTimestamp) {
                            last.second.add(game)
                        } else {
                            result.add(game.releaseTimestamp to mutableListOf(game))
                        }
                    }
                    result
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 95.dp),
                ) {
                    groups.forEach { (timestamp, groupItems) ->
                        item(key = "header_${timestamp ?: "unknown"}") {
                            SectionTitle(
                                if (timestamp == null) stringResource(R.string.date_unknown) else DateUtils.formatDay(context, timestamp.toIsoDate())
                            )
                        }
                        items(groupItems, key = { it.igdbId }) { game ->
                            Row(Modifier.padding(bottom = 8.dp)) {
                                GameItemRow(
                                    item = game.toCardData(context),
                                    onClick = { onOpenItem(game.igdbId) },
                                    trailingContent = {
                                        val isoDate = game.releaseTimestamp?.toIsoDate()
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            OrionColors.colors.navBarSelectedContainerAlt,
                                                            OrionColors.colors.navBarSelectedContainer
                                                        )
                                                    )
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = DateUtils.daysRemainingLabel(context, isoDate),
                                                color = OrionColors.colors.navBarSelectedIcon,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
