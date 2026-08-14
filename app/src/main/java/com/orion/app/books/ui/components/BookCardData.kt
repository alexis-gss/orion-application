package com.orion.app.books.ui.components

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.orion.app.core.ui.components.PosterImage
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.ui.theme.colorForRating
import java.util.Locale

/**
 * Books equivalent of GameCardData/MediaCardData: generic display model for a book,
 * independent of the source (ReadBook, FollowedBook, HardcoverBook...). Intentionally
 * duplicated, like for games: no shared Repository between the domains.
 */
data class BookCardData(
    val key: String,
    val volumeId: String,
    val title: String,
    val coverUrl: String?,
    val subtitle: String? = null,
    val trailingText: String? = null,
    /** Hardcover rating out of 5, converted to out of 10 for consistent display. */
    val rating: Double? = null,
    /** Comma-separated Hardcover categories (generic segment before " / "),
     *  same convention as BooksStatsScreen, for SeeAllScreen's genre filter. */
    val genres: String? = null,
    /** Release date in epoch millis, for SeeAllScreen's release-date filter. */
    val releaseDate: Long? = null,
    /** Epoch-millis timestamp of when it was added (followed/read/favorited), for the added-date filter. */
    val addedAt: Long? = null
)

private val CoverShape = RoundedCornerShape(12.dp)
private val DefaultRowCoverSize: Pair<Dp, Dp> = 64.dp to 92.dp
private val DefaultCarouselCoverSize: Pair<Dp, Dp> = 130.dp to 192.dp

/**
 * Placeholder cover shown when Hardcover provides no cover (a field that's often absent,
 * a frequent case on this domain unlike TMDB/IGDB) or when the image fails to load.
 * Gradient + book icon, rather than an empty space that would look like a loading bug.
 */
@Composable
fun BookCoverPlaceholder(modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = extended.badgeText.copy(alpha = 0.85f),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun BookCover(
    coverUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    PosterImage(
        model = coverUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        fallback = { BookCoverPlaceholder() },
    )
}

/** LAYOUT 1 — one row per book: cover + title + subtitle. */
@Composable
fun BookItemRow(
    item: BookCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverSize: Pair<Dp, Dp> = DefaultRowCoverSize,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val extended = OrionColors.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = CoverShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.25f))
            .clip(CoverShape)
            .background(extended.cardSurface)
            .border(1.dp, extended.cardBorder, CoverShape)
            .clickable(onClick = onClick)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookCover(
            coverUrl = item.coverUrl,
            contentDescription = item.title,
            modifier = Modifier.size(coverSize.first, coverSize.second)
        )
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
                BookRatingBadge(rating = rating)
            }
        }
        when {
            trailingContent != null -> {
                Spacer(Modifier.width(10.dp))
                trailingContent()
            }
            item.trailingText != null -> {
                Spacer(Modifier.width(10.dp))
                BookBadge(text = item.trailingText)
            }
        }
    }
}

@Composable
fun BookBadge(text: String, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = extended.badgeText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BookRatingBadge(rating: Double) {
    val extended = OrionColors.colors
    val color = extended.colorForRating(rating)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(String.format(Locale.US, "%.1f", rating), fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

/** LAYOUT 2 — horizontal cover carousel. */
@Composable
fun BookCarouselRow(
    items: List<BookCardData>,
    onItemClick: (BookCardData) -> Unit,
    modifier: Modifier = Modifier,
    coverSize: Pair<Dp, Dp> = DefaultCarouselCoverSize,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp),
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = contentPadding,
    ) {
        items(items, key = { it.key }) { item ->
            BookCoverCard(item = item, coverSize = coverSize, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun BookCoverCard(item: BookCardData, coverSize: Pair<Dp, Dp>, onClick: () -> Unit) {
    val extended = OrionColors.colors
    Box(
        modifier = Modifier
            .size(coverSize.first, coverSize.second)
            .shadow(elevation = 10.dp, shape = CoverShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.35f))
            .clip(CoverShape)
            .clickable(onClick = onClick)
    ) {
        BookCover(coverUrl = item.coverUrl, contentDescription = item.title, modifier = Modifier.fillMaxSize())
        item.trailingText?.let { badge ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(colors = listOf(extended.posterOverlayBottom, extended.posterOverlayTop))
                    )
            )
            BookBadge(text = badge, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
    }
}

/**
 * "Series" tab: every volume of the viewed book's saga, shown as a vertical list like the
 * "Same franchise" tab on the games side (franchiseTabContent), rather than a
 * carousel/section with its own title — the TabRow above already carries the "Series"
 * label, a second title would be redundant.
 */
fun LazyListScope.seriesTabContent(
    items: List<BookCardData>,
    onItemClick: (BookCardData) -> Unit,
    emptyLabel: String,
) {
    if (items.isEmpty()) {
        item {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }
    items(items, key = { it.key }) { data ->
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            BookItemRow(
                item = data,
                onClick = { onItemClick(data) },
            )
        }
    }
    item { Spacer(modifier = Modifier.height(8.dp)) }
}

fun LazyListScope.bookRowSection(
    title: String,
    items: List<BookCardData>,
    emptyLabel: String,
    onItemClick: (BookCardData) -> Unit,
    trailingContent: (@Composable (BookCardData) -> Unit)? = null,
    onSeeAllClick: (() -> Unit)? = null,
) {
    item { com.orion.app.core.ui.components.SectionTitle(title = title, onSeeAllClick = onSeeAllClick) }
    if (items.isEmpty()) {
        item { EmptyBookSectionHint(emptyLabel) }
    } else {
        items(items, key = { it.key }) { data ->
            BookItemRow(
                item = data,
                onClick = { onItemClick(data) },
                trailingContent = trailingContent?.let { render -> { render(data) } }
            )
        }
    }
}

fun LazyListScope.bookCarouselSection(
    title: String,
    items: List<BookCardData>,
    emptyLabel: String,
    onItemClick: (BookCardData) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    coverSize: Pair<Dp, Dp> = DefaultCarouselCoverSize,
    sectionHorizontalPadding: Dp = 4.dp,
) {
    item {
        com.orion.app.core.ui.components.SectionTitle(
            title = title,
            modifier = Modifier.padding(horizontal = sectionHorizontalPadding),
            onSeeAllClick = onSeeAllClick,
        )
    }
    if (items.isEmpty()) {
        item { EmptyBookSectionHint(emptyLabel, modifier = Modifier.padding(horizontal = sectionHorizontalPadding)) }
    } else {
        item {
            BookCarouselRow(
                items = items,
                onItemClick = onItemClick,
                coverSize = coverSize,
                contentPadding = PaddingValues(horizontal = sectionHorizontalPadding)
            )
        }
    }
}

@Composable
private fun EmptyBookSectionHint(text: String, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(extended.chipSurface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
