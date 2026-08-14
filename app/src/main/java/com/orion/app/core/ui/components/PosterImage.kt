package com.orion.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

/**
 * Poster with a loading indicator shown while the image loads, which disappears as soon as
 * the poster is ready (or on error/missing URL).
 *
 * [fallback] is an optional slot shown in place of the image when [model] is null/blank or
 * loading fails. Defaults to null (unchanged behavior: nothing is shown), used by the books
 * domain for a placeholder cover (see BookCoverPlaceholder) when Hardcover provides none.
 */
@Composable
fun PosterImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background,
) {
    var isLoading by remember(model) { mutableStateOf(true) }
    var isError by remember(model) { mutableStateOf(model.isNullOrBlank()) }

    Box(
        modifier = modifier.background(backgroundColor).then(if (shape != null) Modifier.clip(shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (!model.isNullOrBlank()) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize(),
                onState = { state ->
                    isLoading = state is AsyncImagePainter.State.Loading
                    isError = state is AsyncImagePainter.State.Error
                }
            )
        }
        if (isLoading && !isError) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        }
        if (isError && fallback != null) {
            fallback()
        }
    }
}

// small helper to avoid an extra androidx.compose.ui.unit.dp import everywhere
private fun Int.dp() = Dp(this.toFloat())
private fun Double.dp() = Dp(this.toFloat())