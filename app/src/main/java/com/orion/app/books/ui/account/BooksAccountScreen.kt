package com.orion.app.books.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.ui.components.AppTopBar
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.util.DateUtils
import com.orion.app.books.data.BooksRepository
import com.orion.app.books.ui.components.BookCardData
import com.orion.app.books.ui.components.bookCarouselSection
import com.orion.app.books.ui.components.toCardData
import java.util.Calendar

/** Books Account page: 3 quick stat cards (clickable through to the dedicated page),
 * favorites, and read books. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksAccountScreen(
    repository: BooksRepository,
    onOpenItem: (String) -> Unit,
    onOpenStats: () -> Unit,
    onSeeAll: (String, List<BookCardData>) -> Unit,
    onOpenMenu: () -> Unit = {}
) {
    val favorites by repository.observeFavorites().collectAsState(initial = emptyList())
    val favoritesCards = remember(favorites) {
        favorites.map { it.toCardData(dateLabel = DateUtils.formatShortDate(it.addedAt)) }
    }

    val readBooks by repository.observeReadBooks().collectAsState(initial = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    val readCards = remember(readBooks) { readBooks.filter { it.status == "read" }.map { it.toCardData(context) } }

    val favoritesTitle = stringResource(R.string.favorites_title)
    val favoritesEmpty = stringResource(R.string.games_favorites_empty)
    val readTitle = stringResource(R.string.book_read_title)
    val readEmpty = stringResource(R.string.book_read_empty)

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_account),
                onOpenMenu = onOpenMenu
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 95.dp)
        ) {
            item { BooksStatsSection(readBooks, favoritesCards.size, onOpenStats = onOpenStats) }
            bookCarouselSection(
                title = favoritesTitle,
                items = favoritesCards.take(15),
                emptyLabel = favoritesEmpty,
                onItemClick = { onOpenItem(it.volumeId) },
                onSeeAllClick = { onSeeAll(favoritesTitle, favoritesCards) },
                sectionHorizontalPadding = 16.dp,
            )
            item { Spacer(Modifier.height(8.dp)) }
            bookCarouselSection(
                title = readTitle,
                items = readCards.take(15),
                emptyLabel = readEmpty,
                onItemClick = { onOpenItem(it.volumeId) },
                onSeeAllClick = { onSeeAll(readTitle, readCards) },
                sectionHorizontalPadding = 16.dp,
            )
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ---------------- 3 quick stat cards (same principle as cinema/games) ----------------

private data class BookStatItem(val value: Int, val label: String, val accent: Boolean = false)

@Composable
private fun BooksStatsSection(readBooks: List<com.orion.app.books.data.ReadBook>, favoritesCount: Int, onOpenStats: () -> Unit) {
    val readCount = remember(readBooks) { readBooks.count { it.status == "read" } }
    val thisMonth = remember(readBooks) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        readBooks.count {
            it.status == "read" &&
                run {
                    cal.timeInMillis = it.updatedAt
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
        }
    }

    val stats = listOf(
        BookStatItem(readCount, stringResource(R.string.book_read_title)),
        BookStatItem(favoritesCount, stringResource(R.string.favorites_title)),
        BookStatItem(thisMonth, stringResource(R.string.stats_this_month), accent = true)
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle(stringResource(R.string.stats_title))
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenStats),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            stats.forEach { stat -> BooksStatCard(stat, modifier = Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BooksStatCard(stat: BookStatItem, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (stat.accent) {
                    Modifier.background(Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)))
                } else {
                    Modifier.background(extended.cardSurface)
                }
            )
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stat.value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (stat.accent) extended.badgeText else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stat.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (stat.accent) extended.badgeText.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
