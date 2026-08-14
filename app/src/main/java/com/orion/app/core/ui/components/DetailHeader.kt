package com.orion.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.ui.theme.colorForRating
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * SHARED COMPONENT — used by cinema, games and books detail screens (moved here from
 * cinema/ui/detail/components, its original, domain-specific location, to make the sharing
 * explicit and avoid the other two domains importing "into" the cinema package).
 *
 * DetailHeader is shared between cinema (TMDB, relative paths like "/abc.jpg"), video
 * games (IGDB, already-absolute URLs), and books (Hardcover, already-absolute URLs).
 * Only relative paths are therefore prefixed.
 */
private fun String.toFullImageUrl(size: String): String =
    if (startsWith("http://") || startsWith("https://")) this else "https://image.tmdb.org/t/p/$size$this"

/**
 * Detail page header: a banner (backdrop) in the background, with the poster
 * overlapping it at the bottom left, Netflix / TMDB app-style. Below the title block, a
 * quick-info row (rating, year, runtime/seasons) and genres as chips.
 *
 * [backdropPath] may be null (books have no backdrop concept in the Hardcover API):
 * the banner box then just shows the background gradient with no image behind it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailHeader(
    backdropPath: String?,
    posterPath: String?,
    title: String,
    tagline: String? = null,
    voteAverage: Double? = null,
    infoLine: String? = null,
    genres: List<String> = emptyList(),
    /** Optional slot shown in place of the poster when [posterPath] is absent/errors out (see PosterImage.fallback). Defaults to null, with no impact on cinema/games. */
    posterFallback: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            // Tall banner (Backdrop)
            PosterImage(
                model = backdropPath?.toFullImageUrl("w780"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )

            // Gradient background to fade the banner into the screen's background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // Books domain (no Hardcover banner): without an image behind it,
            // FloatingTopBar's white back arrow (see core/ui/components) can become
            // unreadable on a light background. A dark scrim is added at the top,
            // independent from the background gradient above, which only fades downward.
            if (backdropPath == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                )
            }

            // Poster overlapping the banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .width(110.dp)
                        .height(160.dp)
                        .offset(y = (-15).dp)
                ) {
                    PosterImage(
                        model = posterPath?.toFullImageUrl("w500"),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        fallback = posterFallback,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Bottom)
                        .padding(bottom = 22.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    tagline?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = "\"$it\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Quick-info row: TMDB/IGDB/Hardcover rating + year / runtime / seasons...
        if (voteAverage != null || infoLine != null) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
            ) {
                if (voteAverage != null && voteAverage > 0) {
                    val extended = OrionColors.colors
                    Row {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = extended.colorForRating(voteAverage),
                            modifier = Modifier.height(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            // Locale.US forced: a decimal point ("7.5") is always wanted, not
                            // a comma (lint: DefaultLocale — the separator otherwise depends on
                            // the device's locale and can break the expected format).
                            text = String.format(Locale.US, "%.1f", voteAverage) + "/10",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (infoLine != null) {
                            Text(
                                text = " · ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                infoLine?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Genres
        if (genres.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                genres.forEachIndexed { index, genre ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(genre, style = MaterialTheme.typography.labelMedium) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = null
                    )
                    if (index != genres.lastIndex) Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}
