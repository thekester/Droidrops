package com.readrops.app.account

import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OPMLImportUtilsTest {
    @Test
    fun folderOnlyDocumentHasNoImportableFeeds() {
        val opml: Map<Folder?, List<Feed>> = mapOf(Folder(name = "Empty folder") to emptyList())
        assertFalse(opml.hasImportableFeeds())
    }

    @Test
    fun documentWithAtLeastOneFeedCanBeImported() {
        val opml: Map<Folder?, List<Feed>> =
            mapOf(null to listOf(Feed(name = "Example", url = "https://example.org/feed")))
        assertTrue(opml.hasImportableFeeds())
    }
}
