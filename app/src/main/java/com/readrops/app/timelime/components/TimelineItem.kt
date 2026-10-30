package com.readrops.app.timelime.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.abs
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

// past the threshold, the card keeps following the finger but with growing resistance
private fun rubberBand(rawOffset: Float, threshold: Float, maxDistance: Float): Float {
    val distance = abs(rawOffset)
    if (distance <= threshold || maxDistance <= threshold) return rawOffset

    val extra = maxDistance - threshold
    val overshoot = distance - threshold
    val damped = threshold + extra * (1f - 1f / (overshoot / extra + 1f))
    return if (rawOffset < 0) -damped else damped
}

@Composable
private fun SwipeBackground(
    action: SwipeAction,
    isRead: Boolean,
    isStarred: Boolean,
    progress: Float,
    armed: Boolean,
    alignment: Alignment,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (armed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        label = "swipeBackgroundColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (armed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
        label = "swipeIconColor"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (armed) 1.2f else 0.6f + 0.4f * progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "swipeIconScale"
    )

    Box(
        contentAlignment = alignment,
        modifier = modifier.background(color)
    ) {
        Icon(
            painter = painterResource(
                when {
                    action == SwipeAction.FAVORITE && isStarred -> R.drawable.ic_star_outline
                    action == SwipeAction.FAVORITE -> R.drawable.ic_star
                    isRead -> R.drawable.ic_state_unread
                    else -> R.drawable.ic_done_all
                }
            ),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .size(28.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = min(1f, progress * 1.5f)
                }
        )
    }
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

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    // the card always springs back to its place: there is no dismissed state that could get stuck
    val offset = remember(itemWithFeed.item.id) { Animatable(0f) }
    var rawDrag by remember(itemWithFeed.item.id) { mutableFloatStateOf(0f) }
    var armed by remember(itemWithFeed.item.id) { mutableStateOf(false) }

    val threshold = timelineSwipeThreshold(
        totalDistance = containerWidth,
        minimumDistance = with(density) { 80.dp.toPx() }
    )
    val maxDistance = containerWidth * 0.6f

    val dragState = rememberDraggableState { delta ->
        val proposed = rawDrag + delta
        rawDrag = when {
            proposed > 0 && swipeToRight == SwipeAction.DISABLED -> 0f
            proposed < 0 && swipeToLeft == SwipeAction.DISABLED -> 0f
            else -> proposed
        }

        val nowArmed = threshold > 0f && abs(rawDrag) >= threshold
        if (nowArmed != armed) {
            armed = nowArmed
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        scope.launch { offset.snapTo(rubberBand(rawDrag, threshold, maxDistance)) }
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerWidth = it.width.toFloat() }
            .then(if (swipeEnabled) Modifier.draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    val action = when {
                        !armed -> null
                        rawDrag > 0 -> swipeToRight
                        else -> swipeToLeft
                    }
                    rawDrag = 0f
                    armed = false
                    action?.let { swipeHandler(it) }

                    offset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            ) else Modifier),
    ) {
        if (offset.value != 0f) {
            val swipingRight = offset.value > 0
            SwipeBackground(
                action = if (swipingRight) swipeToRight else swipeToLeft,
                isRead = itemWithFeed.isRead,
                isStarred = itemWithFeed.isStarred,
                progress = if (threshold > 0f) min(1f, abs(offset.value) / threshold) else 0f,
                armed = armed,
                alignment = if (swipingRight) Alignment.CenterStart else Alignment.CenterEnd,
                // follow the card's shape so the background doesn't bleed past its margins and corners
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (size == TimelineItemSize.COMPACT) Modifier else Modifier
                            .padding(horizontal = MaterialTheme.spacing.shortSpacing)
                            .clip(RoundedCornerShape(12.dp))
                    )
            )
        }

        val cardModifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offset.value.roundToInt(), 0) }

        CompositionLocalProvider(LocalTimelineItemSelected provides isSelected) {
            when (size) {
                TimelineItemSize.COMPACT -> {
                    CompactTimelineItem(
                        itemWithFeed = itemWithFeed,
                        onClick = onClick,
                        onFavorite = onFavorite,
                        onShare = onShare,
                        modifier = cardModifier,
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
                        modifier = cardModifier,
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
                        modifier = cardModifier,
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
