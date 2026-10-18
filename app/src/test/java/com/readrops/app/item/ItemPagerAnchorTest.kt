package com.readrops.app.item

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemPagerAnchorTest {
    @Test
    fun selectedItemsInFirstPageStartAtOffsetZero() {
        assertEquals(ItemPagerAnchor(0), itemPagerAnchor(0, pageSize = 50))
        assertEquals(ItemPagerAnchor(0), itemPagerAnchor(49, pageSize = 50))
    }

    @Test
    fun selectedItemsInLaterPagesStartAtTheirPageOffset() {
        assertEquals(ItemPagerAnchor(50), itemPagerAnchor(50, pageSize = 50))
        assertEquals(ItemPagerAnchor(50), itemPagerAnchor(79, pageSize = 50))
        assertEquals(ItemPagerAnchor(950), itemPagerAnchor(999, pageSize = 50))
    }

    @Test
    fun missingTimelineIndexStartsAtTheFirstArticle() {
        assertEquals(ItemPagerAnchor(0), itemPagerAnchor(-1, pageSize = 50))
    }

    @Test
    fun maximumTimelineIndexStillStartsAtABoundedPageBoundary() {
        val anchor = itemPagerAnchor(Int.MAX_VALUE, pageSize = 50)

        assertEquals(2_147_483_600, anchor.initialKey)
        assertEquals(0, anchor.initialKey % 50)
        assert(anchor.initialKey <= Int.MAX_VALUE)
    }
}
