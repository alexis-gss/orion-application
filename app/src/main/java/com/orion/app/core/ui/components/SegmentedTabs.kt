package com.orion.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.orion.app.core.ui.theme.OrionColors

/**
 * Generic multi-option pill selector (e.g. Light/Dark, Movies/TV shows). All options are
 * shown simultaneously, with the active one highlighted. Extracted from StatsSegmentedTabs
 * (CinemaStatsScreen.kt) to be reusable elsewhere (e.g. SettingsScreen for the theme picker).
 */
@Composable
fun <T> SegmentedTabs(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = OrionColors.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(extended.chipSurface)
            .padding(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (isSelected) {
                            Modifier.background(Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)))
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) extended.badgeText else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}