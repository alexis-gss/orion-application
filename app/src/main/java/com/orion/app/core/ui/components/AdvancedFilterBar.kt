package com.orion.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orion.app.R
import com.orion.app.core.ui.theme.OrionColors

/** Sort field available on filterable lists (SeeAllScreen for cinema/games/books, games Bookmark). */
enum class SortField(val labelRes: Int) {
    RATING(R.string.sort_field_rating),
    RELEASE_DATE(R.string.sort_field_release_date),
    ADDED_DATE(R.string.sort_field_added_date),
}

enum class SortDirection { ASC, DESC }

/**
 * Advanced filter bar reused by the "see all" screens (cinema, games, books) and
 * filterable lists (games Bookmark): a genre select and a sort select (rating /
 * release date / added date, with direction toggling by reselecting the same field),
 * rather than chip rows — as the number of genres grows with content added over time, a
 * chip row would quickly become unreadable and endless to scroll through. A reset button
 * on the right clears both filters at once.
 */
@Composable
fun AdvancedFilterBar(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit,
    sortField: SortField?,
    sortDirection: SortDirection,
    onSortFieldSelected: (SortField) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    sortFields: List<SortField> = SortField.entries,
) {
    val hasActiveFilters = selectedGenre != null || sortField != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (genres.isNotEmpty()) {
            GenreSelect(
                genres = genres,
                selectedGenre = selectedGenre,
                onGenreSelected = onGenreSelected,
                modifier = Modifier.weight(1f)
            )
        }
        if (sortFields.isNotEmpty()) {
            SortSelect(
                sortFields = sortFields,
                sortField = sortField,
                sortDirection = sortDirection,
                onSortFieldSelected = onSortFieldSelected,
                modifier = Modifier.weight(1f)
            )
        }
        // weight(1f) here too: the 3 elements (genre, sort, reset) share the width
        // equally rather than leaving the reset button at its default size (48dp) while
        // the two selects split the rest.
        IconButton(
            onClick = onReset,
            enabled = hasActiveFilters
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(R.string.filters_reset),
                tint = if (hasActiveFilters) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun GenreSelect(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilterSelectTrigger(
            label = selectedGenre ?: stringResource(R.string.filters_genre_all),
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filters_genre_all)) },
                onClick = { onGenreSelected(null); expanded = false }
            )
            genres.forEach { genre ->
                DropdownMenuItem(
                    text = { Text(genre) },
                    onClick = { onGenreSelected(if (selectedGenre == genre) null else genre); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SortSelect(
    sortFields: List<SortField>,
    sortField: SortField?,
    sortDirection: SortDirection,
    onSortFieldSelected: (SortField) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilterSelectTrigger(
            label = sortField?.let { stringResource(it.labelRes) } ?: stringResource(R.string.filters_sort_label),
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = sortField?.let {
                {
                    Icon(
                        imageVector = if (sortDirection == SortDirection.DESC) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sortFields.forEach { field ->
                val selected = sortField == field
                DropdownMenuItem(
                    text = { Text(stringResource(field.labelRes)) },
                    trailingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = if (sortDirection == SortDirection.DESC) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                contentDescription = null
                            )
                        }
                    } else null,
                    onClick = {
                        onSortFieldSelected(field)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** "Select" button shared by both dropdowns: current label + chevron (or a direction
 *  icon for sorting), chip style consistent with the rest of the app. Internal (not
 *  private): reused by the search pages (cinema/games/books) via InlineFilterBar.kt for
 *  their own inline selectors, on the same visual principle as this "see all" screen. */
@Composable
internal fun FilterSelectTrigger(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val extended = OrionColors.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(extended.chipSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        if (trailingIcon != null) {
            trailingIcon()
        } else {
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
