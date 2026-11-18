package com.readrops.app.account

import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder

/** OPML documents may contain empty folders before folders that have subscriptions. */
internal fun Map<Folder?, List<Feed>>.firstImportableFeedOrNull(): Feed? {
    for (feeds in values) {
        feeds.firstOrNull()?.let { return it }
    }

    return null
}

internal fun Map<Folder?, List<Feed>>.hasImportableFeeds(): Boolean =
    firstImportableFeedOrNull() != null
