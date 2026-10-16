package com.readrops.app.timelime

import android.content.Context
import com.readrops.db.filters.MainFilter
import com.readrops.db.filters.QueryFilters
import com.readrops.db.filters.SubFilter

/** Keeps the default timeline position across app restarts, separately for each account. */
internal class TimelineScrollPosition(context: Context, accountId: Int) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val indexKey = "account_${accountId}_index"
    private val offsetKey = "account_${accountId}_offset"

    fun read(): Position = Position(
        itemIndex = preferences.getInt(indexKey, 0).coerceAtLeast(0),
        itemOffset = preferences.getInt(offsetKey, 0).coerceAtLeast(0)
    )

    fun write(itemIndex: Int, itemOffset: Int) {
        preferences.edit()
            .putInt(indexKey, itemIndex.coerceAtLeast(0))
            .putInt(offsetKey, itemOffset.coerceAtLeast(0))
            .apply()
    }

    data class Position(val itemIndex: Int, val itemOffset: Int)

    private companion object {
        const val PREFERENCES_NAME = "timeline_scroll_positions"
    }
}

internal fun QueryFilters.isDefaultTimelinePositionScope(): Boolean =
    mainFilter == MainFilter.ALL &&
        subFilter == SubFilter.ALL &&
        feedId == 0 && folderId == 0 && tagId == 0 &&
        searchQuery.isBlank() && !recentlyReadOnly
