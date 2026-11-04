package com.readrops.app.item

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberConstraintsSizeResolver
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.size.SizeResolver
import coil3.size.pxOrElse
import com.readrops.app.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun ItemImageViewerDialog(
    imageUrls: List<String>,
    initialImageUrl: String,
    onDismiss: () -> Unit
) {
    val safeImages = remember(imageUrls, initialImageUrl) {
        (imageUrls + initialImageUrl).filter(String::isNotBlank).distinct()
    }
    val initialPage = safeImages.indexOf(initialImageUrl).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { safeImages.size })
    val rotations = remember { mutableStateMapOf<String, Float>() }
    val density = LocalDensity.current
    val currentImage = safeImages.getOrNull(pagerState.currentPage)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { safeImages[it] }
            ) { page ->
                val imageUrl = safeImages[page]
                ZoomableArticleImage(
                    imageUrl = imageUrl,
                    rotationDegrees = rotations[imageUrl] ?: 0f,
                    density = density
                )
            }

            IconButton(
                onClick = {
                    currentImage?.let { url -> rotations[url] = ((rotations[url] ?: 0f) + 90f) % 360f }
                },
                enabled = currentImage != null,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()
            ) {
                Icon(Icons.Default.Refresh, stringResource(R.string.rotate_image), tint = Color.White)
            }

            if (safeImages.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${safeImages.size}",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
            ) {
                Icon(Icons.Default.Close, stringResource(R.string.close_image_viewer), tint = Color.White)
            }
        }
    }
}

@Composable
private fun ZoomableArticleImage(
    imageUrl: String,
    rotationDegrees: Float,
    density: androidx.compose.ui.unit.Density
) {
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    var viewport by remember(imageUrl) { mutableStateOf(IntSize.Zero) }
    val constraintsSizeResolver = rememberConstraintsSizeResolver()
    val zoomSizeResolver = remember(constraintsSizeResolver) {
        ZoomableImageSizeResolver(constraintsSizeResolver)
    }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .size(zoomSizeResolver)
            .build()
    )
    val imageSize = painter.intrinsicSize
    val rotatedImageSize = if (rotationDegrees % 180f == 0f) imageSize
    else androidx.compose.ui.geometry.Size(imageSize.height, imageSize.width)

    Box(
        modifier = Modifier.fillMaxSize().onSizeChanged { viewport = it }
            .pointerInput(imageUrl, rotationDegrees) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var previousCentroid = Offset.Zero
                    var previousSpan = 0f
                    var multiTouchStarted = false

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pressed = event.changes.filter { it.pressed }
                        when {
                            pressed.size >= 2 -> {
                                multiTouchStarted = true
                                val centroid = pressed.fold(Offset.Zero) { sum, change -> sum + change.position } /
                                    pressed.size.toFloat()
                                val span = pressed.sumOf { change ->
                                    val dx = change.position.x - centroid.x
                                    val dy = change.position.y - centroid.y
                                    sqrt(dx * dx + dy * dy).toDouble()
                                }.toFloat() / pressed.size
                                val zoom = if (previousSpan > 0f) span / previousSpan else 1f
                                val nextScale = (scale * zoom).coerceIn(1f, 8f)
                                val factor = nextScale / scale
                                val center = Offset(viewport.width / 2f, viewport.height / 2f)
                                val pan = centroid - previousCentroid
                                val translated = Offset(
                                    x = offset.x * factor + pan.x + (centroid.x - center.x) * (1f - factor),
                                    y = offset.y * factor + pan.y + (centroid.y - center.y) * (1f - factor)
                                )
                                scale = nextScale
                                offset = clampImageOffset(translated, nextScale, viewport, rotatedImageSize)
                                previousCentroid = centroid
                                previousSpan = span
                                pressed.forEach { it.consume() }
                            }

                            pressed.size == 1 && (scale > 1.01f || multiTouchStarted) -> {
                                val change = pressed.first()
                                val pan = change.position - previousCentroid
                                if (scale > 1.01f) {
                                    offset = clampImageOffset(offset + pan, scale, viewport, rotatedImageSize)
                                }
                                previousCentroid = change.position
                                change.consume()
                            }

                            pressed.size == 1 -> previousCentroid = pressed.first().position
                        }
                    } while (pressed.isNotEmpty())
                }
            }
            .pointerInput(imageUrl) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1.05f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                    }
                })
            }
    ) {
        val imageModifier = if (rotationDegrees % 180f != 0f && viewport.width > 0) {
            Modifier.align(Alignment.Center).requiredSize(
                width = with(density) { viewport.height.toDp() },
                height = with(density) { viewport.width.toDp() }
            )
        } else Modifier.fillMaxSize()

        Image(
            painter = painter,
            contentDescription = stringResource(R.string.article_image),
            contentScale = ContentScale.Fit,
            modifier = imageModifier.then(constraintsSizeResolver).graphicsLayer {
                transformOrigin = TransformOrigin.Center
                scaleX = scale
                scaleY = scale
                rotationZ = rotationDegrees
                translationX = offset.x
                translationY = offset.y
            }
        )
    }
}

/** Decode at 1.5x the displayed area for useful pinch zoom without retaining huge source bitmaps. */
private class ZoomableImageSizeResolver(
    private val constraintsSizeResolver: SizeResolver
) : SizeResolver {
    override suspend fun size(): Size {
        val viewportSize = constraintsSizeResolver.size()
        val width = viewportSize.width.pxOrElse { MAX_DECODED_IMAGE_SIDE }
        val height = viewportSize.height.pxOrElse { MAX_DECODED_IMAGE_SIDE }
        val multiplier = min(
            ZOOM_IMAGE_RESOLUTION_MULTIPLIER,
            MAX_DECODED_IMAGE_SIDE / max(width, height).toFloat()
        )

        return Size(
            width = (width * multiplier).roundToInt().coerceAtLeast(1),
            height = (height * multiplier).roundToInt().coerceAtLeast(1)
        )
    }

    private companion object {
        const val ZOOM_IMAGE_RESOLUTION_MULTIPLIER = 1.5f
        const val MAX_DECODED_IMAGE_SIDE = 3072
    }
}

private fun clampImageOffset(
    offset: Offset,
    scale: Float,
    viewport: IntSize,
    imageSize: androidx.compose.ui.geometry.Size
): Offset {
    if (viewport.width == 0 || viewport.height == 0 ||
        !imageSize.width.isFinite() || !imageSize.height.isFinite() ||
        imageSize.width <= 0f || imageSize.height <= 0f
    ) return offset

    val fit = min(viewport.width / imageSize.width, viewport.height / imageSize.height)
    val renderedWidth = imageSize.width * fit
    val renderedHeight = imageSize.height * fit
    val maxX = max(0f, (renderedWidth * scale - viewport.width) / 2f)
    val maxY = max(0f, (renderedHeight * scale - viewport.height) / 2f)
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}
