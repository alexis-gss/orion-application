package com.orion.app.books.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.app.R
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.ui.theme.OrionExtendedColors
import com.orion.app.core.ui.theme.StatNumberStyle
import com.orion.app.books.data.BooksRepository
import com.orion.app.books.data.ReadBook
import java.util.Calendar
import java.util.Locale

/**
 * Equivalent of GamesStatsScreen: headline number, consecutive-day streak, quick
 * monthly stats, breakdown by category, monthly activity — derived only from
 * read_books (status = "read"), no extra network call.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksStatsScreen(repository: BooksRepository, onBack: () -> Unit) {
    val allRead by repository.observeReadBooks().collectAsState(initial = emptyList())
    val read = remember(allRead) { allRead.filter { it.status == "read" } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title), fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (read.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.books_stats_empty),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { BooksHeroStatCard(read) }
            item { BooksQuickStatsGrid(read) }
            item { BooksCategoryBreakdownCard(read) }
            item { BooksMonthlyActivityCard(read) }
        }
    }
}

private fun Modifier.cardBackground(extended: OrionExtendedColors, radius: Int = 16): Modifier = this
    .fillMaxWidth()
    .clip(RoundedCornerShape(radius.dp))
    .background(extended.cardSurface)
    .border(1.dp, extended.cardBorder, RoundedCornerShape(radius.dp))
    .padding(16.dp)

private fun longestDailyStreak(items: List<ReadBook>): Int {
    val days = items.map { w ->
        val c = Calendar.getInstance().apply { timeInMillis = w.updatedAt }
        c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }.toSortedSet()
    var best = 0
    var current = 0
    var previous = Int.MIN_VALUE
    days.forEach { day ->
        current = if (day == previous + 1) current + 1 else 1
        best = maxOf(best, current)
        previous = day
    }
    return best
}

@Composable
private fun BooksHeroStatCard(items: List<ReadBook>) {
    val extended = OrionColors.colors
    val streak = remember(items) { longestDailyStreak(items) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)))
            .padding(20.dp)
    ) {
        Column {
            Text(text = items.size.toString(), style = StatNumberStyle.copy(fontSize = 44.sp), color = extended.badgeText)
            Text(
                text = stringResource(R.string.books_stats_total_read),
                style = MaterialTheme.typography.titleSmall,
                color = extended.badgeText.copy(alpha = 0.85f)
            )
            if (streak >= 2) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.12f)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = extended.badgeText, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        stringResource(R.string.games_streak_days, streak),
                        style = MaterialTheme.typography.labelSmall,
                        color = extended.badgeText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class QuickStat(val value: String, val label: String)

@Composable
private fun BooksQuickStatsGrid(items: List<ReadBook>) {
    val thisMonth = remember(items) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        items.count {
            cal.timeInMillis = it.updatedAt
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }
    val byMonth = remember(items) {
        items.groupBy { w ->
            val c = Calendar.getInstance().apply { timeInMillis = w.updatedAt }
            c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH)
        }
    }
    val average = remember(byMonth, items) {
        val monthsSpanned = byMonth.keys.size.coerceAtLeast(1)
        String.format(Locale.US, "%.1f", items.size.toFloat() / monthsSpanned)
    }
    val bestMonthCount = remember(byMonth) { byMonth.maxOfOrNull { it.value.size } ?: 0 }
    val totalPages = remember(items) { items.sumOf { it.pageCount ?: 0 } }

    val pagesReadLabel = stringResource(R.string.books_stats_pages_read)
    val bestMonthLabel = stringResource(R.string.stats_best_month)
    val stats = listOf(
        QuickStat(thisMonth.toString(), stringResource(R.string.stats_this_month)),
        QuickStat(average, stringResource(R.string.stats_average_per_month)),
        QuickStat(if (totalPages > 0) totalPages.toString() else bestMonthCount.toString(), if (totalPages > 0) pagesReadLabel else bestMonthLabel),
    )

    Column {
        SectionTitle(stringResource(R.string.stats_in_brief))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.forEach { stat -> BooksQuickStatCard(stat, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun BooksQuickStatCard(stat: QuickStat, modifier: Modifier = Modifier) {
    val extended = OrionColors.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(extended.cardSurface)
            .border(1.dp, extended.cardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stat.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(2.dp))
        Text(stat.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BooksCategoryBreakdownCard(items: List<ReadBook>) {
    val extended = OrionColors.colors
    val categoryCounts = remember(items) {
        items.asSequence()
            .flatMap { it.categories?.split(",").orEmpty().asSequence() }
            .map { it.trim().substringBefore(" / ") }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)
    }

    Column {
        SectionTitle(stringResource(R.string.books_favorite_categories))
        Box(modifier = Modifier.cardBackground(extended)) {
            if (categoryCounts.isEmpty()) {
                Text(stringResource(R.string.books_stats_no_category_data), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val max = categoryCounts.first().value
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryCounts.forEachIndexed { index, entry -> BooksCategoryBar(entry.key, entry.value, max, index) }
                }
            }
        }
    }
}

@Composable
private fun BooksCategoryBar(category: String, count: Int, max: Int, rank: Int) {
    val extended = OrionColors.colors
    val barGradient = if (rank == 0) Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer))
        else Brush.horizontalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = (0.85f - rank * 0.1f).coerceAtLeast(0.35f)),
            MaterialTheme.colorScheme.primary.copy(alpha = (0.85f - rank * 0.1f).coerceAtLeast(0.35f))
        ))
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(count.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(extended.chipSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (count.toFloat() / max).coerceIn(0.04f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(barGradient)
            )
        }
    }
}

@Composable
private fun BooksMonthlyActivityCard(items: List<ReadBook>) {
    val extended = OrionColors.colors
    // Month names always rendered in English, decoupled from the (English) app locale — see
    // DateUtils's class doc for the app's date-formatting policy.
    val locale = Locale.US
    val monthly = remember(items) {
        val cal = Calendar.getInstance()
        val now = Calendar.getInstance()
        (5 downTo 0).map { offset ->
            cal.timeInMillis = now.timeInMillis
            cal.add(Calendar.MONTH, -offset)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val label = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale).orEmpty().replaceFirstChar { it.uppercase(locale) }
            val count = items.count {
                val c = Calendar.getInstance().apply { timeInMillis = it.updatedAt }
                c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
            }
            label to count
        }
    }
    val max = monthly.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Column {
        SectionTitle(stringResource(R.string.stats_activity_6months))
        Box(modifier = Modifier.cardBackground(extended)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                monthly.forEachIndexed { idx, pair ->
                    val (label, count) = pair
                    val isLast = idx == monthly.lastIndex
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(text = count.toString(), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((90 * count.toFloat() / max).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isLast) Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer))
                                    else Brush.verticalGradient(listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    ))
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
