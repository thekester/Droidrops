package com.readrops.db.pojo

import androidx.room.ColumnInfo

/** The small set of stored fields needed to update an existing remote item safely. */
data class ExistingItem(
    val id: Int,
    @ColumnInfo(name = "remote_id") val remoteId: String,
    @ColumnInfo(name = "feed_id") val feedId: Int,
    @ColumnInfo(name = "read") val isRead: Boolean,
    @ColumnInfo(name = "starred") val isStarred: Boolean
)
