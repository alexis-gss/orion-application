package com.orion.app.cinema.ui.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.orion.app.cinema.data.WatchProviderItem
import com.orion.app.core.ui.components.PosterImage
import com.orion.app.core.ui.components.SectionTitle
import androidx.compose.ui.res.stringResource
import com.orion.app.R

@Composable
fun StreamingProvidersRow(
    providers: List<WatchProviderItem>,
    posterUrlProvider: (String?) -> String?
) {
    if (providers.isEmpty()) return

    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)) {
        SectionTitle(title = stringResource(R.string.streaming_available_title))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(providers) { provider ->
                PosterImage(
                    model = posterUrlProvider(provider.logoPath),
                    contentDescription = provider.providerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}
