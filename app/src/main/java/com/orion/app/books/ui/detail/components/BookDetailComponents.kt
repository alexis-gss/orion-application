package com.orion.app.books.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.books.data.HardcoverBook
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors

/** Loading state for the book detail page — same shape as GameDetailScreenState. */
sealed interface BookDetailScreenState {
    data object Loading : BookDetailScreenState
    data class Success(val book: HardcoverBook) : BookDetailScreenState
    data class Error(val message: String) : BookDetailScreenState
}

/**
 * "Info" section: publisher, publication date, pages, ISBN — the books equivalent of
 * TimeToBeatSection on the games side (IGDB completion times), following the same
 * key/value row card pattern. Hardcover exposes neither publisher nor ISBN at the book
 * level (only per edition, not fetched here so this stays a single lightweight network
 * call per page): these two rows simply disappear from the card rather than showing an
 * empty value, exactly like any other field missing from Hardcover.
 */
@Composable
fun BookInfoSection(book: HardcoverBook) {
    val publisherLabel = stringResource(R.string.game_publisher_label)
    val pagesLabel = stringResource(R.string.book_pages_label)
    val languageLabel = stringResource(R.string.book_language_label)
    val rows = listOfNotNull(
        book.publisher?.let { publisherLabel to it },
        book.pageCount?.let { pagesLabel to it.toString() },
        book.volumeInfo.language?.uppercase()?.let { languageLabel to it },
        (book.isbn13 ?: book.isbn10)?.let { "ISBN" to it },
    )
    if (rows.isEmpty()) return

    val extended = OrionColors.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        SectionTitle(title = stringResource(R.string.game_info_title), modifier = Modifier.padding(horizontal = 16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(extended.cardSurface)
                .border(1.dp, extended.cardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            rows.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                if (index != rows.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(color = extended.cardBorder)
                }
            }
        }
    }
}
