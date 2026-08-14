package com.orion.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orion.app.core.ui.theme.OrionColors

data class PillTabItem(
    val label: String,
    val icon: ImageVector? = null,
)

/**
 * Pill-style tab selector: same visual language as the filter chips
 * (AdvancedFilterBar/InlineSearchFilterBar) rather than the default Material TabRow, whose
 * plain underline is barely visible with only a few short tabs. The overall background
 * reuses chipSurface, the selected pill reuses badgeBackground/badgeText — consistent with
 * the rest of the app, and automatically re-tinted based on the active domain
 * (cinema yellow / games purple / books blue, see OrionTheme).
 */
@Composable
fun PillTabRow(
    items: List<PillTabItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = OrionColors.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(extended.chipSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val backgroundColor by animateColorAsState(
                targetValue = if (selected) extended.badgeBackground else extended.chipSurface.copy(alpha = 0f),
                label = "pillTabBackground"
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) extended.badgeText else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "pillTabContent"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(backgroundColor)
                    .clickable { onSelected(index) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tab.icon?.let {
                    Icon(it, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = tab.label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
