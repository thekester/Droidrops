package com.readrops.app.timelime

import com.readrops.db.entities.OpenIn

internal enum class TimelineOpenAction {
    LOCAL_ARTICLE,
    EXTERNAL_BROWSER,
    CUSTOM_TAB
}

internal fun resolveTimelineOpenAction(
    feedOpenIn: OpenIn?,
    hasArticleLink: Boolean,
    preferExternalBrowser: Boolean
): TimelineOpenAction = when {
    feedOpenIn == OpenIn.LOCAL_VIEW || !hasArticleLink -> TimelineOpenAction.LOCAL_ARTICLE
    feedOpenIn == OpenIn.EXTERNAL_VIEW || preferExternalBrowser -> TimelineOpenAction.EXTERNAL_BROWSER
    else -> TimelineOpenAction.CUSTOM_TAB
}
