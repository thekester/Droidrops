package com.readrops.app.account.backup

import com.readrops.app.testutil.ReadropsTestRule
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.ItemState
import com.readrops.db.entities.Tag
import com.readrops.db.entities.TagJoin
import com.readrops.db.entities.OpenIn
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.inject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

class LocalAccountBackupRepositoryTest : KoinTest {

    private val database: Database by inject()
    private val repository by lazy { LocalAccountBackupRepository(database) }
    private lateinit var account: Account

    @get:Rule
    val koinTest = ReadropsTestRule()

    @Before
    fun before() = runTest {
        account = Account(name = "Original", type = AccountType.LOCAL, isNotificationsEnabled = true)
        account.id = database.accountDao().insert(account).toInt()

        val folderId = database.folderDao().insert(Folder(name = "Tech", accountId = account.id)).toInt()
        val feedId = database.feedDao().insert(
            Feed(
                name = "In folder",
                url = "https://example.org/a.xml",
                folderId = folderId,
                accountId = account.id,
                isNotificationEnabled = false,
                openIn = OpenIn.EXTERNAL_VIEW,
                etag = "cached",
            )
        ).toInt()
        val rootFeedId = database.feedDao().insert(
            Feed(name = "At root", url = "https://example.org/b.xml", accountId = account.id)
        ).toInt()

        // more than one export page, to cover the keyset pagination
        database.itemDao().insert((1..250).map { index ->
            Item(
                title = "Item $index",
                content = "<p>Body $index</p>",
                feedId = if (index % 2 == 0) feedId else rootFeedId,
                pubDate = LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(index.toLong()),
                isRead = index % 3 == 0,
                isStarred = index % 5 == 0,
                remoteId = "guid-$index",
            )
        })
    }

    private suspend fun exportOriginal(): ByteArray {
        val output = ByteArrayOutputStream()
        val summary = repository.export(account, output)
        assertEquals(2, summary.feedCount)
        assertEquals(250, summary.itemCount)
        return output.toByteArray()
    }

    @Test
    fun restoreCreatesANewCurrentAccountWithEverything() = runTest {
        val backup = exportOriginal()

        val summary = repository.restore(ByteArrayInputStream(backup), "Local").summary

        assertEquals(1, summary.folderCount)
        assertEquals(2, summary.feedCount)
        assertEquals(250, summary.itemCount)

        val restored = database.accountDao().selectCurrentAccount().first()!!
        assertTrue(restored.id != account.id)
        assertEquals("Original", restored.name)
        assertEquals(AccountType.LOCAL, restored.type)
        assertTrue(restored.isNotificationsEnabled)

        val folder = database.folderDao().selectAllFolders(restored.id).single()
        val feeds = database.feedDao().selectFeeds(restored.id)
        val inFolder = feeds.single { it.name == "In folder" }
        assertEquals(folder.id, inFolder.folderId)
        assertFalse(inFolder.isNotificationEnabled)
        assertEquals(OpenIn.EXTERNAL_VIEW, inFolder.openIn)
        assertEquals(null, inFolder.etag)

        val items = feeds.flatMap { database.itemDao().selectItems(it.id) }
        assertEquals(250, items.size)
        assertEquals((1..250).count { it % 3 == 0 }, items.count { it.isRead })
        assertEquals((1..250).count { it % 5 == 0 }, items.count { it.isStarred })
        assertEquals("<p>Body 8</p>", items.single { it.remoteId == "guid-8" }.content)

        // the original account is left as it was
        assertEquals(2, database.feedDao().selectFeeds(account.id).size)
        assertEquals(250, database.feedDao().selectFeeds(account.id).sumOf {
            database.itemDao().selectItems(it.id).size
        })
    }

    @Test
    fun serverAccountIsRestoredAsLocalWithItsSeparateStatesAndFullArticleFeeds() = runTest {
        val server = Account(name = "FreshRSS", type = AccountType.FRESHRSS, token = "secret-token")
        server.id = database.accountDao().insert(server).toInt()
        val feedId = database.feedDao().insert(
            Feed(name = "Server feed", url = "https://example.org/s.xml", remoteId = "feed/1", accountId = server.id)
        ).toInt()
        database.itemDao().insert(
            listOf(
                Item(title = "Read one", link = "https://example.org/1", feedId = feedId, remoteId = "tag:1"),
                Item(title = "Starred one", link = "https://example.org/2", feedId = feedId, remoteId = "tag:2"),
                Item(title = "Untouched", link = "https://example.org/3", feedId = feedId, remoteId = "tag:3"),
            )
        )
        database.itemStateDao().insert(ItemState(read = true, remoteId = "tag:1", accountId = server.id))
        database.itemStateDao().insert(ItemState(starred = true, remoteId = "tag:2", accountId = server.id))

        // a FreshRSS label on the first article
        val labelId = database.tagDao().insert(
            Tag(name = "Read later", remoteId = "user/-/label/Read later", accountId = server.id)
        ).toInt()
        val labelledId = database.itemDao().selectItems(feedId).single { it.title == "Read one" }.id
        database.tagJoinDao().insert(TagJoin(tagId = labelId, itemId = labelledId))

        val output = ByteArrayOutputStream()
        repository.export(server, output, fullContentFeedIds = setOf(feedId))
        val json = output.toString(Charsets.UTF_8.name())
        assertFalse(json.contains("secret-token"))
        assertFalse(json.contains("feed/1"))

        val result = repository.restore(ByteArrayInputStream(output.toByteArray()), "Local")

        val restored = database.accountDao().select(result.accountId)
        assertEquals(AccountType.LOCAL, restored.type)
        assertEquals(null, restored.token)
        val restoredFeed = database.feedDao().selectFeeds(restored.id).single()
        assertEquals(setOf(restoredFeed.id), result.fullContentFeedIds)

        val items = database.itemDao().selectItems(restoredFeed.id).associateBy { it.title }
        assertTrue(items.getValue("Read one").isRead)
        assertFalse(items.getValue("Read one").isStarred)
        assertTrue(items.getValue("Starred one").isStarred)
        assertFalse(items.getValue("Untouched").isRead)
        // local syncs recognise entries by link when the server id is meaningless
        assertEquals("https://example.org/1", items.getValue("Read one").remoteId)

        val restoredTag = database.tagDao().selectAll(restored.id).single()
        assertEquals("Read later", restoredTag.name)
        assertEquals(null, restoredTag.remoteId)
        assertEquals(
            listOf("Read later"),
            database.tagDao().selectAllByItem(items.getValue("Read one").id).map { it.name }
        )
        assertTrue(database.tagDao().selectAllByItem(items.getValue("Untouched").id).isEmpty())
    }

    @Test
    fun truncatedBackupRestoresNothing() = runTest {
        val backup = exportOriginal()
        val truncated = backup.copyOf(backup.size * 2 / 3)
        val accountCount = database.accountDao().selectAccountCount()
        val itemCount = database.itemDao().count()

        try {
            repository.restore(ByteArrayInputStream(truncated), "Local")
            fail("A truncated backup must not be accepted")
        } catch (_: Exception) {
        }

        assertEquals(accountCount, database.accountDao().selectAccountCount())
        assertEquals(itemCount, database.itemDao().count())
        assertEquals(account.id, database.accountDao().selectCurrentAccount().first()!!.id)
    }
}
