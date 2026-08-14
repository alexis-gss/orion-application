package com.orion.app.books.ui.all

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.books.ui.components.BookBadge
import com.orion.app.books.ui.components.BookCardData
import com.orion.app.books.ui.components.BookCover
import com.orion.app.core.ui.components.AdvancedFilterBar
import com.orion.app.core.ui.components.SortDirection
import com.orion.app.core.ui.components.SortField
import com.orion.app.core.ui.theme.OrionColors

private const val PAGE_SIZE = 30

/**
 * "See all" on the books side: a grid wall of covers (3 columns), same presentation as
 * SeeAllScreen (cinema) / GamesSeeAllScreen, with the same genre/rating/date filters.
 * Replaces the old row-by-row vertical list (BookItemRow).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksSeeAllScreen(
    title: String,
    allItems: List<BookCardData>,
    onOpenItem: (BookCardData) -> Unit,
    onBack: () -> Unit,
) {
    // Each Hardcover category stored as CSV (generic segment, e.g. "Fiction") is split
    // to build the list of available chips, sorted alphabetically and deduplicated — same
    // logic as cinema/games, see BookCardMappers.toCardData.
    val availableGenres: List<String> = remember(allItems) {
        allItems.asSequence()
            .flatMap { it.genres?.split(",")?.map(String::trim).orEmpty() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var sortField by remember { mutableStateOf<SortField?>(null) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESC) }

    val filteredItems: List<BookCardData> = remember(allItems, selectedGenre, sortField, sortDirection) {
        val filtered = if (selectedGenre == null) {
            allItems
        } else {
            allItems.filter { item ->
                item.genres?.split(",")?.map(String::trim)?.contains(selectedGenre) == true
            }
        }
        when (sortField) {
            null -> filtered
            SortField.RATING -> filtered.sortedWith(compareBy(nullsFirst()) { it.rating })
            SortField.RELEASE_DATE -> filtered.sortedWith(compareBy(nullsFirst()) { it.releaseDate })
            SortField.ADDED_DATE -> filtered.sortedWith(compareBy(nullsFirst()) { it.addedAt })
        }.let { sorted -> if (sortDirection == SortDirection.DESC) sorted.reversed() else sorted }
    }

    var loadedCount by remember(filteredItems) { mutableIntStateOf(minOf(PAGE_SIZE, filteredItems.size)) }
    val visibleItems: List<BookCardData> = remember(filteredItems, loadedCount) { filteredItems.take(loadedCount) }
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    LaunchedEffect(gridState, filteredItems) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= loadedCount - 6 &&
                    loadedCount < filteredItems.size
                ) {
                    loadedCount = minOf(loadedCount + PAGE_SIZE, filteredItems.size)
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 0.dp,
                    bottom = 0.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleItems, key = { it.key }) { item ->
                    val extended = OrionColors.colors
                    Box(
                        modifier = Modifier
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenItem(item) }
                    ) {
                        BookCover(
                            coverUrl = item.coverUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize()
                        )
                        item.trailingText?.let { text ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.55f)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(extended.posterOverlayBottom, extended.posterOverlayTop)
                                        )
                                    )
                            )
                            BookBadge(
                                text = text,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            )
                        }
                    }
                }
                if (loadedCount < filteredItems.size) {
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
