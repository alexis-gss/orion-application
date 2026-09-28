package com.orion.app.cinema.ui.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.app.R
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.cinema.data.WatchedItem
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.components.StatBar
import com.orion.app.core.ui.components.QuickStat
import com.orion.app.core.ui.components.QuickStatsRow
import com.orion.app.core.ui.theme.OrionColors
import com.orion.app.core.ui.theme.OrionExtendedColors
import com.orion.app.core.ui.theme.StatNumberStyle
import java.util.Calendar
import java.util.Locale

/**
 * Screen dedicated to detailed watch statistics. Two tabs (Movies / Series) to avoid mixing
 * a movie (1 watch) with an episode (1 watch) in the same aggregates — otherwise a 20-episode
 * series would completely overwhelm movies in the stats. Purely derived from watched_items
 * already observed via Repository.observeWatched(): no network call, no extra query.
 *
 * Layout: a "hero" card with a gold gradient showing the tab's headline number, a grid of
 * quick mini stat cards, then detailed sections (most-watched series, genres, watch time,
 * recent activity, favorite days) presented as cards rather than plain text lost on the page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaStatsScreen(repository: CinemaRepository, onBack: () -> Unit) {
    val watched by repository.observeWatched().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }

    val movies = remember(watched) { watched.filter { it.mediaType == "movie" } }
    val episodes = remember(watched) { watched.filter { it.mediaType == "tv" } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title), fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(
                            R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.background(Color.White)
            )
        }
    ) { padding ->
        if (watched.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.stats_no_data),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return@Scaffold
        }
        Column(Modifier
            .padding(padding)
            .fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    stringResource(R.string.filters_movie),
                    stringResource(R.string.filters_tv)
                ).forEachIndexed { index, tabLabel ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = tabLabel) }
                    )
                }
            }
            AnimatedContent(
                targetState = selectedTab,
                label = "stats-tab",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { tab ->
                val data = if (tab == 0) movies else episodes
                if (data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (tab == 0) stringResource(R.string.stats_no_movies_watched) else stringResource(R.string.stats_no_episodes_watched),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { HeroStatCard(data, isMovieTab = tab == 0) }
                        item { QuickStatsGrid(data) }
                        item { GenreBreakdownCard(data) }
                        item { WatchTimeCard(data) }
                        item { MonthlyActivityCard(data) }
                        item { WeekdayBreakdownCard(data) }
                    }
                }
            }
        }
    }
}

/** Consistent card background (surface + thin border), reused by every detailed
 *  section of this screen to replace the old version's plain text.
 *  A Modifier extension (rather than a top-level function) to allow fluent chaining
 *  et suivre la convention Compose (lint: ModifierFactoryExtensionFunction). */
private fun Modifier.cardBackground(extended: OrionExtendedColors, radius: Int = 16): Modifier = this
    .fillMaxWidth()
    .clip(RoundedCornerShape(radius.dp))
    .background(extended.cardSurface)
    .border(1.dp, extended.cardBorder, RoundedCornerShape(radius.dp))
    .padding(16.dp)

@Composable
private fun HeroStatCard(items: List<WatchedItem>, isMovieTab: Boolean) {
    val extended = OrionColors.colors
    val distinctShows = remember(items, isMovieTab) {
        if (isMovieTab) 0 else items.map { it.tmdbId }.distinct().size
    }
    val streak = remember(items) { longestDailyStreak(items) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = items.size.toString(),
                style = StatNumberStyle.copy(fontSize = 44.sp),
                color = extended.badgeText
            )
            Text(
                text = if (isMovieTab) stringResource(R.string.stats_movies_watched_total) else stringResource(R.string.stats_episodes_watched_total),
                style = MaterialTheme.typography.titleSmall,
                color = extended.badgeText.copy(alpha = 0.85f)
            )
            if (!isMovieTab || streak >= 2) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isMovieTab) {
                        HeroChip(
                            icon = Icons.Filled.Tv,
                            text = stringResource(R.string.stats_shows_count_short, distinctShows),
                            textColor = extended.badgeText
                        )
                    }
                    if (streak >= 2) {
                        HeroChip(
                            icon = Icons.Filled.LocalFireDepartment,
                            text = stringResource(R.string.stats_streak_days, streak),
                            textColor = extended.badgeText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroChip(icon: ImageVector, text: String, textColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.12f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickStatsGrid(items: List<WatchedItem>) {
    val thisMonth = remember(items) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        items.count {
            cal.timeInMillis = it.watchedAt
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }
    val byMonth = remember(items) {
        items.groupBy { w ->
            val c = Calendar.getInstance().apply { timeInMillis = w.watchedAt }
            c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH)
        }
    }
    val average = remember(byMonth, items) {
        val monthsSpanned = byMonth.keys.size.coerceAtLeast(1)
        String.format(Locale.US, "%.1f", items.size.toFloat() / monthsSpanned) // lint: DefaultLocale
    }
    val bestMonthCount = remember(byMonth) {
        byMonth.maxOfOrNull { it.value.size } ?: 0
    }

    val stats = listOf(
        QuickStat(thisMonth.toString(), stringResource(R.string.stats_this_month)),
        QuickStat(average, stringResource(R.string.stats_average_per_month)),
        QuickStat(bestMonthCount.toString(), stringResource(R.string.stats_best_month)),
    )

    QuickStatsRow(title = stringResource(R.string.stats_in_brief), stats = stats)
}

@Composable
private fun GenreBreakdownCard(items: List<WatchedItem>) {
    val extended = OrionColors.colors
    val genreCounts = remember(items) {
        items.asSequence()
            .flatMap { it.genres?.split(",").orEmpty().asSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)
    }

    Column {
        SectionTitle(stringResource(R.string.stats_favorite_genres))
        Box(modifier = Modifier.cardBackground(extended)) {
            if (genreCounts.isEmpty()) {
                Text(
                    stringResource(R.string.stats_no_genre_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val max = genreCounts.first().value
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    genreCounts.forEachIndexed { index, entry ->
                        StatBar(label = entry.key, count = entry.value, max = max, rank = index)
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchTimeCard(items: List<WatchedItem>) {
    val (totalMinutes, missingCount) = remember(items) {
        val known = items.mapNotNull { it.durationMinutes }
        known.sum() to (items.size - known.size)
    }

    Column {
        SectionTitle(stringResource(R.string.stats_watch_time_estimated))
        Column(modifier = Modifier.cardBackground(OrionColors.colors)) {
            if (totalMinutes <= 0) {
                Text(
                    stringResource(R.string.stats_no_duration_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val totalHours = totalMinutes / 60
                val fullDays = totalHours / 24
                val minutesRemainder = totalMinutes % 60
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (totalHours > 0) "${totalHours}h" else "${minutesRemainder}min",
                        style = StatNumberStyle,
                    )
                    if (totalHours > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${minutesRemainder.toString().padStart(2, '0')}min",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                if (fullDays >= 1) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.stats_days_nonstop, fullDays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (missingCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.stats_unknown_duration_count, missingCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyActivityCard(items: List<WatchedItem>) {
    val extended = OrionColors.colors
    val locale = Locale.US
    val monthLabels = remember {
        (0..11).map { month ->
            Calendar.getInstance().apply { set(Calendar.MONTH, month) }
                .getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)
                .orEmpty()
                .replaceFirstChar { it.uppercase(locale) }
        }
    }

    val last6Months = remember(items) {
        val now = Calendar.getInstance()
        val buckets = (5 downTo 0).map { offset ->
            val c = now.clone() as Calendar
            c.add(Calendar.MONTH, -offset)
            c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
        }

        val counters = IntArray(buckets.size)
        items.forEach { w ->
            val c = Calendar.getInstance().apply { timeInMillis = w.watchedAt }
            val key = c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
            val idx = buckets.indexOf(key)
            if (idx >= 0) counters[idx]++
        }

        buckets.mapIndexed { idx, pair -> monthLabels[pair.second] to counters[idx] }
    }

    Column {
        SectionTitle(stringResource(R.string.stats_activity_6months))
        Box(modifier = Modifier.cardBackground(extended)) {
            val max = (last6Months.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                last6Months.forEachIndexed { idx, pair ->
                    val (label, count) = pair
                    val isLast = idx == last6Months.lastIndex
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

@Composable
private fun WeekdayBreakdownCard(items: List<WatchedItem>) {
    val extended = OrionColors.colors
    val dayLabels = remember {
        val locale = Locale.US
        listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY,
            Calendar.SUNDAY
        ).map { dow ->
            Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dow) }
                .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale)
                .orEmpty()
                .replaceFirstChar { it.uppercase(locale) }
        }
    }

    val counts = remember(items) {
        val counters = IntArray(7)
        items.forEach { w ->
            val c = Calendar.getInstance().apply { timeInMillis = w.watchedAt }
            val dow = c.get(Calendar.DAY_OF_WEEK)
            val index = (dow + 5) % 7
            counters[index]++
        }
        counters.toList()
    }

    Column {
        SectionTitle(stringResource(R.string.stats_favorite_days))
        Box(modifier = Modifier.cardBackground(extended)) {
            val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
            val topIndex = counts.indices.maxByOrNull { counts[it] } ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                dayLabels.forEachIndexed { idx, label ->
                    val count = counts[idx]
                    val isTop = idx == topIndex && count > 0
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((90 * count.toFloat() / max).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (isTop) Brush.horizontalGradient(listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer))
                                    else Brush.verticalGradient(listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    ))
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isTop) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isTop) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Longest streak of consecutive days with at least one watch (movie or episode).
 * Purely derived from timestamps already in memory, no extra field needed.
 */
private fun longestDailyStreak(items: List<WatchedItem>): Int {
    if (items.isEmpty()) return 0
    val days = items.map { w ->
        val c = Calendar.getInstance().apply { timeInMillis = w.watchedAt }
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }.distinct().sorted()

    var longest = 1
    var current = 1
    val dayMillis = 24 * 60 * 60 * 1000L
    for (i in 1 until days.size) {
        if (days[i] - days[i - 1] == dayMillis) {
            current += 1
            longest = maxOf(longest, current)
        } else {
            current = 1
        }
    }
    return longest
}
