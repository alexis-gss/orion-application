package com.orion.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orion.app.R

/**
 * Generic inline selector (compact chip + dropdown menu), same visual principle as
 * GenreSelect/SortSelect in AdvancedFilterBar (the "see all" screen): a dropdown menu
 * rather than a row of chips that keeps growing with the number of options. Generic over T
 * so it's reusable as-is with an enum (MediaFilter, SortOption...) or plain strings
 * (genres/categories derived from search results).
 */
@Composable
fun <T> FilterDropdown(
    label: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilterSelectTrigger(
            label = label,
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Inline filter bar for search pages (cinema/games/books): a filter selector
 * (media type / genre / category, depending on the domain) + a sort selector, on the
 * same principle as AdvancedFilterBar (SeeAllScreen) rather than the previous chip rows —
 * more compact and more practical once the number of options grows (e.g. the list of
 * genres/categories derived from results can quickly get long).
 */
@Composable
fun <F, S> InlineSearchFilterBar(
    filterLabel: String,
    filterOptions: List<F>,
    filterOptionLabel: @Composable (F) -> String,
    onFilterSelected: (F) -> Unit,
    sortLabel: String,
    sortOptions: List<S>,
    sortOptionLabel: @Composable (S) -> String,
    onSortSelected: (S) -> Unit,
    hasActiveFilters: Boolean,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (filterOptions.isNotEmpty()) {
            FilterDropdown(
                label = filterLabel,
                options = filterOptions,
                optionLabel = filterOptionLabel,
                onOptionSelected = onFilterSelected,
                modifier = Modifier.weight(1f),
            )
        }
        if (sortOptions.isNotEmpty()) {
            FilterDropdown(
                label = sortLabel,
                options = sortOptions,
                optionLabel = sortOptionLabel,
                onOptionSelected = onSortSelected,
                modifier = Modifier.weight(1f),
            )
        }
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
