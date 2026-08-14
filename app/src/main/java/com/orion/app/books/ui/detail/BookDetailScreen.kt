package com.orion.app.books.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.orion.app.core.ui.components.ActionButtons
import com.orion.app.core.ui.components.DetailHeader
import com.orion.app.core.ui.components.FloatingTopBar
import com.orion.app.core.ui.components.SynopsisSection
import com.orion.app.books.data.BooksRepository
import com.orion.app.books.data.FavoriteBook
import com.orion.app.books.data.FollowedBook
import com.orion.app.books.data.HardcoverBook
import com.orion.app.books.data.ReadBook
import com.orion.app.books.ui.detail.components.BookDetailScreenState
import com.orion.app.books.ui.detail.components.BookInfoSection
import com.orion.app.books.ui.components.bookCarouselSection
import com.orion.app.books.ui.components.seriesTabContent
import com.orion.app.books.ui.components.toCardData
import com.orion.app.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

/**
 * Book detail page, on the same skeleton as DetailScreen (cinema) / GameDetailScreen
 * (games): banner + overlapping poster, action buttons, synopsis, info, recommendations,
 * a floating bar with the title on scroll. Unlike TMDB/IGDB, Hardcover has no wide banner
 * (backdrop) or media gallery (videos/screenshots) — no equivalent to the cast/financial
 * block either. On the other hand, like TV shows (Episodes tab), a book belonging to a
 * Hardcover series has two tabs: Details (synopsis, info, similar) and Series (every
 * volume of the saga, in order) — a standalone book only gets the Details tab.
 */
@Composable
fun BookDetailScreen(
    repository: BooksRepository,
    volumeId: String,
    onBack: () -> Unit,
    onOpenItem: (volumeId: String) -> Unit = {},
) {
    var state by remember(volumeId) { mutableStateOf<BookDetailScreenState>(BookDetailScreenState.Loading) }
    var reloadKey by remember(volumeId) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var isFavorite by remember(volumeId) { mutableStateOf(false) }
    var isFollowed by remember(volumeId) { mutableStateOf(false) }
    var isRead by remember(volumeId) { mutableStateOf(false) }

    LaunchedEffect(volumeId, reloadKey) {
        state = BookDetailScreenState.Loading
        state = try {
            val b = repository.getBookDetail(volumeId)
            if (b != null) {
                isFavorite = repository.isFavorite(volumeId)
                isFollowed = repository.isFollowed(volumeId)
                isRead = repository.getReadStatus(volumeId)?.status == "read"
                BookDetailScreenState.Success(b)
            } else {
                BookDetailScreenState.Error(context.getString(R.string.detail_load_error))
            }
        } catch (e: Exception) {
            BookDetailScreenState.Error(e.message ?: context.getString(R.string.detail_load_error))
        }
    }

    val currentTitle = (state as? BookDetailScreenState.Success)?.book?.name

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            is BookDetailScreenState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is BookDetailScreenState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = current.message, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { reloadKey++ }) {
                        Text(stringResource(R.string.detail_retry))
                    }
                }
            }

            is BookDetailScreenState.Success -> {
                BookDetailContent(
                    repository = repository,
                    book = current.book,
                    listState = listState,
                    isFollowed = isFollowed,
                    isFavorite = isFavorite,
                    isRead = isRead,
                    onFollowToggle = { b ->
                        scope.launch {
                            if (isFollowed) {
                                repository.unfollow(volumeId)
                            } else {
                                repository.follow(
                                    FollowedBook(
                                        volumeId = volumeId,
                                        title = b.name,
                                        coverUrl = b.coverUrl,
                                        // French date only (see isReleasedInFrance): without a known
                                        // French edition, no reliable release date is yet
                                        // known for this reader.
                                        releaseTimestamp = b.frenchReleaseTimestamp,
                                        isReleased = b.isReleasedInFrance,
                                        lastCheckedAt = System.currentTimeMillis(),
                                        categories = b.categories.joinToString(",").ifBlank { null }
                                    )
                                )
                            }
                            isFollowed = !isFollowed
                        }
                    },
                    onReadToggle = { b ->
                        scope.launch {
                            if (isRead) {
                                repository.removeReadStatus(volumeId)
                            } else {
                                repository.setReadStatus(
                                    ReadBook(
                                        volumeId = volumeId,
                                        title = b.name,
                                        coverUrl = b.coverUrl,
                                        status = "read",
                                        updatedAt = System.currentTimeMillis(),
                                        categories = b.categories.joinToString(",").ifBlank { null },
                                        pageCount = b.pageCount
                                    )
                                )
                                if (isFollowed) {
                                    repository.unfollow(volumeId)
                                    isFollowed = false
                                }
                            }
                            isRead = !isRead
                        }
                    },
                    onFavoriteToggle = { b ->
                        scope.launch {
                            if (isFavorite) {
                                repository.removeFavorite(volumeId)
                            } else {
                                repository.addFavorite(
                                    FavoriteBook(
                                        volumeId = volumeId,
                                        title = b.name,
                                        coverUrl = b.coverUrl,
                                        addedAt = System.currentTimeMillis(),
                                        categories = b.categories.joinToString(",").ifBlank { null }
                                    )
                                )
                                if (!isRead) {
                                    repository.setReadStatus(
                                        ReadBook(
                                            volumeId = volumeId,
                                            title = b.name,
                                            coverUrl = b.coverUrl,
                                            status = "read",
                                            updatedAt = System.currentTimeMillis(),
                                            categories = b.categories.joinToString(",").ifBlank { null },
                                            pageCount = b.pageCount
                                        )
                                    )
                                    isRead = true
                                }
                                if (isFollowed) {
                                    repository.unfollow(volumeId)
                                    isFollowed = false
                                }
                            }
                            isFavorite = !isFavorite
                        }
                    },
                    onSelectBook = onOpenItem,
                )
            }
        }

        FloatingTopBar(
            listState = listState,
            currentTitle = currentTitle,
            onBack = onBack,
        )
    }
}

@Composable
private fun BookDetailContent(
    repository: BooksRepository,
    book: HardcoverBook,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isFollowed: Boolean,
    isFavorite: Boolean,
    isRead: Boolean,
    onFollowToggle: (HardcoverBook) -> Unit,
    onReadToggle: (HardcoverBook) -> Unit,
    onFavoriteToggle: (HardcoverBook) -> Unit,
    onSelectBook: (String) -> Unit,
) {
    var similarBooks by remember(book.id) { mutableStateOf<List<HardcoverBook>>(emptyList()) }
    LaunchedEffect(book.id) {
        similarBooks = try { repository.getSimilarBooks(book) } catch (e: Exception) { emptyList() }
    }

    // "Series" tab: shown if Hardcover exposes an actual series id (seriesId, given
    // priority) or, failing that, if a series name could be extracted from the title (see
    // HardcoverBook.seriesName) — a standalone book has no Series tab at all, rather than
    // an empty one.
    var seriesBooks by remember(book.id) { mutableStateOf<List<HardcoverBook>>(emptyList()) }
    val hasSeriesSignal = book.seriesId != null || book.seriesName != null
    var isSeriesLoading by remember(book.id) { mutableStateOf(hasSeriesSignal) }
    LaunchedEffect(book.id) {
        if (!hasSeriesSignal) return@LaunchedEffect
        isSeriesLoading = true
        seriesBooks = try { repository.getSeriesBooks(book) } catch (e: Exception) { emptyList() }
        isSeriesLoading = false
    }
    val detailsTab = stringResource(R.string.tab_details)
    val seriesTab = stringResource(R.string.book_series_tab)
    val bookSimilar = stringResource(R.string.book_similar_title)
    val bookSeriesEmpty = stringResource(R.string.book_series_empty)
    val bookTabs = if (hasSeriesSignal) listOf(detailsTab, seriesTab) else listOf(detailsTab)
    var selectedTabIndex by remember(book.id) { mutableIntStateOf(0) }

    val infoLine = remember(book.id) {
        listOfNotNull(book.year, book.authorsLabel).joinToString(" · ")
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            // No wide banner on Hardcover's side (no backdrop, unlike TMDB/IGDB):
            // backdropPath is left null, DetailHeader then falls back to the plain
            // background gradient behind the poster.
            DetailHeader(
                backdropPath = null,
                posterPath = book.coverUrl,
                title = book.name,
                tagline = book.volumeInfo.subtitle,
                voteAverage = book.displayRating,
                infoLine = infoLine.ifBlank { null },
                genres = book.categories.map { it.substringBefore(" / ") }.distinct(),
                posterFallback = { com.orion.app.books.ui.components.BookCoverPlaceholder() },
            )
        }

        item {
            val context = LocalContext.current
            ActionButtons(
                isFollowed = isFollowed,
                followDisabled = isRead || isFavorite,
                onFollowToggle = { onFollowToggle(book) },
                isWatched = isRead,
                onWatchedToggle = { onReadToggle(book) },
                watchedDisabled = false,
                watchedLabel = stringResource(R.string.book_read_label),
                notWatchedLabel = stringResource(R.string.book_read_label),
                isFavorite = isFavorite,
                onFavoriteToggle = { onFavoriteToggle(book) },
                favoriteDisabled = false,
                onExtraAction = book.previewUrl?.let { url ->
                    { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                }
            )
        }

        if (bookTabs.size > 1) {
            item {
                androidx.compose.material3.TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                    bookTabs.forEachIndexed { index, tabLabel ->
                        androidx.compose.material3.Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(text = tabLabel) }
                        )
                    }
                }
            }
        }

        when (selectedTabIndex) {
            0 -> {
                // Synopsis/info/recommendations: only in the "Details" tab, not shown in
                // the "Series" tab — same layout as cinema.
                item {
                    SynopsisSection(
                        tagline = null,
                        overview = book.summary,
                    )
                }
                item { BookInfoSection(book) }
                if (similarBooks.isNotEmpty()) {
                    // Horizontal carousel, same presentation as cinema/games
                    // recommendations (bookCarouselSection), rather than a vertical list.
                    bookCarouselSection(
                        title = bookSimilar,
                        items = similarBooks.map { it.toCardData() },
                        emptyLabel = "",
                        onItemClick = { onSelectBook(it.volumeId) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
            1 -> {
                if (isSeriesLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                } else {
                    seriesTabContent(
                        items = seriesBooks.map { it.toCardData() },
                        onItemClick = { onSelectBook(it.volumeId) },
                        emptyLabel = bookSeriesEmpty,
                    )
                }
            }
        }
    }
}