package com.readrops.app.timelime.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.readrops.app.util.DefaultPreview
import com.readrops.app.util.theme.ReadropsTheme
import com.readrops.db.entities.Folder
import com.readrops.db.entities.OpenIn
import com.readrops.db.entities.Tag
import com.readrops.db.pojo.ItemWithFeed
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

enum class TimelineItemSize {
    COMPACT,
    REGULAR,
    LARGE
}

enum class SwipeAction {
    READ,
    FAVORITE,
    DISABLED
}

const val readAlpha = 0.6f

internal fun timelineSwipeThreshold(totalDistance: Float, minimumDistance: Float): Float {
    if (totalDistance <= 0f) return 0f

    return min(totalDistance * 0.3f, max(totalDistance * 0.25f, minimumDistance))
}


@Composable
fun TimelineItem(
    itemWithFeed: ItemWithFeed,
    swipeToLeft: SwipeAction,
    swipeToRight: SwipeAction,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onSetReadState: () -> Unit,
    modifier: Modifier = Modifier,
    size: TimelineItemSize = TimelineItemSize.LARGE,
    showImages: Boolean = true,
    showSource: Boolean = true,
    showDate: Boolean = true,
    showReadTime: Boolean = true,
    showPreview: Boolean = true,
    isSelected: Boolean = false
) {

    val density = LocalDensity.current
    var containerWidth by remember { mutableFloatStateOf(0f) }
    val swipeHandler by rememberUpdatedState(newValue = { action: SwipeAction ->
        when (action) {
            SwipeAction.READ -> onSetReadState()
            SwipeAction.FAVORITE -> onFavorite()
            SwipeAction.DISABLED -> Unit
        }
    })
    val swipeEnabled = swipeToLeft != SwipeAction.DISABLED || swipeToRight != SwipeAction.DISABLED

    Box(
        modifier = modifier
            .onSizeChanged { containerWidth = it.width.toFloat() }
            .then(if (swipeEnabled) Modifier.pointerInput(
                itemWithFeed.item.id,
                swipeToLeft,
                swipeToRight,
                containerWidth
            ) {
                var dragDistance = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> dragDistance += dragAmount },
                    onDragEnd = {
                        val threshold = timelineSwipeThreshold(
                            totalDistance = containerWidth,
                            minimumDistance = with(density) { 80.dp.toPx() }
                        )
                        when {
                            dragDistance <= -threshold && swipeToLeft != SwipeAction.DISABLED -> swipeHandler(swipeToLeft)
                            dragDistance >= threshold && swipeToRight != SwipeAction.DISABLED -> swipeHandler(swipeToRight)
                        }
                        dragDistance = 0f
                    },
                    onDragCancel = { dragDistance = 0f }
                )
            } else Modifier),
    ) {
        CompositionLocalProvider(LocalTimelineItemSelected provides isSelected) {
            when (size) {
                TimelineItemSize.COMPACT -> {
                    CompactTimelineItem(
                        itemWithFeed = itemWithFeed,
                        onClick = onClick,
                        onFavorite = onFavorite,
                        onShare = onShare,
                        modifier = Modifier.fillMaxWidth(),
                        showImages = showImages,
                        showSource = showSource,
                        showDate = showDate,
                        showReadTime = showReadTime
                    )
                }

                TimelineItemSize.REGULAR -> {
                    RegularTimelineItem(
                        itemWithFeed = itemWithFeed,
                        onClick = onClick,
                        onFavorite = onFavorite,
                        onShare = onShare,
                        modifier = Modifier.fillMaxWidth(),
                        showImages = showImages,
                        showSource = showSource,
                        showDate = showDate,
                        showReadTime = showReadTime
                    )
                }

                TimelineItemSize.LARGE -> {
                    LargeTimelineItem(
                        itemWithFeed = itemWithFeed,
                        onClick = onClick,
                        onFavorite = onFavorite,
                        onShare = onShare,
                        modifier = Modifier.fillMaxWidth(),
                        showImages = showImages,
                        showSource = showSource,
                        showDate = showDate,
                        showReadTime = showReadTime,
                        showPreview = showPreview
                    )
                }
            }
        }
    }
}

val itemWithFeed = ItemWithFeed(
    item = com.readrops.db.entities.Item(
        title = "This is a not so long item title",
        pubDate = LocalDateTime.now(),
        cleanDescription = """Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                        Donec a tortor neque. Nam ultrices, diam ac congue finibus, tortor sem congue urna,
                         at finibus elit libero at mi. Etiam hendrerit sapien eu porta feugiat. Duis porttitor"""
            .replace("\n", "")
            .trimMargin(),
        imageLink = "",
        tags = listOf(
            Tag(name = "Tag 1"),
            Tag(name = "Tag 2"),
            Tag(name = "Tag 3"),
            Tag(name = "Tag 4"),
            Tag(name = "Tag 5"),
            Tag(name = "Tag 6"),
            Tag(name = "Tag 7"),
            Tag(name = "Tag 8"),
            Tag(name = "Tag 9")
        )
    ),
    feedName = "feed name",
    color = 0,
    feedId = 0,
    feedIconUrl = "",
    websiteUrl = "",
    folder = Folder(name = "Folder name"),
    openIn = OpenIn.LOCAL_VIEW,
    openInAsk = true,
)

@DefaultPreview
@Composable
private fun RegularTimelineItemPreview() {
    ReadropsTheme {
        RegularTimelineItem(
            itemWithFeed = itemWithFeed,
            onClick = {},
            onFavorite = {},
            onShare = {},
        )
    }
}

@DefaultPreview
@Composable
private fun CompactTimelineItemPreview() {
    ReadropsTheme {
        CompactTimelineItem(
            itemWithFeed = itemWithFeed,
            onClick = {},
            onFavorite = {},
            onShare = {},
        )
    }
}

@DefaultPreview
@Composable
private fun LargeTimelineItemPreview() {
    ReadropsTheme {
        LargeTimelineItem(
            itemWithFeed = itemWithFeed,
            onClick = {},
            onFavorite = {},
            onShare = {},
        )
    }
}
