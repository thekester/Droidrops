package com.readrops.app.account.backup

import com.readrops.app.account.backup.LocalAccountBackupFormat.InvalidBackupException
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.OpenIn
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class LocalAccountBackupFormatTest {

    private val folder = Folder(id = 3, name = "Tech", accountId = 1)
    private val feed = Feed(
        id = 7,
        name = "Example",
        url = "https://example.org/feed.xml",
        siteUrl = "https://example.org",
        color = 0xFF336699.toInt(),
        folderId = 3,
        accountId = 1,
        etag = "\"cached\"",
        lastModified = "Tue, 01 Sep 2026 10:00:00 GMT",
        isNotificationEnabled = false,
        openIn = OpenIn.EXTERNAL_VIEW,
        openInAsk = false,
    )
    private val item = Item(
        id = 42,
        title = "Hello",
        content = "<p>Body with \"quotes\" and é</p>",
        link = "https://example.org/hello",
        pubDate = LocalDateTime.of(2026, 10, 1, 8, 30),
        feedId = 7,
        readTime = 2.5,
        isRead = true,
        isStarred = true,
        remoteId = "guid-42",
    )

    private fun backup(items: List<List<Item>> = listOf(listOf(item))): Buffer {
        val buffer = Buffer()
        LocalAccountBackupFormat.Writer(buffer).use { writer ->
            writer.writeHeader(
                LocalAccountBackupFormat.Header(
                    accountName = "My feeds",
                    notificationsEnabled = true,
                    folders = listOf(folder),
                    feeds = listOf(feed),
                )
            )
            items.forEach(writer::writeItems)
            writer.finish()
        }
        return buffer
    }

    @Test
    fun roundTripKeepsFoldersFeedsArticlesAndTheirState() = runTest {
        var header: LocalAccountBackupFormat.Header? = null
        val items = mutableListOf<Item>()

        LocalAccountBackupFormat.read(
            source = backup(),
            onHeader = { header = it },
            onItems = { items += it }
        )

        assertEquals("My feeds", header!!.accountName)
        assertTrue(header!!.notificationsEnabled)
        assertEquals(listOf(3 to "Tech"), header!!.folders.map { it.id to it.name })

        val restoredFeed = header!!.feeds.single()
        assertEquals(feed.url, restoredFeed.url)
        assertEquals(feed.color, restoredFeed.color)
        assertEquals(3, restoredFeed.folderId)
        assertFalse(restoredFeed.isNotificationEnabled)
        assertEquals(OpenIn.EXTERNAL_VIEW, restoredFeed.openIn)
        assertFalse(restoredFeed.openInAsk)

        val restoredItem = items.single()
        assertEquals(item.title, restoredItem.title)
        assertEquals(item.content, restoredItem.content)
        assertEquals(item.pubDate, restoredItem.pubDate)
        assertEquals(item.remoteId, restoredItem.remoteId)
        assertEquals(7, restoredItem.feedId)
        assertTrue(restoredItem.isRead)
        assertTrue(restoredItem.isStarred)
        assertEquals(2.5, restoredItem.readTime, 0.0)
    }

    @Test
    fun cacheHeadersAreNotExported() = runTest {
        val json = backup().readUtf8()

        assertFalse(json.contains("cached"))
        assertFalse(json.contains("etag"))
        assertFalse(json.contains("token", ignoreCase = true))
        assertFalse(json.contains("password", ignoreCase = true))

        var header: LocalAccountBackupFormat.Header? = null
        LocalAccountBackupFormat.read(Buffer().writeUtf8(json), { header = it }, {})
        assertNull(header!!.feeds.single().etag)
    }

    @Test
    fun accountTypeFullArticleFeedsAndSettingsRoundTrip() = runTest {
        val buffer = Buffer()
        LocalAccountBackupFormat.Writer(buffer).use { writer ->
            writer.writeHeader(
                LocalAccountBackupFormat.Header(
                    accountName = "Server",
                    notificationsEnabled = false,
                    folders = listOf(folder),
                    feeds = listOf(feed, feed.copy(id = 8, url = "https://example.org/other.xml")),
                    sourceAccountType = "FRESHRSS",
                    fullContentFeedIds = setOf(8),
                    preferences = listOf(
                        BackupPreference("theme", BackupPreference.Type.STRING, "dark"),
                        BackupPreference("max_items_per_feed", BackupPreference.Type.INT, 500),
                        BackupPreference("tags", BackupPreference.Type.STRING_SET, setOf("a", "b")),
                    )
                )
            )
            writer.finish()
        }

        var header: LocalAccountBackupFormat.Header? = null
        LocalAccountBackupFormat.read(buffer, { header = it }, {})

        assertEquals("FRESHRSS", header!!.sourceAccountType)
        assertEquals(setOf(8), header!!.fullContentFeedIds)
        assertEquals(
            listOf(
                BackupPreference("theme", BackupPreference.Type.STRING, "dark"),
                BackupPreference("max_items_per_feed", BackupPreference.Type.INT, 500),
                BackupPreference("tags", BackupPreference.Type.STRING_SET, setOf("a", "b")),
            ),
            header!!.preferences
        )
    }

    @Test
    fun firstVersionBackupsWithoutNewFieldsStillRestore() = runTest {
        var header: LocalAccountBackupFormat.Header? = null
        LocalAccountBackupFormat.read(
            Buffer().writeUtf8(
                """{"format":"${LocalAccountBackupFormat.FORMAT}","version":1,
                "account":{"name":"Old","notificationsEnabled":true},"folders":[],
                "feeds":[{"id":1,"url":"https://example.org/feed.xml"}],"items":[]}"""
            ),
            { header = it }, {}
        )

        assertNull(header!!.sourceAccountType)
        assertTrue(header!!.fullContentFeedIds.isEmpty())
        assertTrue(header!!.preferences.isEmpty())
    }

    @Test
    fun articlesAreDeliveredInBatches() = runTest {
        val many = (1..5).map { item.copy(id = it, remoteId = "guid-$it") }
        val batches = mutableListOf<Int>()

        LocalAccountBackupFormat.read(
            source = backup(listOf(many.take(2), many.drop(2))),
            onHeader = {},
            onItems = { batches += it.size },
            batchSize = 2
        )

        assertEquals(listOf(2, 2, 1), batches)
    }

    @Test
    fun emptyAccountProducesAReadableBackup() = runTest {
        var headerRead = false
        LocalAccountBackupFormat.read(backup(items = emptyList()), { headerRead = true }, {})
        assertTrue(headerRead)
    }

    @Test(expected = InvalidBackupException::class)
    fun otherJsonDocumentsAreRejected() = runTest {
        LocalAccountBackupFormat.read(
            Buffer().writeUtf8("""{"format":"something-else","version":1,"folders":[],"feeds":[],"items":[]}"""),
            {}, {}
        )
    }

    @Test(expected = InvalidBackupException::class)
    fun newerBackupVersionsAreRejected() = runTest {
        LocalAccountBackupFormat.read(
            Buffer().writeUtf8(
                """{"format":"${LocalAccountBackupFormat.FORMAT}","version":99,"folders":[],"feeds":[],"items":[]}"""
            ),
            {}, {}
        )
    }

    @Test(expected = InvalidBackupException::class)
    fun backupWithoutArticleSectionIsRejected() = runTest {
        LocalAccountBackupFormat.read(
            Buffer().writeUtf8("""{"format":"${LocalAccountBackupFormat.FORMAT}","version":1,"folders":[],"feeds":[]}"""),
            {}, {}
        )
    }
}
