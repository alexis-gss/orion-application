package com.orion.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.orion.app.R
import com.orion.app.core.ui.theme.OrionColors

/** Domains available from the home screen. */
enum class AppUniverse { CINEMA, GAMES, BOOKS }

// Brand colors of the 3 domains: the exact gradient already used throughout each
// domain (selected nav bar, stats bars, account header...), namely
// Brush.horizontalGradient(navBarSelectedContainerAlt -> navBarSelectedContainer) in
// Theme.kt, rather than a plain pair of colors invented for this screen.
// Made `internal` (not `private`) so the sidebar (AppSidebarContent) can reuse the
// exact same palette/icons instead of duplicating them.
internal val CinemaGradient = listOf(Color(0xFFFFDB74), Color(0xFFFFC93C))
internal val GamesGradient = listOf(Color(0xFFB58CFF), Color(0xFF9147FF))
internal val BooksGradient = listOf(Color(0xFF5B8DFF), Color(0xFF2258D3))

/** Icon shown on this universe's card, shared between the home screen and the sidebar. */
internal fun AppUniverse.icon(): ImageVector = when (this) {
    AppUniverse.CINEMA -> Icons.Filled.Movie
    AppUniverse.GAMES -> Icons.Filled.SportsEsports
    AppUniverse.BOOKS -> Icons.Filled.MenuBook
}

/** Brand gradient of this universe, shared between the home screen and the sidebar. */
internal fun AppUniverse.gradient(): List<Color> = when (this) {
    AppUniverse.CINEMA -> CinemaGradient
    AppUniverse.GAMES -> GamesGradient
    AppUniverse.BOOKS -> BooksGradient
}

/** Tint of the icon glyph itself (dark on the light cinema yellow, white elsewhere). */
internal fun AppUniverse.iconTint(): Color = when (this) {
    AppUniverse.CINEMA -> Color(0xFF241A00)
    AppUniverse.GAMES -> Color.White
    AppUniverse.BOOKS -> Color.White
}

@Composable
internal fun AppUniverse.title(): String = stringResource(
    when (this) {
        AppUniverse.CINEMA -> R.string.domain_cinema
        AppUniverse.GAMES -> R.string.domain_games
        AppUniverse.BOOKS -> R.string.domain_books
    }
)

@Composable
internal fun AppUniverse.subtitle(): String = stringResource(
    when (this) {
        AppUniverse.CINEMA -> R.string.home_cinema_subtitle
        AppUniverse.GAMES -> R.string.home_games_subtitle
        AppUniverse.BOOKS -> R.string.home_books_subtitle
    }
)

/**
 * Home screen shown when the app launches: lets the user choose between the Cinema
 * domain (movies/TV shows, TMDB), the Video Games domain (IGDB), and the Books domain
 * (Hardcover), each with its own navigation, its own data, and its own API key system.
 * A Settings button gives access to the global settings (light/dark theme) shared by
 * all three domains.
 */
@Composable
fun HomeScreen(onSelectUniverse: (AppUniverse) -> Unit, onOpenOptions: () -> Unit = {}) {
    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.10f),
                        bg,
                        bg
                    ),
                    startY = 0f,
                    endY = 900f
                )
            )
    ) {
        IconButton(
            onClick = onOpenOptions,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 16.dp, bottom = 16.dp, start = 16.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = stringResource(R.string.gate_logo_description),
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )

            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))

            AppUniverse.entries.forEachIndexed { index, universe ->
                UniverseCard(
                    title = universe.title(),
                    subtitle = universe.subtitle(),
                    icon = universe.icon(),
                    gradient = universe.gradient(),
                    iconTint = universe.iconTint(),
                    onClick = { onSelectUniverse(universe) }
                )
                if (index != AppUniverse.entries.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * SHARED COMPONENT — used by the home screen and by the sidebar (AppSidebarContent),
 * so switching domains looks and feels the same everywhere in the app instead of the
 * sidebar falling back to a thinner, plainer icon+text row.
 *
 * [selected] highlights the card as "this is where you currently are" (colored border +
 * check icon instead of the chevron) — used by the sidebar only, always false on the
 * home screen since there's no "current" domain there.
 */
@Composable
internal fun UniverseCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    iconTint: Color,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val extended = OrionColors.colors
    val accent = gradient.last()
    val borderColor = if (selected) accent else extended.cardBorder
    val borderWidth = if (selected) 1.5.dp else 1.dp
    val backgroundColor = if (selected) accent.copy(alpha = 0.10f) else extended.cardSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}