package com.readrops.app.util.extensions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletWindowTest {
    @Test
    fun usesTabletLayoutAtTheMediumWindowBreakpoint() {
        assertFalse(isTabletWindow(599))
        assertTrue(isTabletWindow(600))
        assertTrue(isTabletWindow(719))
        assertTrue(isTabletWindow(720))
    }
}
