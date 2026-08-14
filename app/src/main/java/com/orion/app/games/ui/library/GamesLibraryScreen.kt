package com.orion.app.games.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.ui.components.AdvancedFilterBar
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.core.ui.components.SortDirection
import com.orion.app.core.ui.components.SortField
import com.orion.app.games.data.GamesRepository
import com.orion.app.games.ui.components.GameCover
import com.orion.app.games.ui.components.toCardData

private const val PAGE_SIZE = 30

/**
 * Equivalent of BookmarkScreen + SeeAllScreen on the games side: a single list
 * (already-released followed games), shown as a cover grid loaded 30 at a time on
 * scroll, rather than a plain capped row. Followed games not yet released stay in Planning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesLibraryScreen(
    repository: GamesRepository,
    onOpenItem: (Int) -> Unit,
    onOpenMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val released by repository.observeReleasedFollowedNotStarted().collectAsState(initial = emptyList())
    val cards = remember(released) { released.map { it.toCardData(context) } }

    val availableGenres = remember(cards) {
        cards.asSequence()
            .flatMap { it.genres?.split(",")?.map(String::trim).orEmpty() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var sortField by remember { mutableStateOf<SortField?>(null) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESC) }

    val filtered = remember(cards, selectedGenre, sortField, sortDirection) {
        val byGenre = if (selectedGenre == null) cards else cards.filter {
            it.genres?.split(",")?.map(String::trim)?.contains(selectedGenre) == true
        }
        when (sortField) {
            null -> byGenre
            SortField.RATING -> byGenre.sortedWith(compareBy(nullsFirst()) { it.rating })
            SortField.RELEASE_DATE -> byGenre.sortedWith(compareBy(nullsFirst()) { it.releaseDate })
            SortField.ADDED_DATE -> byGenre.sortedWith(compareBy(nullsFirst()) { it.addedAt })
        }.let { sorted -> if (sortDirection == SortDirection.DESC) sorted.reversed() else sorted }
    }

    var loadedCount by remember(filtered) { mutableIntStateOf(minOf(PAGE_SIZE, filtered.size)) }
    val visible = remember(filtered, loadedCount) { filtered.take(loadedCount) }
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    LaunchedEffect(gridState, filtered) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= loadedCount - 6 && loadedCount < filtered.size) {
                    loadedCount = minOf(loadedCount + PAGE_SIZE, filtered.size)
                }
            }
    }

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.games_library_title), onOpenMenu = onOpenMenu) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (released.isNotEmpty()) {
                AdvancedFilterBar(
                    genres = availableGenres,
                    selectedGenre = selectedGenre,
                    onGenreSelected = { selectedGenre = it },
                    sortField = sortField,
                    sortDirection = sortDirection,
                    onSortFieldSelected = { field ->
                        if (sortField == field) {
                            sortDirection = if (sortDirection == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
                        } else {
                            sortField = field
                            sortDirection = SortDirection.DESC
                        }
                    },
                    onReset = {
                        selectedGenre = null
                        sortField = null
                        sortDirection = SortDirection.DESC
                    },
                )
            }
            if (released.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.games_library_empty),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp).let {
                    PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 95.dp)
                },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.igdbId }) { card ->
                    GameCover(
                        coverUrl = card.coverUrl,
                        contentDescription = card.title,
                        modifier = Modifier
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenItem(card.igdbId) }
                    )
                }
                if (loadedCount < filtered.size) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
            }
        }
    }
}
