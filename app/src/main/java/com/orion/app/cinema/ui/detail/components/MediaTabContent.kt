package com.orion.app.cinema.ui.detail.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.orion.app.cinema.data.GalleryImage
import com.orion.app.cinema.data.CinemaGallery
import com.orion.app.cinema.data.VideoItem
import com.orion.app.core.ui.components.SectionTitle
import com.orion.app.core.ui.theme.OrionColors
import androidx.compose.ui.res.stringResource
import com.orion.app.R

/**
 * "Media" tab: trailers/videos (already loaded with the page), then backdrops and
 * posters in better quality, loaded separately (see [isLoadingImages]) only when this tab
 * is selected. Clicking an image opens the fullscreen viewer
 * [com.orion.app.core.ui.components.PosterGalleryDialog] — this feature now lives here,
 * no longer on clicking the header's poster.
 */
@Composable
fun MediaTabContent(
    videos: List<VideoItem>,
    isLoadingImages: Boolean,
    gallery: CinemaGallery?,
    onImageClick: (images: List<GalleryImage>, index: Int) -> Unit
) {
    val context = LocalContext.current
    val backdrops = gallery?.backdrops.orEmpty()
    val posters = gallery?.posters.orEmpty()
    val extended = OrionColors.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (videos.isNotEmpty()) {
            SectionTitle(
                title = stringResource(R.string.media_trailers_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(videos) { _, video ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(200.dp)
                            .height(115.dp)
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com/watch?v=${video.key}")
                                )
                                context.startActivity(intent)
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                            AsyncImage(
                                model = "https://img.youtube.com/vi/${video.key}/hqdefault.jpg",
                                contentDescription = video.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.media_play_description),
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(40.dp)
                                    .background(
                                        // Same gradient as the "date" badges (Account) and
                                        // "episodes remaining" badges (Bookmark), radial for
                                        // a clean circular halo behind the play button.
                                        Brush.radialGradient(
                                            listOf(
                                                extended.navBarSelectedContainerAlt,
                                                extended.navBarSelectedContainer
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isLoadingImages) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (backdrops.isNotEmpty()) {
            SectionTitle(
                title = stringResource(R.string.media_banners_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(backdrops) { index, image ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .height(130.dp)
                            .clickable { onImageClick(backdrops, index) }
                    ) {
                        AsyncImage(
                            model = image.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
        }

        if (posters.isNotEmpty()) {
            SectionTitle(
                title = stringResource(R.string.media_posters_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(posters) { index, image ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(120.dp)
                            .height(175.dp)
                            .clickable { onImageClick(posters, index) }
                    ) {
                        AsyncImage(
                            model = image.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
        }

        if (videos.isEmpty() && backdrops.isEmpty() && posters.isEmpty()) {
            Text(
                text = stringResource(R.string.media_none_available),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
