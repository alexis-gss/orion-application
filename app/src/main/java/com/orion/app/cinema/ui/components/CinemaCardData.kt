package com.orion.app.cinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.core.ui.components.PosterImage
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.ui.theme.colorForRating
import java.util.Locale

/**
 * Generic display model for a media item (movie or TV show), independent of the data
 * source (WatchedItem, FollowedItem, SearchResult, ...).
 *
 * Each screen only knows about this type for rendering: it maps its own data model to a
 * MediaCardData via the functions in CinemaCardMappers.kt, then passes the resulting list
 * to the components below. Result: to change a row's or a poster's style throughout the
 * app (Account, Bookmark, Planning, Search...), there's now only a single file to edit:
 * this one.
 */
data class CinemaCardData(
    val key: String,
    val tmdbId: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val posterPath: String?,
    val subtitle: String? = null,
    val trailingText: String? = null,
    /** TMDB rating out of 10, when known (e.g. search/popular results).
     *  Shown as a TV Time-style colored badge (green/amber/red). */
    val rating: Double? = null,
    /** Comma-separated TMDB genres (e.g. "Action,Drama"), used by SeeAllScreen's genre filter. */
    val genres: String? = null,
    /** TMDB release / first air date (yyyy-MM-dd), used by SeeAllScreen's release-date filter. */
    val releaseDate: String? = null,
    /** Epoch-millis timestamp of when the item was added (watched, followed, or favorited), used by SeeAllScreen's added-date filter. */
    val addedAt: Long? = null
)

private val PosterShape = RoundedCornerShape(12.dp)
private val DefaultRowPosterSize: Pair<Dp, Dp> = 64.dp to 92.dp
private val DefaultCarouselPosterSize: Pair<Dp, Dp> = 130.dp to 192.dp

/**
 * Standalone poster (image + rounded corners). Base building block reused by MediaItemRow
 * and MediaCarouselRow — edit here to change the poster style everywhere.
 */
@Composable
fun CinemaPoster(
    repository: CinemaRepository,
    posterPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    PosterImage(
        model = repository.posterUrl(posterPath),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/**
 * LAYOUT 1 — one row per item: poster + title + subtitle, in a floating card with a soft
 * shadow and a thin border (TV Time-style). `trailingContent` lets a slot be added on the
 * right (e.g. Planning's "days remaining" AssistChip); if not provided, the colored rating
 * (if known) or `trailingText` is shown instead.
 *
 * Used by Account, Bookmark, Planning and Search: any style change here automatically
 * applies everywhere.
 */
@Composable
fun CinemaItemRow(
    repository: CinemaRepository,
    item: CinemaCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    posterSize: Pair<Dp, Dp> = DefaultRowPosterSize,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val extended = OrionColors.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = PosterShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.25f))
            .clip(PosterShape)
            .background(extended.cardSurface)
            .border(1.dp, extended.cardBorder, PosterShape)
            .clickable(onClick = onClick)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            CinemaPoster(
                repository = repository,
                posterPath = item.posterPath,
                contentDescription = item.title,
                modifier = Modifier
                    .size(posterSize.first, posterSize.second)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.subtitle?.let { subtitle ->
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item.rating?.let { rating ->
                RatingBadge(rating = rating)
            }
        }
        when {
            trailingContent != null -> {
                Spacer(Modifier.width(10.dp))
                trailingContent()
            }
            item.trailingText != null -> {
                Spacer(Modifier.width(10.dp))
                CinemaBadge(text = item.trailingText)
            }
        }
    }
}

/**
 * Small pill badge (status/date style) reused across rows and carousel posters.
 */
@Composable
fun CinemaBadge(text: String, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = extended.badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * TV Time-style colored rating badge: green if well-rated, amber if average, red if low.
 * Intentionally a separate color (never the brand's gold accent) so it stays a quick,
 * at-a-glance signal, distinct from the rest of the UI.
 */
@Composable
fun RatingBadge(rating: Double) {
    val extended = OrionColors.colors
    val color = extended.colorForRating(rating)
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            text = String.format(Locale.US, "%.1f", rating), // forced decimal point (lint: DefaultLocale)
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

/**
 * LAYOUT 2 — TV Time-style horizontal poster carousel: title in a gradient overlay at the
 * bottom of the poster, colored rating at the top left, optional badge (trailingText) at
 * the top right, pronounced drop shadow. Ready to use for a compact section ("Featured",
 * recommendations, etc.): just pass it a List<CinemaCardData> and a click callback, from
 * anywhere in the app.
 */
@Composable
fun CinemaCarouselRow(
    repository: CinemaRepository,
    items: List<CinemaCardData>,
    onItemClick: (CinemaCardData) -> Unit,
    modifier: Modifier = Modifier,
    posterSize: Pair<Dp, Dp> = DefaultCarouselPosterSize,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp),
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = contentPadding,
    ) {
        items(items, key = { it.key }) { item ->
            CinemaPosterCard(
                repository = repository,
                item = item,
                posterSize = posterSize,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun CinemaPosterCard(
    repository: CinemaRepository,
    item: CinemaCardData,
    posterSize: Pair<Dp, Dp>,
    onClick: () -> Unit,
) {
    val extended = OrionColors.colors
    Box(
        modifier = Modifier
            .size(posterSize.first, posterSize.second)
            .shadow(elevation = 10.dp, shape = PosterShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.35f))
            .clip(PosterShape)
            .clickable(onClick = onClick)
    ) {
        CinemaPoster(
            repository = repository,
            posterPath = item.posterPath,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize()
        )
        item.trailingText?.let { badge ->
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
            CinemaBadge(
                text = badge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
    }
}

/**
 * Factors out the "section header + list, or empty message" pattern repeated across
 * Account and Bookmark (e.g. "Watched movies" / "No movies watched yet."). Uses
 * CinemaItemRow for each item, so it automatically benefits from any changes there.
 */
fun LazyListScope.cinemaRowSection(
    repository: CinemaRepository,
    title: String,
    items: List<CinemaCardData>,
    emptyLabel: String,
    onItemClick: (CinemaCardData) -> Unit,
    trailingContent: (@Composable (CinemaCardData) -> Unit)? = null
) {
    item {
        SectionTitle(title = title)
    }
    if (items.isEmpty()) {
        item {
            EmptySectionHint(emptyLabel)
        }
    } else {
        items(items, key = { it.key }) { data ->
            CinemaItemRow(
                repository = repository,
                item = data,
                onClick = { onItemClick(data) },
                trailingContent = trailingContent?.let { render -> { render(data) } }
            )
        }
    }
}

/**
 * Equivalent of [cinemaRowSection] but with LAYOUT 2: a section header followed by a
 * horizontal poster carousel (or the empty message). Same principle: a single call in the
 * screen's LazyColumn, the carousel's style is only ever changed in [CinemaCarouselRow].
 */
fun LazyListScope.cinemaCarouselSection(
    repository: CinemaRepository,
    title: String,
    items: List<CinemaCardData>,
    emptyLabel: String,
    onItemClick: (CinemaCardData) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    posterSize: Pair<Dp, Dp> = DefaultCarouselPosterSize,
    sectionHorizontalPadding: Dp = 4.dp,
) {
    item {
        SectionTitle(
            title = title,
            modifier = Modifier.padding(horizontal = sectionHorizontalPadding),
            onSeeAllClick = onSeeAllClick,
        )
    }
    if (items.isEmpty()) {
        item {
            EmptySectionHint(emptyLabel, modifier = Modifier.padding(horizontal = sectionHorizontalPadding))
        }
    } else {
        item {
            CinemaCarouselRow(
                repository = repository,
                items = items,
                onItemClick = onItemClick,
                posterSize = posterSize,
                contentPadding = PaddingValues(horizontal = sectionHorizontalPadding)
            )
        }
    }
}

/** Consistent "empty section" message, in a subtle card rather than plain text lost on
 *  the page (visually clearer, consistent with the rest of the app's cards). */
@Composable
private fun EmptySectionHint(text: String, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(extended.chipSurface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
