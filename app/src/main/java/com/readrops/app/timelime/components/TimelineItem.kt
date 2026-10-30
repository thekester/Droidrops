package com.readrops.app.timelime.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.readrops.app.R
import com.readrops.app.util.DefaultPreview
import com.readrops.app.util.theme.ReadropsTheme
import com.readrops.app.util.theme.spacing
import com.readrops.db.entities.Folder
import com.readrops.db.entities.OpenIn
import com.readrops.db.entities.Tag
import com.readrops.db.pojo.ItemWithFeed
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

    fun handleSwipeAction(swipeAction: SwipeAction) {
        when (swipeAction) {
            SwipeAction.READ -> onSetReadState()
            SwipeAction.FAVORITE -> onFavorite()
            else -> {}
        }
    }

    val density = LocalDensity.current
    var dragOffset by remember(itemWithFeed.item.id) { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    val maximumSwipeOffset = with(density) { 80.dp.toPx() }
    val dragState = rememberDraggableState { delta ->
        val dragLimit = min(containerWidth * 0.3f, maximumSwipeOffset)
        dragOffset = (dragOffset + delta).coerceIn(-dragLimit, dragLimit)
    }

    fun getSwipeIcon(swipeAction: SwipeAction): Int {
        return if (swipeAction == SwipeAction.READ) {
            if (itemWithFeed.isRead) {
                R.drawable.ic_state_unread
            } else {
                R.drawable.ic_state_read
            }
        } else {
            if (itemWithFeed.isStarred) {
                R.drawable.ic_star_outline
            } else {
                R.drawable.ic_star
            }
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerWidth = it.width.toFloat() }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = swipeToLeft != SwipeAction.DISABLED || swipeToRight != SwipeAction.DISABLED,
                onDragStopped = {
                    val threshold = min(
                        timelineSwipeThreshold(
                            totalDistance = containerWidth,
                            minimumDistance = maximumSwipeOffset
                        ),
                        maximumSwipeOffset
                    )
                    val action = when {
                        dragOffset <= -threshold && swipeToLeft != SwipeAction.DISABLED -> swipeToLeft
                        dragOffset >= threshold && swipeToRight != SwipeAction.DISABLED -> swipeToRight
                        else -> null
                    }
                    action?.let(::handleSwipeAction)
                    dragOffset = 0f
                }
            ),
    ) {
        val direction = when {
            dragOffset < 0f -> -1
            dragOffset > 0f -> 1
            else -> 0
        }
        val activeAction = when (direction) {
            -1 -> swipeToLeft
            1 -> swipeToRight
            else -> SwipeAction.DISABLED
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (size == TimelineItemSize.COMPACT) Modifier else Modifier
                    .padding(horizontal = MaterialTheme.spacing.shortSpacing)
                    .clip(CardDefaults.shape))
                .background(if (direction == 0 || activeAction == SwipeAction.DISABLED) Color.Transparent else MaterialTheme.colorScheme.primary),
            contentAlignment = if (direction < 0) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            if (direction != 0 && activeAction != SwipeAction.DISABLED) {
                Icon(
                    painter = painterResource(id = getSwipeIcon(activeAction)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.minimumInteractiveComponentSize().padding(horizontal = MaterialTheme.spacing.mediumSpacing)
                )
            }
        }

        Box(modifier = Modifier.offset { IntOffset(dragOffset.roundToInt(), 0) }) {
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
