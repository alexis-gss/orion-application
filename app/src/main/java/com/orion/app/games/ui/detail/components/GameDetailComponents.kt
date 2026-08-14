package com.orion.app.games.ui.detail.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.ui.components.PosterImage
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.games.data.IgdbGame
import com.orion.app.games.data.IgdbVideo
import com.orion.app.games.ui.components.GameCardData
import com.orion.app.games.ui.components.gameCarouselSection
import com.orion.app.games.ui.components.toCardData

/** Loading state for the game detail page — same shape as DetailScreenState on the cinema side. */
sealed interface GameDetailScreenState {
    data object Loading : GameDetailScreenState
    data class Success(val game: IgdbGame) : GameDetailScreenState
    data class Error(val message: String) : GameDetailScreenState
}

/**
 * "Info" section: publisher and game engine, same card style as BookInfoSection on the
 * books side. The developer is already shown in infoLine (DetailHeader); the publisher can
 * differ (e.g. an indie studio distributed by a major publisher) and so deserves its own row.
 */
@Composable
fun GameInfoSection(game: IgdbGame) {
    val publisherLabel = stringResource(R.string.game_publisher_label)
    val engineLabel = stringResource(R.string.game_engine_label)
    val rows = listOfNotNull(
        game.publisherNames?.let { publisherLabel to it },
        game.gameEngineNames?.let { engineLabel to it },
    )
    if (rows.isEmpty()) return

    val extended = OrionColors.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = stringResource(R.string.game_info_title), modifier = Modifier.padding(horizontal = 16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(extended.cardSurface)
                .border(1.dp, extended.cardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            rows.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                }
                if (index != rows.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(color = extended.cardBorder)
                }
            }
        }
    }
}

/**
 * "Media" tab on the games side, same order and visual language as MediaTabContent on the
 * cinema side: trailers first (YouTube thumbnail + play button), then screenshots.
 * Artworks (IGDB promotional key art) are no longer shown here: the first screenshot now
 * serves as the banner (see DetailHeader in GameDetailScreen), and the rest of the
 * artworks added little compared to actual screenshots.
 */
@Composable
fun GameMediaTabContent(
    videos: List<IgdbVideo>,
    screenshots: List<String>,
    onImageClick: (images: List<String>, index: Int) -> Unit
) {
    val context = LocalContext.current
    val extended = OrionColors.colors

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (videos.isEmpty() && screenshots.isEmpty()) {
            Text(
                text = stringResource(R.string.media_none_available),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
            return
        }

        if (videos.isNotEmpty()) {
            SectionTitle(
                title = stringResource(R.string.media_trailers_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(videos) { _, video ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(200.dp)
                            .height(115.dp)
                            .clickable {
                                video.youtubeUrl?.let { url ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                            PosterImage(
                                model = video.thumbnailUrl,
                                contentDescription = video.name,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.media_play_description),
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(40.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                extended.navBarSelectedContainerAlt,
                                                extended.navBarSelectedContainer
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (screenshots.isNotEmpty()) {
            SectionTitle(
                title = stringResource(R.string.game_screenshots_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(screenshots) { index, url ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .height(130.dp)
                            .clickable { onImageClick(screenshots, index) }
                    ) {
                        PosterImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(130.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * "Time to beat" section: completion times estimated by IGDB (HowLongToBeat data), shown
 * as side-by-side mini stats, in the same card style as the rest of the page (SectionTitle
 * + background Card, with no dependency on an equivalent cinema component since TMDB has
 * no such concept).
 *
 * Always rendered once the game is loaded (no longer hidden while loading or when IGDB has
 * no data at all): each stat shows a pulsing placeholder while [isLoading] is true, then
 * either the resolved duration or "—" if that particular value stays null.
 */
@Composable
fun TimeToBeatSection(timeToBeat: com.orion.app.games.data.IgdbTimeToBeat?, isLoading: Boolean) {
    val extended = OrionColors.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = stringResource(R.string.game_time_to_beat_title), modifier = Modifier.padding(horizontal = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(extended.cardSurface)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimeToBeatStat(stringResource(R.string.game_time_to_beat_fast), timeToBeat?.hastily, isLoading)
            TimeToBeatStat(stringResource(R.string.game_time_to_beat_normal), timeToBeat?.normally, isLoading)
            TimeToBeatStat(stringResource(R.string.game_time_to_beat_complete), timeToBeat?.completely, isLoading)
        }
    }
}

@Composable
private fun TimeToBeatStat(label: String, seconds: Long?, isLoading: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = seconds?.let { "${it / 3600}h" } ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * "Related content" tab: only the game's expansions, remasters and remakes, each in its
 * own carousel-style row (same card as the rest of the games domain) — only non-empty
 * categories are shown. DLC, standalone packs/addons, bundles and editions are no longer
 * shown here (removed on request: too much noise, not very relevant).
 */
fun LazyListScope.relatedContentTabContent(
    expansions: List<IgdbGame>,
    remasters: List<IgdbGame>,
    remakes: List<IgdbGame>,
    onSelectGame: (Int) -> Unit,
    // Titles pre-resolved by the caller (composable context): this function is a
    // LazyListScope extension, not a @Composable, so it cannot call stringResource() itself
    // outside an item { } block.
    labels: RelatedContentLabels,
) {
    if (expansions.isEmpty() && remasters.isEmpty() && remakes.isEmpty()) {
        item {
            Text(
                text = labels.noneAvailable,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    if (expansions.isNotEmpty()) {
        gameCarouselSection(
            title = labels.expansions,
            items = expansions.map { it.toCardData() },
            emptyLabel = "",
            onItemClick = { onSelectGame(it.igdbId) },
            sectionHorizontalPadding = 16.dp,
        )
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
    if (remasters.isNotEmpty()) {
        gameCarouselSection(
            title = labels.remasters,
            items = remasters.map { it.toCardData() },
            emptyLabel = "",
            onItemClick = { onSelectGame(it.igdbId) },
            sectionHorizontalPadding = 16.dp,
        )
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
    if (remakes.isNotEmpty()) {
        gameCarouselSection(
            title = labels.remakes,
            items = remakes.map { it.toCardData() },
            emptyLabel = "",
            onItemClick = { onSelectGame(it.igdbId) },
            sectionHorizontalPadding = 16.dp,
        )
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

data class RelatedContentLabels(
    val noneAvailable: String,
    val expansions: String,
    val remasters: String,
    val remakes: String,
)

/** Builds [RelatedContentLabels] from string resources, to be called in a composable
 *  context (e.g. at the top of GameDetailContent) before entering the LazyColumn. */
@Composable
fun rememberRelatedContentLabels(): RelatedContentLabels = RelatedContentLabels(
    noneAvailable = stringResource(R.string.game_related_content_none),
    expansions = stringResource(R.string.game_expansions_title),
    remasters = stringResource(R.string.game_remasters_title),
    remakes = stringResource(R.string.game_remakes_title),
)

/**
 * "Same franchise" tab: the other games in the same IGDB franchise(s) as the game being
 * viewed (e.g. other GTA games on GTA 6's page), shown as a SeeAllScreen-style grid rather
 * than a plain carousel since some franchises have a lot of titles.
 */
fun LazyListScope.franchiseTabContent(
    games: List<IgdbGame>,
    onSelectGame: (Int) -> Unit,
    emptyLabel: String,
) {
    if (games.isEmpty()) {
        item {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }
    items(games.map { it.toCardData() }, key = { it.key }) { data ->
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            com.orion.app.games.ui.components.GameItemRow(
                item = data,
                onClick = { onSelectGame(data.igdbId) }
            )
        }
    }
    item { Spacer(modifier = Modifier.height(8.dp)) }
}

fun LazyListScope.gameRecommendationsSection(
    recommendations: List<IgdbGame>,
    onSelectGame: (Int) -> Unit,
    title: String
) {
    val items: List<GameCardData> = recommendations.map { it.toCardData() }
    gameCarouselSection(
        title = title,
        items = items,
        emptyLabel = "",
        onItemClick = { onSelectGame(it.igdbId) },
        sectionHorizontalPadding = 16.dp,
    )
}