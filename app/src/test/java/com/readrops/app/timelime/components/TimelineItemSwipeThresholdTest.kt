package com.readrops.app.timelime.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineItemSwipeThresholdTest {
    @Test
    fun wideArticleUsesBoundedSwipeThreshold() {
        assertEquals(100f, timelineSwipeThreshold(totalDistance = 400f, minimumDistance = 80f), 0.001f)
    }

    @Test
    fun narrowArticleThresholdNeverExceedsItsWidth() {
        assertEquals(30f, timelineSwipeThreshold(totalDistance = 100f, minimumDistance = 80f), 0.001f)
        assertEquals(15f, timelineSwipeThreshold(totalDistance = 50f, minimumDistance = 80f), 0.001f)
    }
}
