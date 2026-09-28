package com.orion.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orion.app.core.ui.theme.OrionColors

/** A single "N / label" tile, used in the quick-stats row of every domain's Statistics page. */
data class QuickStat(val value: String, val label: String)

/**
 * Generic quick-stats row: a titled row of [QuickStat] tiles, evenly spaced. Extracted so
 * Cinema, Games and Books all render their "in brief" numbers with the same component
 * instead of each domain keeping its own near-identical copy.
 */
@Composable
fun QuickStatsRow(title: String, stats: List<QuickStat>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SectionTitle(title)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEach { stat ->
                QuickStatCard(stat, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun QuickStatCard(stat: QuickStat, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(extended.cardSurface)
            .border(1.dp, extended.cardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stat.value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stat.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** A single labeled bar in a genre/category breakdown card (e.g. "Action — 12"). */
@Composable
fun StatBar(label: String, count: Int, max: Int, rank: Int, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    val fraction = if (max <= 0) 0f else count.toFloat() / max.toFloat()
    val barColor = if (rank == 0) extended.badgeBackground else extended.badgeBackground.copy(alpha = 0.55f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(extended.chipSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            ) {}
        }
    }
}
