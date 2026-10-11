package com.readrops.app.timelime.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineItemSwipeThresholdTest {
    @Test
    fun wideArticleRequiresSwipeAcrossAlmostHalfItsWidth() {
        assertEquals(180f, timelineSwipeThreshold(totalDistance = 400f, minimumDistance = 80f), 0.001f)
    }

    @Test
    fun narrowArticleUsesMinimumThresholdWithoutExceedingItsWidth() {
        assertEquals(80f, timelineSwipeThreshold(totalDistance = 100f, minimumDistance = 80f), 0.001f)
        assertEquals(40f, timelineSwipeThreshold(totalDistance = 50f, minimumDistance = 80f), 0.001f)
    }
}
