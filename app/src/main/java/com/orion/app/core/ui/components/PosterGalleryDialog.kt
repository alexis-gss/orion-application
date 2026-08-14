package com.orion.app.core.ui.components

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.orion.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Fullscreen viewer for a list of posters: horizontal swipe (HorizontalPager),
 * previous/next arrows, "i / total" counter. Open/close transition with a fade + slight
 * zoom (not a true "shared element" from the original thumbnail, but a smooth progressive
 * transition with no experimental Compose dependency).
 *
 * Les boutons zoom +/- sont rendus au niveau du Dialog (pas dans chaque page du pager) :
 * ils restent donc fixes à l'écran pendant le swipe, et ne sont plus "avalés" par le
 * détecteur de pan/zoom de l'image une fois zoomée.
 */
@Composable
fun PosterGalleryDialog(
    posters: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (posters.isEmpty()) return

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = { visible = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val view = LocalView.current

        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(0, posters.lastIndex)
        ) { posters.size }

        var isZoomed by remember { mutableStateOf(false) }
        var isDownloading by remember { mutableStateOf(false) }

        LaunchedEffect(visible) {
            if (!visible) {
                kotlinx.coroutines.delay(200)
                onDismiss()
            }
        }

        DisposableEffect(Unit) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // Fond de fenêtre semi-transparent (au lieu d'un noir opaque).
                window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.argb(230, 0, 0, 0)))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
            onDispose {
                if (window != null) {
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    WindowInsetsControllerCompat(window, window.decorView)
                        .show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                scope.launch {
                    isDownloading = true
                    val ok = downloadImageToGallery(context, posters[pagerState.currentPage])
                    isDownloading = false
                    Toast.makeText(
                        context,
                        context.getString(
                            if (ok) R.string.gallery_download_success else R.string.gallery_download_error
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.gallery_download_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        fun requestDownload() {
            val needsLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED

            if (needsLegacyPermission) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                scope.launch {
                    isDownloading = true
                    val ok = downloadImageToGallery(context, posters[pagerState.currentPage])
                    isDownloading = false
                    Toast.makeText(
                        context,
                        context.getString(
                            if (ok) R.string.gallery_download_success else R.string.gallery_download_error
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(tween(220), initialScale = 0.85f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.85f)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    // Fond semi-transparent au lieu d'un noir opaque.
                    .background(Color.Black.copy(alpha = 0.9f))
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isZoomed,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ZoomableMedia(
                        model = posters[page],
                        isActivePage = page == pagerState.currentPage,
                        onZoomStateChanged = { isZoomed = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AnimatedVisibility(
                    visible = !isZoomed,
                    modifier = Modifier.align(Alignment.TopCenter),
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150))
                ) {
                    GalleryTopBar(
                        isDownloading = isDownloading,
                        onDownloadClick = { requestDownload() },
                        onCloseClick = { visible = false }
                    )
                }

                AnimatedVisibility(
                    visible = !isZoomed,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150))
                ) {
                    GalleryBottomBar(
                        posters = posters,
                        pagerState = pagerState,
                        scope = scope
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableMedia(
    model: String,
    isActivePage: Boolean,
    onZoomStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scale = remember(model) { Animatable(1f) }
    val offsetX = remember(model) { Animatable(0f) }
    val offsetY = remember(model) { Animatable(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val maxScale = 5f
    val doubleTapScale = 2.5f

    LaunchedEffect(scale.value, isActivePage) {
        onZoomStateChanged(isActivePage && scale.value > 1.01f)
    }

    fun clamp(value: Float, dimension: Int, currentScale: Float): Float {
        if (currentScale <= 1f || dimension == 0) return 0f
        val maxOffset = dimension * (currentScale - 1f) / 2f
        return value.coerceIn(-maxOffset, maxOffset)
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(model) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        scope.launch {
                            if (scale.value > 1f) {
                                scale.animateTo(1f, tween(250))
                                offsetX.animateTo(0f, tween(250))
                                offsetY.animateTo(0f, tween(250))
                            } else {
                                val newX = clamp(
                                    (containerSize.width / 2f - tapOffset.x) * (doubleTapScale - 1f),
                                    containerSize.width,
                                    doubleTapScale
                                )
                                val newY = clamp(
                                    (containerSize.height / 2f - tapOffset.y) * (doubleTapScale - 1f),
                                    containerSize.height,
                                    doubleTapScale
                                )
                                scale.animateTo(doubleTapScale, tween(250))
                                offsetX.animateTo(newX, tween(250))
                                offsetY.animateTo(newY, tween(250))
                            }
                        }
                    }
                )
            }
            .pointerInput(model) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val isMultiTouch = event.changes.size > 1
                        if (isMultiTouch || scale.value > 1f) {
                            val newScale = (scale.value * zoomChange).coerceIn(1f, maxScale)
                            val newX = if (newScale > 1f) {
                                clamp(offsetX.value + panChange.x, containerSize.width, newScale)
                            } else 0f
                            val newY = if (newScale > 1f) {
                                clamp(offsetY.value + panChange.y, containerSize.height, newScale)
                            } else 0f
                            scope.launch {
                                scale.snapTo(newScale)
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        PosterImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            backgroundColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offsetX.value
                    translationY = offsetY.value
                }
        )
    }
}

@Composable
private fun GalleryTopBar(
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDownloadClick, enabled = !isDownloading) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = stringResource(R.string.gallery_download),
                    tint = Color.White
                )
            }
        }
        IconButton(onClick = onCloseClick) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.gallery_close), tint = Color.White)
        }
    }
}

@Composable
private fun GalleryBottomBar(
    posters: List<String>,
    pagerState: PagerState,
    scope: CoroutineScope
) {
    val listState = rememberLazyListState()
    val currentPage = pagerState.currentPage

    LaunchedEffect(currentPage) {
        val target = (currentPage - 2).coerceAtLeast(0)
        listState.animateScrollToItem(target)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            .padding(top = 24.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (posters.size > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    scope.launch {
                        val prev = (currentPage - 1 + posters.size) % posters.size
                        pagerState.animateScrollToPage(prev)
                    }
                }) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = stringResource(R.string.gallery_previous),
                        tint = Color.White
                    )
                }

                var dragAccumulator by remember { mutableStateOf(0f) }
                Text(
                    "${currentPage + 1} / ${posters.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .pointerInput(posters.size) {
                            val stepPx = 24.dp.toPx()
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccumulator = 0f },
                                onHorizontalDrag = { change, delta ->
                                    change.consume()
                                    dragAccumulator += delta
                                    if (abs(dragAccumulator) >= stepPx) {
                                        val direction = if (dragAccumulator > 0) 1 else -1
                                        val next = (pagerState.currentPage + direction)
                                            .coerceIn(0, posters.lastIndex)
                                        scope.launch { pagerState.scrollToPage(next) }
                                        dragAccumulator = 0f
                                    }
                                }
                            )
                        }
                )

                IconButton(onClick = {
                    scope.launch {
                        val next = (currentPage + 1) % posters.size
                        pagerState.animateScrollToPage(next)
                    }
                }) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.gallery_next),
                        tint = Color.White
                    )
                }
            }
        }

        if (posters.size > 4) {
            Spacer(Modifier.height(12.dp))
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(posters) { index, url ->
                    val selected = index == currentPage
                    PosterImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(if (selected) 52.dp else 40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                scope.launch {
                                    if (abs(index - currentPage) <= 3) {
                                        pagerState.animateScrollToPage(index)
                                    } else {
                                        pagerState.scrollToPage(index)
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

// Enregistre l'image dans la galerie publique de l'appareil, sous
// Pictures/Orion (Android 10+) — PAS dans le dossier "Téléchargements".
// C'est le comportement attendu de MediaStore.Images avec RELATIVE_PATH :
// l'appli Photos/Galerie affichera l'image dans un album "Orion".
private suspend fun downloadImageToGallery(context: Context, url: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            val bitmap = (result as? SuccessResult)?.drawable?.let { (it as? BitmapDrawable)?.bitmap }
                ?: return@withContext false

            val filename = "orion_${System.currentTimeMillis()}.jpg"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Orion")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext false

            val written = resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: false

            if (written && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            written
        } catch (e: Exception) {
            false
        }
    }