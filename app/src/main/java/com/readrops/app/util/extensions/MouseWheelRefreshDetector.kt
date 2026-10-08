package com.readrops.app.util.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Accumulates downward mouse-wheel movement while a list is at its start and requests a refresh
 * once the movement reaches [threshold]. Android reports downward wheel movement as a positive
 * vertical scroll value.
 */
internal class MouseWheelRefreshDetector(
    private val threshold: Float = 2f
) {
    private var accumulatedDownwardScroll = 0f
    private var refreshTriggered = false

    fun onScroll(verticalScroll: Float, canScrollBackward: Boolean): Boolean {
        if (canScrollBackward || verticalScroll <= 0f) {
            reset()
            return false
        }

        if (refreshTriggered) return false

        accumulatedDownwardScroll += verticalScroll
        if (accumulatedDownwardScroll < threshold) return false

        accumulatedDownwardScroll = 0f
        refreshTriggered = true
        return true
    }

    fun resetAccumulatedScroll() {
        accumulatedDownwardScroll = 0f
    }

    fun reset() {
        resetAccumulatedScroll()
        refreshTriggered = false
    }
}

internal fun Modifier.mouseWheelPullToRefresh(
    canScrollBackward: Boolean,
    isRefreshing: Boolean,
    detector: MouseWheelRefreshDetector,
    onRefresh: () -> Unit
): Modifier = pointerInput(canScrollBackward, isRefreshing, detector, onRefresh) {
    if (isRefreshing) detector.reset()

    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Scroll) continue

            val change = event.changes.firstOrNull() ?: continue
            val verticalScroll = change.scrollDelta.y

            if (isRefreshing) {
                // A completed wheel-triggered refresh must allow the next gesture to refresh too.
                detector.reset()
            } else if (!canScrollBackward && verticalScroll > 0f) {
                if (detector.onScroll(verticalScroll, canScrollBackward = false)) {
                    onRefresh()
                }

                // At the top, consume downward wheel input so PullToRefreshBox cannot leave its
                // touch-based pull indicator stretched after the mouse event has finished.
                change.consume()
            } else {
                detector.onScroll(verticalScroll, canScrollBackward)
            }
        }
    }
}
