package com.readrops.app.item

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemPaneTest {

    private fun layout(enabled: Boolean = true, width: Int, height: Int, smallest: Int = minOf(width, height)) =
        twoPaneLayout(enabled, width, height, smallest)

    @Test
    fun landscapeTabletsShowPanesSideBySideFromTheExpandedBreakpoint() {
        assertEquals(TwoPaneLayout.NONE, layout(width = 839, height = 700))
        assertEquals(TwoPaneLayout.SIDE_BY_SIDE, layout(width = 840, height = 700))
        assertEquals(TwoPaneLayout.SIDE_BY_SIDE, layout(width = 1280, height = 800))
    }

    @Test
    fun portraitTabletsStackThePanes() {
        assertEquals(TwoPaneLayout.STACKED, layout(width = 800, height = 1280))
        assertEquals(TwoPaneLayout.NONE, layout(width = 700, height = 839))
    }

    @Test
    fun twoPaneIsOptIn() {
        assertEquals(TwoPaneLayout.NONE, layout(enabled = false, width = 1280, height = 800))
        assertEquals(TwoPaneLayout.NONE, layout(enabled = false, width = 800, height = 1280))
    }

    @Test
    fun phonesKeepTheFullScreenReaderInBothOrientations() {
        assertEquals(TwoPaneLayout.NONE, layout(width = 411, height = 923))
        // Pixel 9 in landscape: wide enough, but only 411 dp high
        assertEquals(TwoPaneLayout.NONE, layout(width = 923, height = 411))
    }
}
