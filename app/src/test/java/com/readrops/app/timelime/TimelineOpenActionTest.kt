package com.readrops.app.timelime

import com.readrops.db.entities.OpenIn
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineOpenActionTest {

    @Test
    fun feedExternalBrowserSettingOverridesGlobalCustomTabPreference() {
        assertEquals(
            TimelineOpenAction.EXTERNAL_BROWSER,
            resolveTimelineOpenAction(OpenIn.EXTERNAL_VIEW, hasArticleLink = true, preferExternalBrowser = false)
        )
    }

    @Test
    fun articlesWithoutLinksOpenInTheLocalReader() {
        assertEquals(
            TimelineOpenAction.LOCAL_ARTICLE,
            resolveTimelineOpenAction(OpenIn.EXTERNAL_VIEW, hasArticleLink = false, preferExternalBrowser = true)
        )
    }

    @Test
    fun defaultOpeningPreferenceStillSelectsCustomTab() {
        assertEquals(
            TimelineOpenAction.CUSTOM_TAB,
            resolveTimelineOpenAction(null, hasArticleLink = true, preferExternalBrowser = false)
        )
    }
}
