package com.orion.app.cinema.ui.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.orion.app.cinema.data.GalleryImage
import com.orion.app.cinema.data.CinemaGallery
import com.orion.app.cinema.data.MovieDetail
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.core.ui.components.PosterGalleryDialog
import com.orion.app.cinema.ui.detail.components.CastAndCrewSection
import com.orion.app.cinema.ui.detail.components.FinancialInfoSection
import com.orion.app.cinema.ui.detail.components.MediaTabContent
import com.orion.app.cinema.ui.detail.components.StreamingProvidersRow
import com.orion.app.cinema.ui.detail.components.recommendationsSection
import com.orion.app.core.ui.components.ActionButtons
import com.orion.app.core.ui.components.DetailHeader
import com.orion.app.core.ui.components.SynopsisSection
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.orion.app.core.util.DateUtils
import androidx.compose.ui.res.stringResource
import com.orion.app.R

// Tab labels are resolved in the Composable via stringResource (see below).

@Composable
fun MovieDetailContent(
    repository: CinemaRepository,
    movie: MovieDetail,
    isFollowed: Boolean = false,
    onFollowToggle: () -> Unit = {},
    isWatched: Boolean = false,
    onWatchedToggle: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteToggle: () -> Unit = {},
    onSelectMedia: (mediaType: String, tmdbId: Int) -> Unit = { _, _ -> },
    listState: LazyListState = rememberLazyListState(),
) {
    val movieTabs = listOf(stringResource(R.string.tab_details), stringResource(R.string.tab_media))
    val recommendationsTitle = stringResource(R.string.recommendations_title)
    var selectedTabIndex by remember(movie.id) { mutableIntStateOf(0) }

    // Gallery (HD posters/backdrops) loaded on demand, only the first time the Media
    // tab is opened — not during the page's initial load.
    var isGalleryLoading by remember(movie.id) { mutableStateOf(false) }
    var gallery by remember(movie.id) { mutableStateOf<CinemaGallery?>(null) }
    var hasFetchedGallery by remember(movie.id) { mutableStateOf(false) }

    val isMovieReleased = remember(movie.id) {
        DateUtils.isWatchable(movie.releaseDate)
    }

    LaunchedEffect(movie.id, selectedTabIndex) {
        if (selectedTabIndex == 1 && !hasFetchedGallery) {
            isGalleryLoading = true
            gallery = repository.getMovieGallery(movie.id, movie.posterPath, movie.backdropPath)
            isGalleryLoading = false
            hasFetchedGallery = true
        }
    }

    // Fullscreen viewer, opened by clicking an image in the Media tab.
    var galleryViewerImages by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var galleryViewerIndex by remember { mutableIntStateOf(0) } // avoids autoboxing (lint: AutoboxingStateCreation)
    if (galleryViewerImages.isNotEmpty()) {
        PosterGalleryDialog(
            posters = galleryViewerImages.map { it.fullQualityUrl },
            initialIndex = galleryViewerIndex,
            onDismiss = { galleryViewerImages = emptyList() }
        )
    }

    // ActionButtons (shared component, see core/ui/components) no longer knows about
    // VideoItem: the YouTube-opening lambda is built here instead of passing it the video object.
    val context = LocalContext.current
    val trailer = remember(movie.id) {
        movie.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }
            ?: movie.videos?.results?.firstOrNull { it.site == "YouTube" }
    }
    val onTrailerAction: (() -> Unit)? = trailer?.let { tr ->
        {
            context.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/watch?v=${tr.key}"))
            )
        }
    }

    val infoLine = remember(movie.id) {
        listOfNotNull(
            movie.releaseDate?.take(4),
            movie.runtime?.takeIf { it > 0 }?.let { "${it / 60}h${(it % 60).toString().padStart(2, '0')}" }
        ).joinToString(" · ")
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            DetailHeader(
                backdropPath = movie.backdropPath,
                posterPath = movie.posterPath,
                title = movie.title,
                tagline = movie.tagline,
                voteAverage = movie.voteAverage,
                infoLine = infoLine.ifBlank { null },
                genres = movie.genres.map { it.name }
            )
        }

        item {
            ActionButtons(
                isFollowed = isFollowed,
                followDisabled = isWatched || isFavorite,
                onFollowToggle = onFollowToggle,
                isWatched = isWatched,
                onWatchedToggle = onWatchedToggle,
                watchedDisabled = !isMovieReleased,
                isFavorite = isFavorite,
                onFavoriteToggle = onFavoriteToggle,
                favoriteDisabled = !isMovieReleased,
                onTrailerAction = onTrailerAction,
            )
        }

        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                movieTabs.forEachIndexed { index, tabLabel ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = tabLabel) }
                    )
                }
            }
        }

        when (selectedTabIndex) {
            0 -> {
                item {
                    SynopsisSection(
                        tagline = null,
                        overview = movie.overview,
                    )
                }
                item {
                    CastAndCrewSection(
                        cast = movie.credits?.cast.orEmpty(),
                        crew = movie.credits?.crew.orEmpty()
                    )
                }
                item {
                    FinancialInfoSection(
                        budget = movie.budget,
                        revenue = movie.revenue
                    )
                }
                // US region: matches this app's English/US-focused content (see the
                // language and TMDB "en-US" query defaults elsewhere in the codebase).
                val providers = movie.watchProviders?.results?.get("US")?.flatrate.orEmpty()
                if (providers.isNotEmpty()) {
                    item {
                        StreamingProvidersRow(
                            providers = providers,
                            posterUrlProvider = { path -> repository.posterUrl(path) }
                        )
                    }
                }
                val recommendations = movie.recommendations?.results.orEmpty()
                if (recommendations.isNotEmpty()) {
                    recommendationsSection(
                        repository = repository,
                        recommendations = recommendations,
                        onSelectMedia = onSelectMedia,
                        title = recommendationsTitle,
                    )
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            1 -> {
                item {
                    MediaTabContent(
                        videos = movie.videos?.results.orEmpty(),
                        isLoadingImages = isGalleryLoading,
                        gallery = gallery,
                        onImageClick = { images, index ->
                            galleryViewerImages = images
                            galleryViewerIndex = index
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
