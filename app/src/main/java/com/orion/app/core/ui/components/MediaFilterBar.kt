package com.orion.app.core.ui.components

import androidx.annotation.StringRes
import com.orion.app.R

enum class CinemaFilter(@StringRes val label: Int) {
    ALL(R.string.filters_all),
    MOVIE(R.string.filters_movie),
    TV(R.string.filters_tv)
}

enum class SortOption(@StringRes val label: Int) {
    RELEVANCE(R.string.sort_relevance),
    RATING(R.string.sort_rating),
    RECENT(R.string.sort_recent)
}
