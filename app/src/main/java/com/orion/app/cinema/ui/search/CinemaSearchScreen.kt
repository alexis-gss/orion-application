package com.orion.app.cinema.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.cinema.data.SearchResult
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.core.ui.components.InlineSearchFilterBar
import com.orion.app.core.ui.components.CinemaFilter
import com.orion.app.cinema.ui.components.CinemaItemRow
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.components.SortOption
import com.orion.app.cinema.ui.components.toCardData
import com.orion.app.core.ui.theme.OrionColors
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext

private const val DEBOUNCE_MS = 500L
private const val POPULAR_LIMIT = 15

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaSearchScreen(repository: CinemaRepository, onOpenItem: (String, Int) -> Unit, onOpenMenu: () -> Unit = {}) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var rawResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var cinemaFilter by remember { mutableStateOf(CinemaFilter.ALL) }
    var sortOption by remember { mutableStateOf(SortOption.RELEVANCE) }

    var popularItems by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isPopularLoading by remember { mutableStateOf(true) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            popularItems = repository.getPopular()
        } catch (e: Exception) {
            // silencieux
        } finally {
            isPopularLoading = false
        }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            rawResults = emptyList()
            errorMessage = null
            return@LaunchedEffect
        }
        delay(DEBOUNCE_MS)
        isLoading = true
        errorMessage = null
        try {
            rawResults = repository.search(query)
        } catch (e: Exception) {
            errorMessage = context.getString(R.string.search_error)
        } finally {
            isLoading = false
        }
    }

    val filteredResults: List<SearchResult> = remember(rawResults, cinemaFilter, sortOption) {
        rawResults
            .filter { r ->
                when (cinemaFilter) {
                    CinemaFilter.ALL -> true
                    CinemaFilter.MOVIE -> r.resolvedMediaType == "movie"
                    CinemaFilter.TV -> r.resolvedMediaType == "tv"
                }
            }
            .let { list ->
                when (sortOption) {
                    SortOption.RELEVANCE -> list
                    SortOption.RATING -> list.sortedByDescending { it.voteAverage ?: 0.0 }
                    SortOption.RECENT -> list.sortedByDescending {
                        it.releaseDate ?: it.firstAirDate ?: ""
                    }
                }
            }
    }

    val extended = OrionColors.colors

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.nav_search), onOpenMenu = onOpenMenu)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Zone fixe (ne scrolle pas) ---
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
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

            if (query.isNotBlank()) {
                InlineSearchFilterBar(
                    filterLabel = stringResource(cinemaFilter.label),
                    filterOptions = CinemaFilter.entries,
                    filterOptionLabel = { stringResource(it.label) },
                    onFilterSelected = { cinemaFilter = it },
                    sortLabel = stringResource(sortOption.label),
                    sortOptions = SortOption.entries,
                    sortOptionLabel = { stringResource(it.label) },
                    onSortSelected = { sortOption = it },
                    hasActiveFilters = cinemaFilter != CinemaFilter.ALL || sortOption != SortOption.RELEVANCE,
                    onReset = {
                        cinemaFilter = CinemaFilter.ALL
                        sortOption = SortOption.RELEVANCE
                    },
                )
            }

            // --- Zone scrollable ---
            Box(modifier = Modifier.weight(1f)) {
                when {
                    query.isBlank() -> {
                        if (isPopularLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 0.dp,
                                    end = 16.dp,
                                    bottom = 95.dp
                                ),
                            ) {
                                item { SectionTitle(stringResource(R.string.search_popular_now)) }
                                itemsIndexed(
                                    popularItems.take(POPULAR_LIMIT),
                                    key = { _, item -> "${item.resolvedMediaType}_${item.id}" }
                                ) { index, popularItem ->
                                    Row(
                                        Modifier.padding(
                                            bottom = if (index == popularItems.take(POPULAR_LIMIT).lastIndex) 0.dp else 12.dp
                                        )
                                    ) {
                                        CinemaItemRow(
                                            repository = repository,
                                            item = popularItem.toCardData(context),
                                            onClick = { onOpenItem(popularItem.resolvedMediaType, popularItem.id) }
                                        )
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage != null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(errorMessage!!, modifier = Modifier.padding(24.dp))
                        }
                    }

                    filteredResults.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.search_no_results))
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 8.dp,
                                end = 16.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredResults, key = { "${it.resolvedMediaType}_${it.id}" }) { r ->
                                CinemaItemRow(
                                    repository = repository,
                                    item = r.toCardData(context),
                                    onClick = { onOpenItem(r.resolvedMediaType, r.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}