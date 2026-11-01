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
    fun savedItemIdTracksItsPositionAfterNewArticlesAreInsertedTest() {
        val position = TimelineScrollPosition.Position(itemIndex = 42, itemOffset = 96, itemId = 123)

        assertEquals(1, position.resolveIndex(listOf(456, 123, 789)))
        assertEquals(2, position.resolveIndex(listOf(456, 789, 999)))
        assertEquals(0, position.resolveIndex(emptyList()))
    }

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
            TimelineScrollPosition(context, accountId = 7)
                .write(itemIndex = 42, itemOffset = 96, itemId = 123)

            assertEquals(
                TimelineScrollPosition.Position(itemIndex = 42, itemOffset = 96, itemId = 123),
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
