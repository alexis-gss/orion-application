package com.orion.app.cinema.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orion.app.cinema.data.FollowedItem
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.R
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.cinema.ui.components.CinemaItemRow
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.cinema.ui.components.toPlanningCardData
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaPlanningScreen(repository: CinemaRepository, onOpenItem: (String, Int) -> Unit, onOpenMenu: () -> Unit = {}) {
    val allFollowed by repository.observeFollowed().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Planning only makes sense for things with a known, not-yet-past release date: an
    // "Ended" show or a "Returning Series" with no next episode announced has
    // nextAirDate == null and must therefore not appear here (it lands in Bookmark once
    // released, if not yet watched).
    val followed = remember(allFollowed) {
        allFollowed
            .map { it.copy(nextAirDate = it.nextAirDate?.takeIf { d -> d.isNotBlank() }) }
            .filter { item ->
                when {
                    item.nextAirDate != null -> !DateUtils.isReleased(item.nextAirDate)
                    item.mediaType == "movie" -> true
                    item.mediaType == "tv" -> item.nextEpisodeName != null &&
                            item.status != "Ended" && item.status != "Canceled"
                    else -> false
                }
            }
            .sortedWith(compareBy({ it.nextAirDate == null }, { it.nextAirDate }))
    }

    // "Just in case" refresh when the screen opens: throttled on the Repository side
    // (6h normally, 30min if nextAirDate is still unknown for an ongoing show), so this
    // doesn't systematically trigger a network call.
    LaunchedEffect(allFollowed.map { it.tmdbId }) {
        allFollowed.filter { it.mediaType == "tv" }.forEach { item ->
            repository.refreshFollowedIfStale(item)
        }
    }

    fun refreshAllManually() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            try {
                allFollowed.filter { it.mediaType == "tv" }.forEach { item ->
                    repository.refreshFollowedIfStale(item, force = true)
                }
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (followed.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.planning_no_upcoming),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                // Grouped by release date while keeping the order already sorted by the DAO
                // (nextAirDate IS NULL then ASC), to show a single "Friday, August 14" header
                // above every release on that day.
                val groups: List<Pair<String?, List<FollowedItem>>> = remember(followed) {
                    val result = mutableListOf<Pair<String?, MutableList<FollowedItem>>>()
                    followed.forEach { item ->
                        val last = result.lastOrNull()
                        if (last != null && last.first == item.nextAirDate) {
                            last.second.add(item)
                        } else {
                            result.add(item.nextAirDate to mutableListOf(item))
                        }
                    }
                    result
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 0.dp,
                        end = 16.dp,
                        bottom = 95.dp
                    ),
                ) {
                    groups.forEach { (date, groupItems) ->
                        item(key = "header_${date ?: "unknown"}") {
                            SectionTitle(
                                if (date.isNullOrBlank()) stringResource(R.string.date_unknown) else DateUtils.formatDay(LocalContext.current, date),
                            )
                        }
                        items(groupItems, key = { item -> "${item.mediaType}_${item.tmdbId}" }) { followedItem ->
                            Row(Modifier.padding(bottom = 8.dp)) {
                                CinemaItemRow(
                                    repository = repository,
                                    item = followedItem.toPlanningCardData(LocalContext.current),
                                    onClick = { onOpenItem(followedItem.mediaType, followedItem.tmdbId) },
                                    trailingContent = {
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
                                                text = DateUtils.daysRemainingLabel(LocalContext.current, followedItem.nextAirDate),
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
