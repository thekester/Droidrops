package com.readrops.app.timelime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.readrops.db.filters.MainFilter
import com.readrops.db.filters.QueryFilters
import com.readrops.db.filters.SubFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineScrollPositionTest {

    @Test
    fun onlyTheUnfilteredTimelineUsesTheSavedPositionTest() {
        assertEquals(true, QueryFilters(accountId = 7).isDefaultTimelinePositionScope())
        assertEquals(
            false,
            QueryFilters(accountId = 7, mainFilter = MainFilter.NEW).isDefaultTimelinePositionScope()
        )
        assertEquals(
            false,
            QueryFilters(accountId = 7, subFilter = SubFilter.FEED, feedId = 3)
                .isDefaultTimelinePositionScope()
        )
        assertEquals(
            false,
            QueryFilters(accountId = 7, searchQuery = "term").isDefaultTimelinePositionScope()
        )
    }

    @Test
    fun timelinePositionSurvivesRecreationAndIsScopedToAccountTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteSharedPreferences("timeline_scroll_positions")

        try {
            TimelineScrollPosition(context, accountId = 7).write(itemIndex = 42, itemOffset = 96)

            assertEquals(
                TimelineScrollPosition.Position(itemIndex = 42, itemOffset = 96),
                TimelineScrollPosition(context, accountId = 7).read()
            )
            assertEquals(
                TimelineScrollPosition.Position(itemIndex = 0, itemOffset = 0),
                TimelineScrollPosition(context, accountId = 8).read()
            )
        } finally {
            context.deleteSharedPreferences("timeline_scroll_positions")
        }
    }
}
