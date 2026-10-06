package com.readrops.app.util.extensions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MouseWheelRefreshDetectorTest {

    @Test
    fun downwardWheelMovementAtTopTriggersAfterThreshold() {
        val detector = MouseWheelRefreshDetector(threshold = 2f)

        assertFalse(detector.onScroll(verticalScroll = 1f, canScrollBackward = false))
        assertTrue(detector.onScroll(verticalScroll = 1f, canScrollBackward = false))
    }

    @Test
    fun upwardMovementResetsAccumulatedDownwardMovement() {
        val detector = MouseWheelRefreshDetector(threshold = 2f)

        assertFalse(detector.onScroll(verticalScroll = 1f, canScrollBackward = false))
        assertFalse(detector.onScroll(verticalScroll = -1f, canScrollBackward = false))
        assertFalse(detector.onScroll(verticalScroll = 1f, canScrollBackward = false))
    }

    @Test
    fun movementWhileListCanScrollBackwardDoesNotTriggerRefresh() {
        val detector = MouseWheelRefreshDetector(threshold = 2f)

        assertFalse(detector.onScroll(verticalScroll = 3f, canScrollBackward = true))
        assertFalse(detector.onScroll(verticalScroll = 1f, canScrollBackward = false))
    }

    @Test
    fun oneContinuousWheelGestureTriggersOnlyOnce() {
        val detector = MouseWheelRefreshDetector(threshold = 2f)

        assertTrue(detector.onScroll(verticalScroll = 2f, canScrollBackward = false))
        assertFalse(detector.onScroll(verticalScroll = 2f, canScrollBackward = false))
        assertFalse(detector.onScroll(verticalScroll = -1f, canScrollBackward = false))
        assertTrue(detector.onScroll(verticalScroll = 2f, canScrollBackward = false))
    }
}
