package com.orion.app.cinema.ui.detail.components

import com.orion.app.cinema.data.MovieDetail
import com.orion.app.cinema.data.TvDetail

/**
 * Unified loading state for the detail page (movie OR TV show). Replaces the two originally
 * separate sealed interfaces: a single screen (DetailScreen) handles both media types based
 * on `mediaType`, so a single state is enough.
 */
sealed interface DetailScreenState {
    data object Loading : DetailScreenState
    data class MovieSuccess(val movie: MovieDetail) : DetailScreenState
    data class TvSuccess(val tvShow: TvDetail) : DetailScreenState
    data class Error(val message: String) : DetailScreenState
}
