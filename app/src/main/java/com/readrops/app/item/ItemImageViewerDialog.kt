package com.readrops.app.item

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberAsyncImagePainter
import com.readrops.app.R
import kotlin.math.max
import kotlin.math.min

@Composable
fun ItemImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    var viewport by remember(imageUrl) { mutableStateOf(IntSize.Zero) }
    val painter = rememberAsyncImagePainter(imageUrl)
    val imageSize = painter.intrinsicSize

    fun resetZoom() {
        scale = 1f
        offset = Offset.Zero
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { viewport = it }
                .pointerInput(imageUrl) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 8f)
                        val factor = nextScale / scale
                        val center = Offset(viewport.width / 2f, viewport.height / 2f)
                        val translated = Offset(
                            x = offset.x * factor + pan.x + (centroid.x - center.x) * (1f - factor),
                            y = offset.y * factor + pan.y + (centroid.y - center.y) * (1f - factor)
                        )
                        scale = nextScale
                        offset = clampImageOffset(translated, nextScale, viewport, imageSize)
                    }
                }
                .pointerInput(imageUrl) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.05f) {
                                resetZoom()
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
        ) {
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.article_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin.Center
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_image_viewer),
                    tint = Color.White
                )
            }
        }
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
    ) {
        return offset
    }

    val fit = min(viewport.width / imageSize.width, viewport.height / imageSize.height)
    val renderedWidth = imageSize.width * fit
    val renderedHeight = imageSize.height * fit
    val maxX = max(0f, (renderedWidth * scale - viewport.width) / 2f)
    val maxY = max(0f, (renderedHeight * scale - viewport.height) / 2f)

    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}
