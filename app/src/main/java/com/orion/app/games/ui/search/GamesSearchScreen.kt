package com.orion.app.games.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.core.ui.components.InlineSearchFilterBar
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import androidx.compose.ui.res.stringResource
import com.orion.app.core.ui.components.SortOption
import com.orion.app.R
import com.orion.app.games.data.GamesRepository
import com.orion.app.games.data.IgdbGame
import com.orion.app.games.ui.components.GameItemRow
import com.orion.app.games.ui.components.igdbRatingTo10
import com.orion.app.games.ui.components.toCardData
import kotlinx.coroutines.delay

private const val DEBOUNCE_MS = 500L
private const val POPULAR_LIMIT = 15
private const val ALL_GENRES = "All"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesSearchScreen(repository: GamesRepository, onOpenItem: (Int) -> Unit, onOpenMenu: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<IgdbGame>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var genreFilter by remember { mutableStateOf(ALL_GENRES) }
    var sortOption by remember { mutableStateOf(SortOption.RELEVANCE) }

    var popularItems by remember { mutableStateOf<List<IgdbGame>>(emptyList()) }
    var isPopularLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            popularItems = repository.getPopular()
        } catch (e: Exception) {
            // silent, same as the cinema side
        } finally {
            isPopularLoading = false
        }
    }

    LaunchedEffect(query) {
        genreFilter = ALL_GENRES
        sortOption = SortOption.RELEVANCE
        if (query.isBlank()) {
            results = emptyList()
            errorMessage = null
            return@LaunchedEffect
        }
        delay(DEBOUNCE_MS)
        isLoading = true
        errorMessage = null
        try {
            results = repository.search(query)
        } catch (e: Exception) {
            errorMessage = context.getString(R.string.games_search_error)
        } finally {
            isLoading = false
        }
    }

    // Available genres derived from the results themselves (no fixed IGDB taxonomy to
    // load separately), like the Movie/Series filter on the cinema side but adapted:
    // games can have several genres, so the list is open-ended rather than fixed.
    val availableGenres = remember(results) {
        listOf(ALL_GENRES) + results.flatMap { it.genres.map { g -> g.name } }.distinct().sorted()
    }

    val filteredResults: List<IgdbGame> = remember(results, genreFilter, sortOption) {
        results
            .filter { genreFilter == ALL_GENRES || it.genres.any { g -> g.name == genreFilter } }
            .let { list ->
                when (sortOption) {
                    SortOption.RELEVANCE -> list
                    SortOption.RATING -> list.sortedByDescending { igdbRatingTo10(it.displayRating) ?: 0.0 }
                    SortOption.RECENT -> list.sortedByDescending { it.firstReleaseDate ?: 0L }
                }
            }
    }

    val extended = OrionColors.colors

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.nav_search), onOpenMenu = onOpenMenu) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(50)),
                placeholder = { Text(stringResource(R.string.games_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = extended.chipSurface,
                    focusedContainerColor = extended.chipSurface,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (query.isNotBlank() && results.isNotEmpty()) {
                InlineSearchFilterBar(
                    filterLabel = if (genreFilter == ALL_GENRES) stringResource(R.string.filters_genre_all) else genreFilter,
                    filterOptions = availableGenres,
                    filterOptionLabel = { if (it == ALL_GENRES) stringResource(R.string.filters_genre_all) else it },
                    onFilterSelected = { genreFilter = it },
                    sortLabel = stringResource(sortOption.label),
                    sortOptions = SortOption.entries,
                    sortOptionLabel = { stringResource(it.label) },
                    onSortSelected = { sortOption = it },
                    hasActiveFilters = genreFilter != ALL_GENRES || sortOption != SortOption.RELEVANCE,
                    onReset = {
                        genreFilter = ALL_GENRES
                        sortOption = SortOption.RELEVANCE
                    },
                )
            }

            Box(Modifier.weight(1f)) {
                when {
                    query.isBlank() -> {
                        if (isPopularLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 95.dp)
                            ) {
                                item { SectionTitle(stringResource(R.string.search_popular_now)) }
                                itemsIndexed(popularItems.take(POPULAR_LIMIT), key = { _, g -> g.id }) { index, game ->
                                    Row(Modifier.padding(bottom = if (index == popularItems.take(POPULAR_LIMIT).lastIndex) 0.dp else 12.dp)) {
                                        GameItemRow(item = game.toCardData(), onClick = { onOpenItem(game.id) })
                                    }
                                }
                                item { Spacer(Modifier.height(8.dp)) }
                            }
                        }
                    }
                    isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(errorMessage!!, modifier = Modifier.padding(24.dp))
                    }
                    filteredResults.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.search_no_results))
                    }
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredResults, key = { it.id }) { game ->
                            GameItemRow(item = game.toCardData(), onClick = { onOpenItem(game.id) })
                        }
                    }
                }
            }
        }
    }
}
