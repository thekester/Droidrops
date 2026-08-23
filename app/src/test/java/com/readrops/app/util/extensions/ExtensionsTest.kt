package com.readrops.app.util.extensions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsTest {

    @Test
    fun acceptsSelfHostedFeedUrls() {
        assertTrue("http://nas/feed.xml".isValidFeedUrl())
        assertTrue("http://localhost:8080/feed".isValidFeedUrl())
        assertTrue("https://example.com/feed".isValidFeedUrl())
    }

    @Test
    fun rejectsMalformedOrUnsupportedUrls() {
        assertFalse("not a url".isValidFeedUrl())
        assertFalse("ftp://example.com/feed".isValidFeedUrl())
        assertFalse("https://".isValidFeedUrl())
    }
}
