package com.orion.app.cinema.ui.detail.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.dp
import com.orion.app.cinema.data.CinemaRepository
import com.orion.app.cinema.data.SearchResult
import com.orion.app.cinema.ui.components.CinemaCardData
import com.orion.app.cinema.ui.components.cinemaCarouselSection

/**
 * Maps the detail page's SearchResult items to MediaCardData and delegates rendering to
 * mediaCarouselSection, so this uses the same carousel style as AccountScreen.
 */
fun LazyListScope.recommendationsSection(
    repository: CinemaRepository,
    recommendations: List<SearchResult>,
    onSelectMedia: (String, Int) -> Unit,
    title: String
) {
    val items = recommendations.map { item ->
        CinemaCardData(
            key = "${item.resolvedMediaType}_${item.id}",
            tmdbId = item.id,
            mediaType = item.resolvedMediaType,
            title = item.displayTitle,
            posterPath = item.posterPath
        )
    }

    cinemaCarouselSection(
        repository = repository,
        title = title,
        items = items,
        emptyLabel = "",
        onItemClick = { data ->
            onSelectMedia(data.mediaType, data.tmdbId)
        },
        sectionHorizontalPadding = 16.dp,
    )
}
