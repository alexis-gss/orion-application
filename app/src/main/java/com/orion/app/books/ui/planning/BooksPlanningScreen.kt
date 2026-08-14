package com.orion.app.books.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orion.app.books.data.BooksRepository
import com.orion.app.books.data.FollowedBook
import com.orion.app.books.ui.components.BookItemRow
import com.orion.app.books.ui.components.toCardData
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.util.DateUtils
import com.orion.app.R
import kotlinx.coroutines.launch

/**
 * Epoch seconds -> "yyyy-MM-dd" (Europe/Paris time zone) to reuse DateUtils as-is.
 *
 * java.text.SimpleDateFormat rather than java.time.Instant/ZoneId: java.time only works
 * natively from Android 8.0 (API 26) without "core library desugaring" enabled on the
 * build.gradle side — see the same note in HardcoverApi.kt/BooksRepository.kt.
 */
private fun Long.toIsoDate(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    formatter.timeZone = java.util.TimeZone.getTimeZone("Europe/Paris")
    return formatter.format(java.util.Date(this * 1000L))
}

/** Equivalent of GamesPlanningScreen: followed books whose publication isn't yet
 * past, grouped by release date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksPlanningScreen(repository: BooksRepository, onOpenItem: (String) -> Unit, onOpenMenu: () -> Unit = {}) {
    val allFollowed by repository.observeFollowed().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val upcoming = remember(allFollowed) {
        allFollowed.filter { !it.isReleased }
            .sortedWith(compareBy({ it.releaseTimestamp == null }, { it.releaseTimestamp }))
    }

    LaunchedEffect(allFollowed.map { it.volumeId }) {
        allFollowed.forEach { item -> repository.refreshFollowedIfStale(item) }
    }

    fun refreshAllManually() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            try {
                allFollowed.forEach { item -> repository.refreshFollowedIfStale(item, force = true) }
            } finally {
                isRefreshing = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                onOpenMenu = onOpenMenu,
                title = stringResource(R.string.nav_planning),
                actions = {
                    IconButton(onClick = { refreshAllManually() }, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (upcoming.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.book_planning_empty),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                val groups: List<Pair<Long?, List<FollowedBook>>> = remember(upcoming) {
                    val result = mutableListOf<Pair<Long?, MutableList<FollowedBook>>>()
                    upcoming.forEach { book ->
                        val last = result.lastOrNull()
                        if (last != null && last.first == book.releaseTimestamp) {
                            last.second.add(book)
                        } else {
                            result.add(book.releaseTimestamp to mutableListOf(book))
                        }
                    }
                    result
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 95.dp),
                ) {
                    groups.forEach { (timestamp, groupItems) ->
                        item(key = "header_${timestamp ?: "unknown"}") {
                            SectionTitle(
                                if (timestamp == null) stringResource(R.string.date_unknown) else DateUtils.formatDay(context, timestamp.toIsoDate())
                            )
                        }
                        items(groupItems, key = { it.volumeId }) { book ->
                            Row(Modifier.padding(bottom = 8.dp)) {
                                BookItemRow(
                                    item = book.toCardData(context),
                                    onClick = { onOpenItem(book.volumeId) },
                                    trailingContent = {
                                        val isoDate = book.releaseTimestamp?.toIsoDate()
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            OrionColors.colors.navBarSelectedContainerAlt,
                                                            OrionColors.colors.navBarSelectedContainer
                                                        )
                                                    )
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = DateUtils.daysRemainingLabel(context, isoDate),
                                                color = OrionColors.colors.navBarSelectedIcon,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}