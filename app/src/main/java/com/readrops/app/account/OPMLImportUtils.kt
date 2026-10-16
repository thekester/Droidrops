package com.readrops.app.account

import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder

/** OPML documents may contain folders but no subscriptions. */
internal fun Map<Folder?, List<Feed>>.hasImportableFeeds(): Boolean =
    values.any { feeds -> feeds.isNotEmpty() }
