package com.orion.app.cinema.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.cinema.data.WatchedItem
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.cinema.ui.components.CinemaCardData
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.cinema.ui.components.cinemaCarouselSection
import com.orion.app.cinema.ui.components.toFavoriteCardData
import com.orion.app.cinema.ui.components.toWatchedMovieCardData
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.util.DateUtils
import java.util.Calendar
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaAccountScreen(
    repository: CinemaRepository,
    onOpenItem: (String, Int) -> Unit,
    onOpenStats: () -> Unit,
    onSeeAll: (title: String, items: List<CinemaCardData>) -> Unit,
    onOpenMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val favoritesMoviesTitle = stringResource(R.string.favorites_movies_title)
    val favoritesShowsTitle = stringResource(R.string.favorites_shows_title)
    val favoritesEmpty = stringResource(R.string.favorites_empty)
    val moviesWatchedTitle = stringResource(R.string.movies_watched_title)
    val moviesWatchedEmpty = stringResource(R.string.stats_no_movies_watched)
    val showsWatchedTitle = stringResource(R.string.shows_watched_title)
    val showsWatchedEmpty = stringResource(R.string.shows_watched_empty)
    val watched by repository.observeWatched().collectAsState(initial = emptyList())
    val favorites by repository.observeFavorites().collectAsState(initial = emptyList())

    val favoriteMovies = remember(favorites) {
        favorites.filter { it.mediaType == "movie" }
            .sortedByDescending { it.addedAt }
            .map { it.toFavoriteCardData(context, DateUtils.formatShortDate(it.addedAt)) }
    }
    val favoriteShows = remember(favorites) {
        favorites.filter { it.mediaType == "tv" }
            .sortedByDescending { it.addedAt }
            .map { it.toFavoriteCardData(context, DateUtils.formatShortDate(it.addedAt)) }
    }

    val movies = remember(watched) {
        watched.filter { it.mediaType == "movie" }
            .sortedByDescending { it.watchedAt }
            .map { it.toWatchedMovieCardData(context, DateUtils.formatShortDate(it.watchedAt)) }
    }

    // Summary of a watched show: one row per show (not one per episode), with the
    // number of episodes checked off. The grouping stays here since it's specific to this
    // screen; only the result (MediaCardData) is shared with the rest of the app.
    val tv = remember(watched) {
        watched.filter { it.mediaType == "tv" }
            .groupBy { it.tmdbId }
            .values
            .map { items -> items.maxBy { it.watchedAt } to items.size }
            .sortedByDescending { (latest, _) -> latest.watchedAt }
            .map { (latest, episodesWatched) ->
                CinemaCardData(
                    key = "tv_${latest.tmdbId}",
                    tmdbId = latest.tmdbId,
                    mediaType = "tv",
                    title = latest.title,
                    posterPath = latest.posterPath,
                    subtitle = context.resources.getQuantityString(R.plurals.episodes_watched_count_short, episodesWatched, episodesWatched),
                    trailingText = DateUtils.formatShortDate(latest.watchedAt),
                    // Was missing here (unlike toWatchedMovieCardData/toFavoriteCardData),
                    // hence an empty genre picker in "See all" for watched shows even
                    // though WatchedItem.genres was properly set at write time.
                    genres = latest.genres
                )
            }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_account),
                onOpenMenu = onOpenMenu,
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.account_stats_description))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            StatsSection(watched, onOpenStats = onOpenStats)
            if (movies.isEmpty() && tv.isEmpty() && favoriteMovies.isEmpty() && favoriteShows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.account_no_items),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        top = 0.dp,
                        end = 0.dp,
                        bottom = 95.dp
                    ),
                ) {
                    cinemaCarouselSection(
                        repository = repository,
                        title = favoritesMoviesTitle,
                        items = favoriteMovies.take(15),
                        emptyLabel = favoritesEmpty,
                        onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                        onSeeAllClick = { onSeeAll(favoritesMoviesTitle, favoriteMovies) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    cinemaCarouselSection(
                        repository = repository,
                        title = moviesWatchedTitle,
                        items = movies.take(15),
                        emptyLabel = moviesWatchedEmpty,
                        onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                        onSeeAllClick = { onSeeAll(moviesWatchedTitle, movies) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    cinemaCarouselSection(
                        repository = repository,
                        title = favoritesShowsTitle,
                        items = favoriteShows.take(15),
                        emptyLabel = favoritesEmpty,
                        onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                        onSeeAllClick = { onSeeAll(favoritesShowsTitle, favoriteShows) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    cinemaCarouselSection(
                        repository = repository,
                        title = showsWatchedTitle,
                        items = tv.take(15),
                        emptyLabel = showsWatchedEmpty,
                        onItemClick = { onOpenItem("tv", it.tmdbId) },
                        onSeeAllClick = { onSeeAll(showsWatchedTitle, tv) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

// ---------------- Statistics (specific to this screen) ----------------
// Purely derived from the list already observed in memory (watched_items):
// no network call or extra query, just filtering/counting.

private data class StatItem(val value: Int, val label: String, val accent: Boolean = false)

@Composable
private fun StatsSection(watched: List<WatchedItem>, onOpenStats: () -> Unit) {
    val movies = remember(watched) { watched.count { it.seasonNumber == null } }
    val episodes = remember(watched) { watched.count { it.seasonNumber != null } }
    val thisMonth = remember(watched) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        watched.count {
            cal.timeInMillis = it.watchedAt
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }

    val stats = listOf(
        StatItem(movies, stringResource(R.string.filters_movie)),
        StatItem(episodes, stringResource(R.string.account_episodes_label)),
        StatItem(thisMonth, stringResource(R.string.stats_this_month), accent = true)
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle(stringResource(R.string.stats_title))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenStats),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            stats.forEach { stat ->
                StatCard(stat, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatCard(stat: StatItem, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (stat.accent) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)
                        )
                    )
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
