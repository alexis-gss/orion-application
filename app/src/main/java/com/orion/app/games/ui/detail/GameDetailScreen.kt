package com.orion.app.games.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orion.app.core.ui.components.ActionButtons
import com.orion.app.core.ui.components.DetailHeader
import com.orion.app.core.ui.components.FloatingTopBar
import com.orion.app.core.ui.components.SynopsisSection
import com.orion.app.R
import com.orion.app.core.ui.components.PosterGalleryDialog
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.games.data.FavoriteGame
import com.orion.app.games.data.FollowedGame
import com.orion.app.games.data.GamesRepository
import com.orion.app.games.data.IgdbGame
import com.orion.app.games.data.IgdbPlatform
import com.orion.app.games.data.IgdbTimeToBeat
import com.orion.app.games.data.PlayedGame
import com.orion.app.games.ui.components.igdbRatingTo10
import com.orion.app.games.ui.detail.components.GameDetailScreenState
import com.orion.app.games.ui.detail.components.GameInfoSection
import com.orion.app.games.ui.detail.components.GameMediaTabContent
import com.orion.app.games.ui.detail.components.TimeToBeatSection
import com.orion.app.games.ui.detail.components.franchiseTabContent
import com.orion.app.games.ui.detail.components.gameRecommendationsSection
import com.orion.app.games.ui.detail.components.relatedContentTabContent
import kotlinx.coroutines.launch

/**
 * Game detail page, restructured to follow exactly the same skeleton as
 * DetailScreen/MovieDetailContent on the cinema side: banner + overlapping poster header,
 * action buttons, Details/Media tabs, synopsis, recommendations, a floating bar with the
 * title on scroll. IGDB doesn't have every TMDB block (no cast, no finances, no streaming
 * platforms): these sections have no equivalent here.
 */
@Composable
fun GameDetailScreen(
    repository: GamesRepository,
    igdbId: Int,
    onBack: () -> Unit,
    onOpenItem: (igdbId: Int) -> Unit = {}
) {
    var state by remember(igdbId) { mutableStateOf<GameDetailScreenState>(GameDetailScreenState.Loading) }
    var reloadKey by remember(igdbId) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var isFavorite by remember(igdbId) { mutableStateOf(false) }
    var isFollowed by remember(igdbId) { mutableStateOf(false) }
    var isCompleted by remember(igdbId) { mutableStateOf(false) }

    LaunchedEffect(igdbId, reloadKey) {
        state = GameDetailScreenState.Loading
        state = try {
            val g = repository.getGameDetail(igdbId)
            if (g != null) {
                isFavorite = repository.isFavorite(igdbId)
                isFollowed = repository.isFollowed(igdbId)
                isCompleted = repository.getPlayedStatus(igdbId)?.status == "completed"
                GameDetailScreenState.Success(g)
            } else {
                GameDetailScreenState.Error(context.getString(R.string.detail_load_error))
            }
        } catch (e: Exception) {
            GameDetailScreenState.Error(e.message ?: context.getString(R.string.detail_load_error))
        }
    }

    val currentTitle = (state as? GameDetailScreenState.Success)?.game?.name

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            is GameDetailScreenState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is GameDetailScreenState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = current.message, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { reloadKey++ }) {
                        Text(stringResource(R.string.detail_retry))
                    }
                }
            }

            is GameDetailScreenState.Success -> {
                GameDetailContent(
                    repository = repository,
                    game = current.game,
                    listState = listState,
                    isFollowed = isFollowed,
                    isFavorite = isFavorite,
                    isCompleted = isCompleted,
                    onFollowToggle = { g ->
                        scope.launch {
                            if (isFollowed) {
                                repository.unfollow(igdbId)
                            } else {
                                repository.follow(
                                    FollowedGame(
                                        igdbId = igdbId,
                                        title = g.name,
                                        coverUrl = g.coverUrl,
                                        releaseTimestamp = g.nextReleaseTimestamp ?: g.firstReleaseDate,
                                        isReleased = g.firstReleaseDate?.let { it * 1000 < System.currentTimeMillis() } ?: false,
                                        lastCheckedAt = System.currentTimeMillis(),
                                        genres = g.genres.joinToString(",") { it.name }.ifBlank { null },
                                        voteAverage = igdbRatingTo10(g.displayRating),
                                        studio = g.developerNames
                                    )
                                )
                            }
                            isFollowed = !isFollowed
                        }
                    },
                    onCompletedToggle = { g ->
                        scope.launch {
                            if (isCompleted) {
                                repository.removePlayedStatus(igdbId)
                            } else {
                                repository.setPlayedStatus(
                                    PlayedGame(
                                        igdbId = igdbId,
                                        title = g.name,
                                        coverUrl = g.coverUrl,
                                        status = "completed",
                                        playedAt = System.currentTimeMillis(),
                                        genres = g.genres.joinToString(",") { it.name }.ifBlank { null },
                                        voteAverage = igdbRatingTo10(g.displayRating),
                                        releaseDate = g.firstReleaseDate?.let { it * 1000 }
                                    )
                                )
                                if (isFollowed) {
                                    repository.unfollow(igdbId)
                                    isFollowed = false
                                }
                            }
                            isCompleted = !isCompleted
                        }
                    },
                    onFavoriteToggle = { g ->
                        scope.launch {
                            if (isFavorite) {
                                repository.removeFavorite(igdbId)
                            } else {
                                repository.addFavorite(
                                    FavoriteGame(
                                        igdbId = igdbId,
                                        title = g.name,
                                        coverUrl = g.coverUrl,
                                        addedAt = System.currentTimeMillis(),
                                        genres = g.genres.joinToString(",") { it.name }.ifBlank { null },
                                        voteAverage = igdbRatingTo10(g.displayRating),
                                        releaseDate = g.firstReleaseDate?.let { it * 1000 }
                                    )
                                )
                                if (!isCompleted) {
                                    repository.setPlayedStatus(
                                        PlayedGame(
                                            igdbId = igdbId,
                                            title = g.name,
                                            coverUrl = g.coverUrl,
                                            status = "completed",
                                            playedAt = System.currentTimeMillis(),
                                            genres = g.genres.joinToString(",") { it.name }.ifBlank { null },
                                            voteAverage = igdbRatingTo10(g.displayRating),
                                            releaseDate = g.firstReleaseDate?.let { it * 1000 }
                                        )
                                    )
                                    isCompleted = true
                                }
                                if (isFollowed) {
                                    repository.unfollow(igdbId)
                                    isFollowed = false
                                }
                            }
                            isFavorite = !isFavorite
                        }
                    },
                    onSelectGame = onOpenItem,
                )
            }
        }

        FloatingTopBar(
            listState = listState,
            currentTitle = currentTitle,
            onBack = onBack,
        )
    }
}

@Composable
private fun PlatformChip(platform: IgdbPlatform) {
    val extended = OrionColors.colors
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(extended.cardSurface)
            .border(1.dp, extended.cardBorder, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (platform.logoUrl != null) {
            com.orion.app.core.ui.components.PosterImage(
                model = platform.logoUrl,
                contentDescription = platform.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.height(20.dp).width(28.dp)
            )
        } else {
            Text(
                text = platform.abbreviation ?: platform.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GameDetailContent(
    repository: GamesRepository,
    game: IgdbGame,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isFollowed: Boolean,
    isFavorite: Boolean,
    isCompleted: Boolean,
    onFollowToggle: (IgdbGame) -> Unit,
    onCompletedToggle: (IgdbGame) -> Unit,
    onFavoriteToggle: (IgdbGame) -> Unit,
    onSelectGame: (Int) -> Unit,
) {
    val detailsTab = stringResource(R.string.tab_details)
    val mediaTab = stringResource(R.string.tab_media)
    val relatedTab = stringResource(R.string.game_related_content_title)
    val franchiseTab = stringResource(R.string.game_franchise_tab)
    val hasFranchiseTab = game.licenseIds.isNotEmpty()
    val gameTabs = buildList {
        add(detailsTab)
        add(mediaTab)
        if (game.hasRelatedContent) add(relatedTab)
        if (hasFranchiseTab) add(franchiseTab)
    }
    // Index of the "Related content"/"Same franchise" tab within gameTabs, computed
    // dynamically since both are optional and independent from each other.
    val relatedTabIndex = if (game.hasRelatedContent) gameTabs.indexOf(relatedTab) else -1
    val franchiseTabIndex = if (hasFranchiseTab) gameTabs.indexOf(franchiseTab) else -1
    var selectedTabIndex by remember(game.id) { mutableIntStateOf(0) }
    val similarGamesTitle = stringResource(R.string.game_similar_title)
    val relatedContentLabels = com.orion.app.games.ui.detail.components.rememberRelatedContentLabels()

    var galleryViewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var galleryViewerIndex by remember { mutableIntStateOf(0) }
    if (galleryViewerImages.isNotEmpty()) {
        PosterGalleryDialog(
            posters = galleryViewerImages,
            initialIndex = galleryViewerIndex,
            onDismiss = { galleryViewerImages = emptyList() }
        )
    }

    var recommendations by remember(game.id) { mutableStateOf<List<IgdbGame>>(emptyList()) }
    LaunchedEffect(game.id) {
        recommendations = repository.getGamesByIds(game.similarGames)
    }

    var relatedExpansions by remember(game.id) { mutableStateOf<List<IgdbGame>>(emptyList()) }
    var relatedRemasters by remember(game.id) { mutableStateOf<List<IgdbGame>>(emptyList()) }
    var relatedRemakes by remember(game.id) { mutableStateOf<List<IgdbGame>>(emptyList()) }
    LaunchedEffect(game.id) {
        if (!game.hasRelatedContent) return@LaunchedEffect
        relatedExpansions = repository.getGamesByIds(game.expansions)
        relatedRemasters = repository.getGamesByIds(game.remasters)
        relatedRemakes = repository.getGamesByIds(game.remakes)
    }

    var franchiseGames by remember(game.id) { mutableStateOf<List<IgdbGame>>(emptyList()) }
    LaunchedEffect(game.id) {
        if (game.licenseIds.isEmpty()) return@LaunchedEffect
        franchiseGames = repository.getGamesInFranchise(game)
    }

    val infoLine = remember(game.id) {
        listOfNotNull(
            game.year,
            game.developerNames
        ).joinToString(" · ")
    }

    // A game not yet released cannot be marked "completed" or "favorite" — only
    // following it (to be notified on release) makes sense at this stage.
    val isGameReleased = remember(game.id) {
        game.firstReleaseDate?.let { it * 1000 < System.currentTimeMillis() } ?: false
    }

    var timeToBeat by remember(game.id) { mutableStateOf<IgdbTimeToBeat?>(null) }
    var isTimeToBeatLoading by remember(game.id) { mutableStateOf(true) }
    LaunchedEffect(game.id) {
        isTimeToBeatLoading = true
        timeToBeat = repository.getTimeToBeat(game.id)
        isTimeToBeatLoading = false
    }

    // Play button: opens the first YouTube video from the media, like the trailer
    // button on the cinema side.
    val context = LocalContext.current
    val onTrailerAction: (() -> Unit)? = game.videos.firstOrNull()?.youtubeUrl?.let { url ->
        { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            DetailHeader(
                // Artworks removed: the banner now uses the first screenshot, more
                // representative of the actual game than promotional key art.
                backdropPath = game.screenshots.firstOrNull()?.fullUrl,
                posterPath = game.coverUrl,
                title = game.name,
                tagline = null,
                voteAverage = igdbRatingTo10(game.displayRating),
                infoLine = infoLine.ifBlank { null },
                genres = game.genres.map { it.name }
            )
        }

        item {
            ActionButtons(
                isFollowed = isFollowed,
                followDisabled = isCompleted || isFavorite,
                onFollowToggle = { onFollowToggle(game) },
                isWatched = isCompleted,
                onWatchedToggle = { onCompletedToggle(game) },
                watchedDisabled = !isGameReleased,
                watchedLabel = stringResource(R.string.game_status_completed),
                notWatchedLabel = stringResource(R.string.game_status_completed),
                isFavorite = isFavorite,
                onFavoriteToggle = { onFavoriteToggle(game) },
                favoriteDisabled = !isGameReleased,
                onTrailerAction = onTrailerAction,
            )
        }

        item {
            TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                gameTabs.forEachIndexed { index, tabLabel ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = tabLabel) }
                    )
                }
            }
        }

        when (selectedTabIndex) {
            relatedTabIndex -> {
                relatedContentTabContent(
                    expansions = relatedExpansions,
                    remasters = relatedRemasters,
                    remakes = relatedRemakes,
                    onSelectGame = onSelectGame,
                    labels = relatedContentLabels,
                )
            }
            franchiseTabIndex -> {
                franchiseTabContent(
                    games = franchiseGames,
                    onSelectGame = onSelectGame,
                    emptyLabel = context.getString(R.string.game_franchise_empty),
                )
            }
            0 -> {
                item {
                    SynopsisSection(
                        tagline = null,
                        overview = game.summary ?: game.storyline,
                    )
                }
                item {
                    TimeToBeatSection(timeToBeat = timeToBeat, isLoading = isTimeToBeatLoading)
                }
                item { GameInfoSection(game) }
                if (game.platforms.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            SectionTitle(
                                title = stringResource(R.string.game_platforms_title),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(game.platforms) { platform ->
                                    PlatformChip(platform)
                                }
                            }
                        }
                    }
                }
                if (recommendations.isNotEmpty()) {
                    gameRecommendationsSection(
                        recommendations = recommendations,
                        onSelectGame = onSelectGame,
                        title = similarGamesTitle,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
            1 -> {
                item {
                    GameMediaTabContent(
                        videos = game.videos,
                        screenshots = game.screenshots.mapNotNull { it.fullUrl },
                        onImageClick = { images, index ->
                            galleryViewerImages = images
                            galleryViewerIndex = index
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}