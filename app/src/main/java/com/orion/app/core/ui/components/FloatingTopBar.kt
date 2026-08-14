package com.orion.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orion.app.R

/**
 * SHARED COMPONENT — used by cinema, games and books detail screens (moved here from
 * cinema/ui/detail/components, its original, domain-specific location).
 *
 * Floating bar at the top of a detail page: transparent at rest (content scrolls
 * underneath it), it takes on an opaque background and shows the current title once the
 * LazyColumn has scrolled past the header (see [titleScrollThresholdPx]).
 */
@Composable
fun FloatingTopBar(
    listState: LazyListState,
    currentTitle: String?,
    onBack: () -> Unit
) {
    val density = LocalDensity.current
    val titleScrollThresholdPx = remember(density) { with(density) { 170.dp.toPx() } }
    val showTitleInBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > titleScrollThresholdPx)
        }
    }
    val barBackgroundColor by animateColorAsState(
        targetValue = if (showTitleInBar) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "barBackground"
    )
    val barContentColor by animateColorAsState(
        targetValue = if (showTitleInBar) MaterialTheme.colorScheme.onSurface else Color.White,
        label = "barContent"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(barBackgroundColor)
            .statusBarsPadding()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = barContentColor)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }

        AnimatedVisibility(
            visible = showTitleInBar && currentTitle != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = barContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}
