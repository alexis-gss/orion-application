package com.orion.app.cinema.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orion.app.cinema.data.FavoriteItem
import com.orion.app.cinema.data.FollowedItem
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.cinema.data.WatchedItem
import com.orion.app.cinema.data.toGenresCsv
import com.orion.app.cinema.ui.detail.components.DetailScreenState
import com.orion.app.core.ui.components.FloatingTopBar
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.orion.app.R

/**
 * Entry point for the detail page (movie or TV show). Loads the TMDB page (a single call,
 * credits/videos/watch-providers/recommendations already included via append_to_response),
 * then delegates rendering to MovieDetailContent / TvDetailContent based on `mediaType`.
 * Heavier data (HD media gallery, a season's episodes) is only loaded when the
 * corresponding tab is clicked — see Repository.getMovieGallery/getTvGallery/getSeasonDetail.
 */
@Composable
fun CinemaDetailScreen(
    repository: CinemaRepository,
    mediaType: String,
    tmdbId: Int,
    onBack: () -> Unit,
    onOpenItem: (mediaType: String, tmdbId: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var state by remember(mediaType, tmdbId) { mutableStateOf<DetailScreenState>(DetailScreenState.Loading) }
    // Incremented to force a manual reload (the "Retry" button).
    var reloadKey by remember(mediaType, tmdbId) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mediaType, tmdbId, reloadKey) {
        state = DetailScreenState.Loading
        state = try {
            if (mediaType == "tv") {
                DetailScreenState.TvSuccess(repository.getTvDetail(tmdbId))
            } else {
                DetailScreenState.MovieSuccess(repository.getMovieDetail(tmdbId))
            }
        } catch (e: Exception) {
            DetailScreenState.Error(e.message ?: context.getString(R.string.detail_load_error))
        }
    }

    // Followed / favorites / watched: observed live from the local database (as
    // everywhere else in the app) so the action buttons react immediately.
    val followedList by repository.observeFollowed().collectAsState(initial = emptyList())
    val favoriteList by repository.observeFavorites().collectAsState(initial = emptyList())
    val watchedList by repository.observeWatched().collectAsState(initial = emptyList())

    val isFollowed = followedList.any { it.tmdbId == tmdbId && it.mediaType == mediaType }
    val isFavorite = favoriteList.any { it.tmdbId == tmdbId && it.mediaType == mediaType }
    val isMovieWatched = mediaType == "movie" &&
            watchedList.any { it.tmdbId == tmdbId && it.mediaType == mediaType }

    val listState = rememberLazyListState()
    val currentTitle = when (val current = state) {
        is DetailScreenState.MovieSuccess -> current.movie.title
        is DetailScreenState.TvSuccess -> current.tvShow.name
        else -> null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            is DetailScreenState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is DetailScreenState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = current.message, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { reloadKey++ }) {
                        Text(stringResource(R.string.detail_retry))
                    }
                }
            }

            is DetailScreenState.MovieSuccess -> {
                val movie = current.movie
                MovieDetailContent(
                    repository = repository,
                    movie = movie,
                    listState = listState,
                    isFollowed = isFollowed,
                    onFollowToggle = {
                        scope.launch {
                            if (isFollowed) {
                                repository.unfollow(tmdbId, mediaType)
                            } else {
                                repository.follow(
                                    FollowedItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = movie.title,
                                        posterPath = movie.posterPath,
                                        releaseDate = movie.releaseDate,
                                        nextAirDate = movie.releaseDate,
                                        genres = movie.genres.toGenresCsv(),
                                        voteAverage = movie.voteAverage,
                                        addedAt = System.currentTimeMillis(),
                                    )
                                )
                            }
                        }
                    },
                    isWatched = isMovieWatched,
                    onWatchedToggle = {
                        scope.launch {
                            if (isMovieWatched) {
                                repository.unmarkWatched(tmdbId, mediaType, null, null)
                            } else {
                                repository.markWatched(
                                    WatchedItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = movie.title,
                                        posterPath = movie.posterPath,
                                        watchedAt = System.currentTimeMillis(),
                                        genres = movie.genres.toGenresCsv(),
                                        durationMinutes = movie.runtime,
                                        voteAverage = movie.voteAverage,
                                        releaseDate = movie.releaseDate
                                    )
                                )
                                if (isFollowed) {
                                    repository.unfollow(tmdbId, mediaType)
                                }
                            }
                        }
                    },
                    isFavorite = isFavorite,
                    onFavoriteToggle = {
                        scope.launch {
                            if (isFavorite) {
                                repository.removeFavorite(tmdbId, mediaType)
                            } else {
                                repository.addFavorite(
                                    FavoriteItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = movie.title,
                                        posterPath = movie.posterPath,
                                        addedAt = System.currentTimeMillis(),
                                        genres = movie.genres.toGenresCsv(),
                                        voteAverage = movie.voteAverage,
                                        releaseDate = movie.releaseDate
                                    )
                                )
                                if (!isMovieWatched) {
                                    repository.markWatched(
                                        WatchedItem(
                                            tmdbId = tmdbId,
                                            mediaType = mediaType,
                                            title = movie.title,
                                            posterPath = movie.posterPath,
                                            watchedAt = System.currentTimeMillis(),
                                            genres = movie.genres.toGenresCsv(),
                                            durationMinutes = movie.runtime,
                                            voteAverage = movie.voteAverage,
                                            releaseDate = movie.releaseDate
                                        )
                                    )
                                }
                                if (isFollowed) {
                                    repository.unfollow(tmdbId, mediaType)
                                }
                            }
                        }
                    },
                    onSelectMedia = onOpenItem
                )
            }

            is DetailScreenState.TvSuccess -> {
                val tvShow = current.tvShow
                TvDetailContent(
                    repository = repository,
                    tvShow = tvShow,
                    listState = listState,
                    isFollowed = isFollowed,
                    onFollowToggle = {
                        scope.launch {
                            if (isFollowed) {
                                repository.unfollow(tmdbId, mediaType)
                            } else {
                                repository.follow(
                                    FollowedItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = tvShow.name,
                                        posterPath = tvShow.posterPath,
                                        nextAirDate = tvShow.nextEpisodeToAir?.airDate,
                                        nextEpisodeName = tvShow.nextEpisodeToAir?.name,
                                        nextSeasonNumber = tvShow.nextEpisodeToAir?.seasonNumber,
                                        nextEpisodeNumber = tvShow.nextEpisodeToAir?.episodeNumber,
                                        status = tvShow.status,
                                        lastAiredSeasonNumber = tvShow.lastEpisodeToAir?.seasonNumber,
                                        lastAiredEpisodeNumber = tvShow.lastEpisodeToAir?.episodeNumber,
                                        lastCheckedAt = System.currentTimeMillis(),
                                        releaseDate = tvShow.firstAirDate,
                                        genres = tvShow.genres.toGenresCsv(),
                                        voteAverage = tvShow.voteAverage,
                                        addedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    },
                    isFavorite = isFavorite,
                    onFavoriteToggle = {
                        scope.launch {
                            if (isFavorite) {
                                repository.removeFavorite(tmdbId, mediaType)
                            } else {
                                repository.addFavorite(
                                    FavoriteItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = tvShow.name,
                                        posterPath = tvShow.posterPath,
                                        addedAt = System.currentTimeMillis(),
                                        genres = tvShow.genres.toGenresCsv(),
                                        voteAverage = tvShow.voteAverage,
                                        releaseDate = tvShow.firstAirDate
                                    )
                                )
                            }
                        }
                    },
                    onSelectMedia = onOpenItem
                )
            }
        }

        // Floating bar: back button + title that appears on scroll
        FloatingTopBar(
            listState = listState,
            currentTitle = currentTitle,
            onBack = onBack,
        )
    }
}
