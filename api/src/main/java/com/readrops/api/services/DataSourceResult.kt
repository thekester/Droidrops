package com.readrops.api.services

import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.Tag

data class DataSourceResult(
    var items: List<Item> = mutableListOf(),
    var starredItems: List<Item> = mutableListOf(),
    var feeds: List<Feed> = listOf(),
    var folders: List<Folder> = listOf(),
    var unreadIds: List<String> = listOf(),
    var readIds: List<String> = listOf(),
    var starredIds: List<String> = listOf(),
    var tags: List<Tag> = listOf(),
    /** Cursor for resuming a bounded initial import, used by Miniflux. */
    var syncCursor: String? = null,
    /** Timestamp to use for incremental sync after the initial import is complete. */
    var initialSyncTimestamp: Long? = null
)
