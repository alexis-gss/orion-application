package com.orion.app.core.ui.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.ui.AppUniverse
import com.orion.app.core.ui.UniverseCard
import com.orion.app.core.ui.gradient
import com.orion.app.core.ui.icon
import com.orion.app.core.ui.iconTint
import com.orion.app.core.ui.subtitle
import com.orion.app.core.ui.title
import com.orion.app.core.ui.theme.OrionColors

/**
 * Content of the sidebar opened from the hamburger menu: a list of domains to switch
 * between quickly (without going back through the home screen), and access to the
 * global settings at the bottom.
 *
 * Domain entries now reuse UniverseCard, the exact same card used on the home screen
 * (gradient icon badge, title + subtitle, fillMaxWidth), instead of a thin icon+text
 * row — so the sidebar looks like a continuation of the home screen rather than a
 * plainer fallback, and each entry actually fills the drawer's width instead of
 * leaving a lot of unused space around a small icon and label.
 */
@Composable
fun AppSidebarContent(
    currentUniverse: AppUniverse,
    onSelectUniverse: (AppUniverse) -> Unit,
    onOpenOptions: () -> Unit
) {
    val extended = OrionColors.colors
    ModalDrawerSheet {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(extended.chipSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter = painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Orion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppUniverse.entries.forEach { universe ->
                    UniverseCard(
                        title = universe.title(),
                        subtitle = universe.subtitle(),
                        icon = universe.icon(),
                        gradient = universe.gradient(),
                        iconTint = universe.iconTint(),
                        selected = currentUniverse == universe,
                        onClick = { onSelectUniverse(universe) }
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(8.dp))
            SidebarSettingsItem(
                title = stringResource(R.string.settings_title),
                onClick = onOpenOptions,
            )
        }
    }
}

/**
 * Settings entry, kept visually lighter than the domain UniverseCards (no gradient
 * badge, no subtitle) since it's a single global action, not a domain to switch into
 * — but still full-width and generously padded, matching the same "take up real
 * space, no thin row" spirit as the cards above.
 */
@Composable
private fun SidebarSettingsItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    }
}