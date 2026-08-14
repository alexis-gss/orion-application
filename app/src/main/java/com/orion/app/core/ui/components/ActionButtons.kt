package com.orion.app.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.orion.app.R

/**
 * SHARED COMPONENT — used by cinema, games and books detail screens.
 *
 * Row of action chips (follow/watched/favorite) plus up to two round icon buttons on the
 * trailing edge: [onTrailerAction] (cinema: opens the YouTube trailer) and [onExtraAction]
 * (books: opens the in-app preview).
 *
 * FIX (chip-wrap bug): the previous implementation decided between "equal-weight full width"
 * and "natural width + scroll" by comparing the SUM of the chips' natural widths against the
 * available width. That check is not sufficient: even when the sum fits, splitting the space
 * EQUALLY between chips of very different natural widths (e.g. "Read" vs "Favorites" on the
 * books screen) can starve the widest chip below what it needs, causing its label to wrap
 * onto two lines instead of the row scrolling. The fix measures each chip's natural width
 * INDIVIDUALLY and only switches to the equal-weight full-width mode when the WIDEST chip
 * would still fit within its equal share of the available space; otherwise it falls back to
 * natural widths + horizontal scroll, which is always safe.
 */
@Composable
fun ActionButtons(
    isFollowed: Boolean,
    followDisabled: Boolean = false,
    onFollowToggle: () -> Unit,
    followLabel: String? = null,
    followedLabel: String? = null,
    isWatched: Boolean? = null,
    onWatchedToggle: (() -> Unit)? = null,
    watchedDisabled: Boolean = false,
    watchedLabel: String? = null,
    notWatchedLabel: String? = null,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    favoriteDisabled: Boolean = false,
    favoriteLabel: String? = null,
    favoritesLabel: String? = null,
    /** Cinema: opens the trailer (YouTube) when non-null. Null hides the button entirely. */
    onTrailerAction: (() -> Unit)? = null,
    /** Books: opens the in-app preview when non-null. Null hides the button entirely. */
    onExtraAction: (() -> Unit)? = null,
) {
    val minChipWidth = 85.dp
    val itemSpacing = 8.dp
    val edgeSpacing = 8.dp

    SubcomposeLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 8.dp)
    ) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        val itemSpacingPx = itemSpacing.roundToPx()
        val edgeSpacingPx = edgeSpacing.roundToPx()

        // --- Step 1: measure each CHIP individually (natural, unweighted width) ---
        // Order matches the visual order in ActionButtonsRow: Follow, Watched?, Favorite?
        // FollowChip/WatchedChip/FavoriteChip are RowScope extensions (they call
        // Modifier.weight()), so this slot list must be built and invoked from inside an
        // actual Row — a bare subcompose lambda has no RowScope receiver on its own.
        val chipSlots = buildList<@Composable RowScope.() -> Unit> {
            add {
                FollowChip(
                    isFollowed = isFollowed,
                    followDisabled = followDisabled,
                    onFollowToggle = onFollowToggle,
                    followLabel = followLabel,
                    followedLabel = followedLabel,
                    minChipWidth = minChipWidth,
                    useWeight = false,
                )
            }
            if (isWatched != null && onWatchedToggle != null) {
                add {
                    WatchedChip(
                        isWatched = isWatched,
                        watchedDisabled = watchedDisabled,
                        onWatchedToggle = onWatchedToggle,
                        watchedLabel = watchedLabel,
                        notWatchedLabel = notWatchedLabel,
                        minChipWidth = minChipWidth,
                        useWeight = false,
                    )
                }
            }
            if (onFavoriteToggle != null) {
                add {
                    FavoriteChip(
                        isFavorite = isFavorite,
                        onFavoriteToggle = onFavoriteToggle,
                        favoriteDisabled = favoriteDisabled,
                        favoriteLabel = favoriteLabel,
                        favoritesLabel = favoritesLabel,
                        minChipWidth = minChipWidth,
                        useWeight = false,
                    )
                }
            }
        }

        // Each chip is subcomposed as its OWN single-child Row so every measured
        // Placeable's width is exactly that one chip's natural (unweighted) width,
        // with no interference between chips and no leftover Row padding/arrangement
        // affecting the measurement.
        val chipNaturalWidths = chipSlots.mapIndexed { index, slot ->
            subcompose("chip_$index") {
                Row { slot() }
            }.first().measure(looseConstraints).width
        }

        val chipCount = chipNaturalWidths.size

        // --- Step 2: measure the round trailing buttons (trailer/extra), not weighted ---
        val roundButtonSlots = buildList<@Composable () -> Unit> {
            onTrailerAction?.let { action -> add { TrailerButton(action) } }
            onExtraAction?.let { action -> add { ExtraButton(action) } }
        }
        val roundButtonsWidth = subcompose("measureRound") {
            roundButtonSlots.forEach { it() }
        }.sumOf { it.measure(looseConstraints).width }

        // --- Step 3: total spacing (edge + edge + spacedBy between every visible item) ---
        val totalItemCount = chipCount + roundButtonSlots.size
        val spacingTotal = edgeSpacingPx * 2 +
                (if (totalItemCount > 1) itemSpacingPx * (totalItemCount - 1) else 0)

        val availableForChips = (constraints.maxWidth - roundButtonsWidth - spacingTotal)
            .coerceAtLeast(0)
        val equalShare = if (chipCount > 0) availableForChips / chipCount else 0
        val maxChipNaturalWidth = chipNaturalWidths.maxOrNull() ?: 0

        // THE FIX: only use equal-weight full-width mode if the WIDEST chip's natural
        // width still fits within the equal share it would receive. Comparing the sum
        // instead (as before) can pass while still starving the widest chip.
        val fits = chipCount == 0 || maxChipNaturalWidth <= equalShare

        // --- Step 4: final layout pass ---
        val placeable = subcompose("final") {
            ActionButtonsRow(
                minChipWidth = minChipWidth,
                useWeight = fits,
                scrollable = !fits,
                matchWidth = true,
                isFollowed = isFollowed, followDisabled = followDisabled, onFollowToggle = onFollowToggle,
                followLabel = followLabel, followedLabel = followedLabel,
                isWatched = isWatched, onWatchedToggle = onWatchedToggle, watchedDisabled = watchedDisabled,
                watchedLabel = watchedLabel, notWatchedLabel = notWatchedLabel,
                isFavorite = isFavorite, onFavoriteToggle = onFavoriteToggle, favoriteDisabled = favoriteDisabled,
                favoriteLabel = favoriteLabel, favoritesLabel = favoritesLabel,
                onTrailerAction = onTrailerAction, onExtraAction = onExtraAction,
            )
        }.first().measure(constraints)

        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
}

/**
 * The actual row of action buttons. [useWeight] makes each FilterChip share the
 * available width equally (occupying the whole screen width, no scroll); [scrollable]
 * adds horizontal scroll for the case where the buttons overflow at their natural width,
 * OR where equal division would starve the widest chip (see [ActionButtons] fix notes).
 * The two are never active at the same time.
 */
@Composable
private fun ActionButtonsRow(
    minChipWidth: Dp,
    useWeight: Boolean,
    scrollable: Boolean,
    matchWidth: Boolean,
    isFollowed: Boolean,
    followDisabled: Boolean,
    onFollowToggle: () -> Unit,
    followLabel: String?,
    followedLabel: String?,
    isWatched: Boolean?,
    onWatchedToggle: (() -> Unit)?,
    watchedDisabled: Boolean,
    watchedLabel: String?,
    notWatchedLabel: String?,
    isFavorite: Boolean,
    onFavoriteToggle: (() -> Unit)?,
    favoriteDisabled: Boolean,
    favoriteLabel: String?,
    favoritesLabel: String?,
    onTrailerAction: (() -> Unit)?,
    onExtraAction: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .let { if (matchWidth) it.fillMaxWidth() else it }
            .let { if (scrollable) it.horizontalScroll(rememberScrollState()) else it },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))

        FollowChip(
            isFollowed = isFollowed,
            followDisabled = followDisabled,
            onFollowToggle = onFollowToggle,
            followLabel = followLabel,
            followedLabel = followedLabel,
            minChipWidth = minChipWidth,
            useWeight = useWeight,
        )

        if (isWatched != null && onWatchedToggle != null) {
            WatchedChip(
                isWatched = isWatched,
                watchedDisabled = watchedDisabled,
                onWatchedToggle = onWatchedToggle,
                watchedLabel = watchedLabel,
                notWatchedLabel = notWatchedLabel,
                minChipWidth = minChipWidth,
                useWeight = useWeight,
            )
        }

        if (onFavoriteToggle != null) {
            FavoriteChip(
                isFavorite = isFavorite,
                onFavoriteToggle = onFavoriteToggle,
                favoriteDisabled = favoriteDisabled,
                favoriteLabel = favoriteLabel,
                favoritesLabel = favoritesLabel,
                minChipWidth = minChipWidth,
                useWeight = useWeight,
            )
        }

        onTrailerAction?.let { action -> TrailerButton(action) }
        onExtraAction?.let { action -> ExtraButton(action) }

        Spacer(Modifier.width(8.dp))
    }
}

// ---------------------------------------------------------------------------------------
// Individual pieces, extracted so they can be measured in isolation (see step 1 above)
// as well as reused inside the real row (step 4), guaranteeing both passes render the
// exact same content and therefore the exact same natural widths.
// ---------------------------------------------------------------------------------------

@Composable
private fun RowScope.FollowChip(
    isFollowed: Boolean,
    followDisabled: Boolean,
    onFollowToggle: () -> Unit,
    followLabel: String?,
    followedLabel: String?,
    minChipWidth: Dp,
    useWeight: Boolean,
) {
    FilterChip(
        selected = isFollowed,
        enabled = !followDisabled,
        onClick = onFollowToggle,
        modifier = Modifier
            .let { if (useWeight) it.weight(1f) else it }
            .defaultMinSize(minWidth = minChipWidth),
        shape = RoundedCornerShape(12.dp),
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isFollowed) Icons.Filled.Bookmark else Icons.Filled.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isFollowed) (followedLabel ?: stringResource(R.string.action_followed)) else (followLabel ?: stringResource(R.string.action_follow)))
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
    )
}

@Composable
private fun RowScope.WatchedChip(
    isWatched: Boolean,
    watchedDisabled: Boolean,
    onWatchedToggle: () -> Unit,
    watchedLabel: String?,
    notWatchedLabel: String?,
    minChipWidth: Dp,
    useWeight: Boolean,
) {
    FilterChip(
        selected = isWatched,
        enabled = !watchedDisabled,
        onClick = onWatchedToggle,
        modifier = Modifier
            .let { if (useWeight) it.weight(1f) else it }
            .defaultMinSize(minWidth = minChipWidth),
        shape = RoundedCornerShape(12.dp),
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isWatched) (watchedLabel ?: stringResource(R.string.action_watched)) else (notWatchedLabel ?: stringResource(R.string.action_not_watched)))
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
    )
}

@Composable
private fun RowScope.FavoriteChip(
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    favoriteDisabled: Boolean,
    favoriteLabel: String?,
    favoritesLabel: String?,
    minChipWidth: Dp,
    useWeight: Boolean,
) {
    FilterChip(
        selected = isFavorite,
        enabled = !favoriteDisabled,
        onClick = onFavoriteToggle,
        modifier = Modifier
            .let { if (useWeight) it.weight(1f) else it }
            .defaultMinSize(minWidth = minChipWidth),
        shape = RoundedCornerShape(12.dp),
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isFavorite) (favoriteLabel ?: stringResource(R.string.action_favorite)) else (favoritesLabel ?: stringResource(R.string.action_favorites)))
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
    )
}

@Composable
private fun TrailerButton(action: () -> Unit) {
    FilledTonalIconButton(
        onClick = action,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(R.string.action_trailer_description),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun ExtraButton(action: () -> Unit) {
    FilledTonalIconButton(
        onClick = action,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(R.string.action_preview_description),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}