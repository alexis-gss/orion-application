package com.orion.app.cinema.ui.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orion.app.cinema.data.GalleryImage
import com.orion.app.cinema.data.CinemaGallery
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.cinema.data.TvDetail
import com.orion.app.core.ui.components.PosterGalleryDialog
import com.orion.app.cinema.ui.detail.components.CastAndCrewSection
import com.orion.app.cinema.ui.detail.components.MediaTabContent
import com.orion.app.cinema.ui.detail.components.StreamingProvidersRow
import com.orion.app.cinema.ui.detail.components.recommendationsSection
import com.orion.app.cinema.ui.detail.components.seasonsSection
import com.orion.app.core.ui.components.ActionButtons
import com.orion.app.core.ui.components.DetailHeader
import com.orion.app.core.ui.components.SynopsisSection
import com.orion.app.core.util.DateUtils
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.orion.app.R

// Tab labels are resolved in the Composable via stringResource (see below).

@Composable
fun TvDetailContent(
    repository: CinemaRepository,
    tvShow: TvDetail,
    isFollowed: Boolean = false,
    onFollowToggle: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteToggle: () -> Unit = {},
    onSelectMedia: (mediaType: String, tmdbId: Int) -> Unit = { _, _ -> },
    listState: LazyListState = rememberLazyListState(),
) {
    val tvTabs = listOf(stringResource(R.string.tab_details), stringResource(R.string.tab_seasons), stringResource(R.string.tab_media))
    val recommendationsTitle = stringResource(R.string.recommendations_title)
    var selectedTabIndex by remember(tvShow.id) { mutableIntStateOf(0) }

    var isGalleryLoading by remember(tvShow.id) { mutableStateOf(false) }
    var gallery by remember(tvShow.id) { mutableStateOf<CinemaGallery?>(null) }
    var hasFetchedGallery by remember(tvShow.id) { mutableStateOf(false) }

    LaunchedEffect(tvShow.id, selectedTabIndex) {
        if (selectedTabIndex == 2 && !hasFetchedGallery) {
            isGalleryLoading = true
            gallery = repository.getTvGallery(tvShow.id, tvShow.posterPath, tvShow.backdropPath)
            isGalleryLoading = false
            hasFetchedGallery = true
        }
    }

    var galleryViewerImages by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var galleryViewerIndex by remember { mutableIntStateOf(0) } // avoids autoboxing (lint: AutoboxingStateCreation)
    if (galleryViewerImages.isNotEmpty()) {
        PosterGalleryDialog(
            posters = galleryViewerImages.map { it.fullQualityUrl },
            initialIndex = galleryViewerIndex,
            onDismiss = { galleryViewerImages = emptyList() }
        )
    }

    val watchedEpisodes by repository.observeWatched().collectAsState(initial = emptyList())
    val watchedForThisShow = remember(watchedEpisodes, tvShow.id) {
        watchedEpisodes.filter { it.tmdbId == tvShow.id && it.mediaType == "tv" }
    }

    // ActionButtons (shared component, see core/ui/components) no longer knows about
    // VideoItem: the YouTube-opening lambda is built here instead of passing it the video object.
    val context = LocalContext.current
    val trailer = remember(tvShow.id) {
        tvShow.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }
            ?: tvShow.videos?.results?.firstOrNull { it.site == "YouTube" }
    }
    val onTrailerAction: (() -> Unit)? = trailer?.let { tr ->
        {
            context.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/watch?v=${tr.key}"))
            )
        }
    }

    // A not-yet-released show (no episode aired yet) cannot be marked favorite: a
    // "favorite" shouldn't apply to content that was never actually watchable. Following
    // stays always possible (that's exactly the intended use for an upcoming release).
    val isShowReleased = remember(tvShow.id) { DateUtils.isReleased(tvShow.firstAirDate) }

    val statusLabel = when (tvShow.status) {
        "Returning Series" -> stringResource(R.string.tv_status_returning)
        "Ended" -> stringResource(R.string.tv_status_ended)
        "Canceled" -> stringResource(R.string.tv_status_canceled)
        "In Production" -> stringResource(R.string.tv_status_in_production)
        "Planned" -> stringResource(R.string.tv_status_planned)
        "Pilot" -> stringResource(R.string.tv_status_pilot)
        else -> tvShow.status
    }

    // pluralStringResource cannot be called inside remember's lambda (DisallowComposableCalls),
    // so it's resolved here, in the Composable's body, before memoizing the final concatenation.
    val seasonsLabel = tvShow.numberOfSeasons?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.seasons_count, it, it) }
    val episodesLabel = tvShow.numberOfEpisodes?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.episodes_count, it, it) }
    val infoLine = remember(tvShow.id, statusLabel, seasonsLabel, episodesLabel) {
        listOfNotNull(
            tvShow.firstAirDate?.take(4),
            seasonsLabel,
            episodesLabel,
            statusLabel,
        ).joinToString(" · ")
    }

    val allEpisodesWatched = remember(tvShow, watchedForThisShow) {
        if (tvShow.status != "Ended" && tvShow.status != "Canceled") {
            false
        } else {
            val totalEpisodes = tvShow.numberOfEpisodes ?: 0
            totalEpisodes > 0 && watchedForThisShow.size >= totalEpisodes
        }
    }

    LaunchedEffect(allEpisodesWatched, isFollowed) {
        if (allEpisodesWatched && isFollowed) {
            onFollowToggle()
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            DetailHeader(
                backdropPath = tvShow.backdropPath,
                posterPath = tvShow.posterPath,
                title = tvShow.name,
                tagline = tvShow.tagline,
                voteAverage = tvShow.voteAverage,
                infoLine = infoLine.ifBlank { null },
                genres = tvShow.genres.map { it.name }
            )
        }

        item {
            ActionButtons(
                isFollowed = isFollowed,
                followDisabled = allEpisodesWatched,
                onFollowToggle = onFollowToggle,
                isFavorite = isFavorite,
                onFavoriteToggle = onFavoriteToggle,
                favoriteDisabled = !isShowReleased,
                onTrailerAction = onTrailerAction,
            )
        }

        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tvTabs.forEachIndexed { index, tabLabel ->
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
                        overview = tvShow.overview,
                    )
                }
                item {
                    CastAndCrewSection(
                        cast = tvShow.credits?.cast.orEmpty(),
                        crew = tvShow.credits?.crew.orEmpty()
                    )
                }
                // US region: matches this app's English/US-focused content (see the
                // language and TMDB "en-US" query defaults elsewhere in the codebase).
                val providers = tvShow.watchProviders?.results?.get("US")?.flatrate.orEmpty()
                if (providers.isNotEmpty()) {
                    item {
                        StreamingProvidersRow(
                            providers = providers,
                            posterUrlProvider = { path -> repository.posterUrl(path) }
                        )
                    }
                }
                val recommendations = tvShow.recommendations?.results.orEmpty()
                if (recommendations.isNotEmpty()) {
                    recommendationsSection(
                        repository = repository,
                        recommendations = recommendations,
                        onSelectMedia = onSelectMedia,
                        title = recommendationsTitle
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
                seasonsSection(
                    repository = repository,
                    tvId = tvShow.id,
                    tvTitle = tvShow.name,
                    seasons = tvShow.seasons,
                    watchedEpisodes = watchedForThisShow,
                    tvGenres = tvShow.genres,
                )
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            2 -> {
                item {
                    MediaTabContent(
                        videos = tvShow.videos?.results.orEmpty(),
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