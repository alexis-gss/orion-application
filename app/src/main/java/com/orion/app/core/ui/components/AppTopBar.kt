package com.orion.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.ui.theme.OrionColors

/**
 * The app's TopAppBar: in place of the logo, a "back to home" button (cinema / video
 * games selection) inside a rounded pill — more useful than a static logo, since each
 * domain has its own internal navigation and no other way back to the domain picker.
 * Large title text, blending with the screen's background for a modern look.
 */
import androidx.compose.material.icons.filled.Menu
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBackToHome: (() -> Unit)? = null,
    onOpenMenu: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    colors: TopAppBarColors? = null
) {
    val extended = OrionColors.colors
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onOpenMenu != null) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(extended.chipSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onOpenMenu, modifier = Modifier.size(34.dp)) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = stringResource(R.string.top_bar_menu_description), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                } else if (onBackToHome != null) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(extended.chipSurface)
                            .then(Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onBackToHome, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.top_bar_home_description),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        actions = actions,
        colors = colors ?: TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}
