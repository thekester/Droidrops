package com.readrops.app.item

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomBarLayoutTest {
    @Test
    fun fullyHiddenScrollableBarReleasesItsScaffoldSlot() {
        assertEquals(0.dp, visibleBottomBarSlotHeight(64.dp, -64.dp, isScrollable = true))
    }

    @Test
    fun partiallyHiddenBarReservesOnlyItsVisibleHeight() {
        assertEquals(40.dp, visibleBottomBarSlotHeight(64.dp, -24.dp, isScrollable = true))
    }

    @Test
    fun nonScrollableArticleKeepsTheFullActionBarSlot() {
        assertEquals(64.dp, visibleBottomBarSlotHeight(64.dp, -64.dp, isScrollable = false))
    }
}
