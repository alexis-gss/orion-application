package com.orion.app.cinema.ui.bookmark

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.cinema.ui.components.CinemaCardData
import com.orion.app.cinema.ui.components.cinemaCarouselSection
import com.orion.app.cinema.ui.components.toBookmarkCardData
import com.orion.app.core.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaBookmarkScreen(repository: CinemaRepository, onOpenItem: (String, Int) -> Unit, onSeeAll: (String, List<CinemaCardData>) -> Unit, onOpenMenu: () -> Unit = {}) {
    val followed by repository.observeFollowed().collectAsState(initial = emptyList())
    val watched by repository.observeWatched().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    // 1. Shows marked "fully watched" (seasonNumber == null)
    val fullyWatchedShowIds = remember(watched) {
        watched.filter { it.mediaType == "tv" && it.seasonNumber == null }
            .map { it.tmdbId }
            .toSet()
    }

    // 2. Watched episodes in the "tmdbId_season_episode" format
    val watchedEpisodesSet = remember(watched) {
        watched.filter { it.mediaType == "tv" && it.seasonNumber != null && it.episodeNumber != null }
            .map { "${it.tmdbId}_${it.seasonNumber}_${it.episodeNumber}" }
            .toSet()
    }

    // 2bis. Number of episodes watched per show (for the posters' "remaining to watch" badge).
    val watchedEpisodesCountByShow = remember(watched) {
        watched.filter { it.mediaType == "tv" && it.seasonNumber != null && it.episodeNumber != null }
            .groupingBy { it.tmdbId }
            .eachCount()
    }

    val movies = remember(followed) {
        followed.filter {
            it.mediaType == "movie" && DateUtils.isReleased(it.releaseDate)
        }.map { it.toBookmarkCardData(context) }
    }

    val tvToWatch = remember(followed, fullyWatchedShowIds, watchedEpisodesSet) {
        followed.filter { item ->
            if (item.mediaType != "tv") return@filter false

            // A followed show with no episode aired yet (e.g. VisionQuest, announced but
            // not released) has nothing to "watch" in this screen's sense: it stays in
            // Planning (upcoming) but must not appear here until it has at least one past
            // air date.
            val hasAiredContent = item.lastAiredSeasonNumber != null ||
                    (item.nextAirDate != null && DateUtils.isWatchable(item.nextAirDate))
            if (!hasAiredContent) return@filter false

            // If the whole show is checked as watched -> hide it right away!
            if (item.tmdbId in fullyWatchedShowIds) return@filter false

            // Last episode "officially" aired according to TMDB
            val lastSeason = item.lastAiredSeasonNumber
            val lastEpisode = item.lastAiredEpisodeNumber

            // TMDB can publish next_episode_to_air with a slight delay after its actual
            // release: if its date is already past or today, treat it as released.
            val nextIsActuallyReleased = item.nextAirDate != null &&
                    DateUtils.isWatchable(item.nextAirDate)

            val effectiveSeason = if (nextIsActuallyReleased) item.nextSeasonNumber else lastSeason
            val effectiveEpisode = if (nextIsActuallyReleased) item.nextEpisodeNumber else lastEpisode

            if (effectiveSeason != null && effectiveEpisode != null) {
                val isEffectiveEpisodeWatched =
                    "${item.tmdbId}_${effectiveSeason}_${effectiveEpisode}" in watchedEpisodesSet
                !isEffectiveEpisodeWatched
            } else if (lastSeason != null && lastEpisode != null) {
                val isLastEpisodeWatched = "${item.tmdbId}_${lastSeason}_${lastEpisode}" in watchedEpisodesSet
                !isLastEpisodeWatched
            } else {
                val hasWatchedEpisodes = watchedEpisodesSet.any { it.startsWith("${item.tmdbId}_") }
                !hasWatchedEpisodes
            }
        }
    }

    // Shows in tvToWatch that watching has already started on (at least 1 episode watched)
    val inProgressSeries = remember(tvToWatch, watchedEpisodesSet, watchedEpisodesCountByShow) {
        tvToWatch.filter { item ->
            watchedEpisodesSet.any { it.startsWith("${item.tmdbId}_") }
        }.map { it.toBookmarkCardData(context, watchedEpisodesCountByShow[it.tmdbId] ?: 0) }
    }
    // Shows in tvToWatch that have never been started
    val notStartedSeries = remember(tvToWatch, watchedEpisodesSet, watchedEpisodesCountByShow) {
        tvToWatch.filter { item ->
            watchedEpisodesSet.none { it.startsWith("${item.tmdbId}_") }
        }.map { it.toBookmarkCardData(context, watchedEpisodesCountByShow[it.tmdbId] ?: 0) }
    }

    val isEmpty = movies.isEmpty() && inProgressSeries.isEmpty() && notStartedSeries.isEmpty()

    // "Just in case" refresh on open, throttled to 6h on the Repository side like Planning.
    LaunchedEffect(followed.map { it.tmdbId }) {
        followed.forEach { item ->
            repository.refreshFollowedIfStale(item)
        }
    }

    fun refreshAllManually() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            try {
                followed.forEach { item ->
                    repository.refreshFollowedIfStale(item, force = true)
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                onOpenMenu = onOpenMenu,
                title = stringResource(R.string.nav_bookmark),
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
        }) { padding ->
        if (isEmpty) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.bookmark_not_elements),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
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
                        title = context.getString(R.string.films_to_watch),
                        items = movies.take(15),
                        emptyLabel = context.getString(R.string.films_not_watched),
                        onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                        onSeeAllClick = { onSeeAll(context.getString(R.string.films_to_watch), movies) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    cinemaCarouselSection(
                        repository = repository,
                        title = context.getString(R.string.series_started_to_watch),
                        items = inProgressSeries.take(15),
                        emptyLabel = context.getString(R.string.series_started_not_watched),
                        onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                        onSeeAllClick = { onSeeAll(context.getString(R.string.series_started_to_watch), inProgressSeries) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    cinemaCarouselSection(
                        repository = repository,
                        title = context.getString(R.string.series_unstarted_to_watch),
                        items = notStartedSeries.take(15),
                        emptyLabel = context.getString(R.string.series_unstarted_not_watched),
                        onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                        onSeeAllClick = { onSeeAll(context.getString(R.string.series_unstarted_to_watch), notStartedSeries) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}
