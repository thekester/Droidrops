package com.readrops.app.repositories

import com.readrops.app.testutil.ReadropsTestRule
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject
import java.net.HttpURLConnection

/**
 * Covers the two behaviours reported against a FreshRSS backend:
 * a category renamed on the server side, and a feed added from the app.
 */
class GReaderRepositoryTest : KoinTest {

    private val database: Database by inject()
    private val mockServer = MockWebServer()

    @get:Rule
    val rule = ReadropsTestRule()

    private lateinit var account: Account

    @Before
    fun before() = runTest {
        account = Account(
            name = "FreshRSS",
            type = AccountType.FRESHRSS,
            url = mockServer.url("/greader").toString(),
            writeToken = "writeToken"
        )
        account.id = database.accountDao().insert(account).toInt()
    }

    @After
    fun after() {
        mockServer.shutdown()
        database.clearAllTables()
    }

    private fun repository(): BaseRepository = get { parametersOf(account) }

    private fun ok(body: String) = MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_OK)
        .setBody(body)

    private fun folders(vararg names: String) = """
        {"tags":[{"id":"user/-/state/com.google/starred"},
        ${names.joinToString(",") { """{"id":"user/-/label/$it","type":"folder"}""" }}]}
    """.trimIndent()

    private fun subscriptions(folder: String) = """
        {"subscriptions":[{"id":"feed/1","title":"Hacker News",
        "categories":[{"id":"user/-/label/$folder","label":"$folder"}],
        "url":"https://news.ycombinator.com/rss","htmlUrl":"https://news.ycombinator.com",
        "iconUrl":"https://news.ycombinator.com/favicon.ico"}]}
    """.trimIndent()

    private fun items(vararg ids: String) = """
        {"id":"feed/1","updated":1625235516,"items":[
        ${
        ids.joinToString(",") {
            """{"id":"$it","published":1625234040,"title":"Title $it",
            "summary":{"content":"<p>Some content</p>"},
            "alternate":[{"href":"https://example.com/$it"}],
            "categories":["user/-/state/com.google/reading-list"],
            "origin":{"streamId":"feed/1","title":"Hacker News"}}"""
        }
    }]}
    """.trimIndent()

    private val noIds = """{"itemRefs":[]}"""

    /**
     * A GReader folder id embeds its name, so a rename looks like a delete plus a create. The local
     * folder must keep its id, otherwise the timeline filter pointing at it silently breaks.
     */
    @Test
    fun renamedFolderKeepsItsLocalIdTest() = runTest {
        var folderName = "Tech"

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                with(request.path!!) {
                    return when {
                        contains("tag/list") -> ok(folders(folderName))
                        contains("subscription/list") -> ok(subscriptions(folderName))
                        contains("stream/items/ids") -> ok(noIds)
                        contains("stream/contents") -> ok(items("item/1"))
                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }
        }

        repository().synchronize()

        val before = database.folderDao().selectAllFolders(account.id)
        assertEquals(1, before.size)
        assertEquals("Tech", before.first().name)
        val localId = before.first().id
        assertEquals(localId, database.feedDao().selectFeeds(account.id).first().folderId)

        // the category is renamed on the server, everything else is untouched
        folderName = "Techno"
        repository().synchronize()

        val after = database.folderDao().selectAllFolders(account.id)
        assertEquals(1, after.size)
        assertEquals("the local folder must be reused, not recreated", localId, after.first().id)
        assertEquals("Techno", after.first().name)
        assertEquals("user/-/label/Techno", after.first().remoteId)

        val feed = database.feedDao().selectFeeds(account.id).first()
        assertEquals("the feed must stay in the renamed folder", localId, feed.folderId)
    }

    /**
     * A folder really removed on the server side must still be deleted locally.
     */
    @Test
    fun deletedFolderIsRemovedTest() = runTest {
        var folderName = "Tech"
        var feedFolder = "Tech"

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                with(request.path!!) {
                    return when {
                        contains("tag/list") -> ok(folders(folderName))
                        contains("subscription/list") -> ok(subscriptions(feedFolder))
                        contains("stream/items/ids") -> ok(noIds)
                        contains("stream/contents") -> ok(items("item/1"))
                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }
        }

        repository().synchronize()
        assertEquals(1, database.folderDao().selectAllFolders(account.id).size)

        // the feed moves to another folder, so the disappearance is a real deletion
        folderName = "News"
        feedFolder = "News"
        // make the destination already known so it isn't mistaken for a rename
        database.folderDao().insert(
            com.readrops.db.entities.Folder(
                name = "News",
                remoteId = "user/-/label/News",
                accountId = account.id
            )
        )

        repository().synchronize()

        val after = database.folderDao().selectAllFolders(account.id)
        assertEquals(1, after.size)
        assertEquals("user/-/label/News", after.first().remoteId)
    }

    /**
     * Adding a feed from the app used to only call the server: nothing was stored locally and no
     * synchronization was triggered, so the feed stayed invisible.
     */
    @Test
    fun newFeedIsInsertedWithItsItemsTest() = runTest {
        var subscribed = false

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                with(request.path!!) {
                    return when {
                        contains("subscription/edit") -> {
                            subscribed = true
                            ok("OK")
                        }

                        contains("tag/list") -> ok(folders("Tech"))
                        contains("subscription/list") -> ok(subscriptions("Tech"))
                        contains("stream/contents/feed/1") -> ok(items("item/1", "item/2"))
                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }
        }

        val errors = repository().insertNewFeeds(
            newFeeds = listOf(Feed(url = "https://news.ycombinator.com/rss")),
            onUpdate = {}
        )

        assertTrue("subscription call must happen", subscribed)
        assertTrue("no error expected: $errors", errors.isEmpty())

        val feeds = database.feedDao().selectFeeds(account.id)
        assertEquals("the new feed must be visible without waiting for a sync", 1, feeds.size)
        assertEquals("feed/1", feeds.first().remoteId)
        assertEquals(
            "the new feed must keep its server-side category",
            database.folderDao().selectAllFolders(account.id).single().id,
            feeds.first().folderId
        )

        val items = database.itemDao().selectItems(feeds.first().id)
        assertEquals("the feed backlog must be fetched too", 2, items.size)

        // this account type reads the state from ItemState, an item without a row is hidden
        for (item in items) {
            assertNotNull(
                "item ${item.remoteId} must have an ItemState row",
                database.itemStateDao().selectItemState(account.id, item.remoteId!!)
            )
        }
    }

    /**
     * The backlog fetched when adding the feed is returned again by the next synchronization,
     * since FreshRSS filters the main item call on the insertion time. It must not be duplicated.
     */
    @Test
    fun newFeedItemsAreNotDuplicatedByNextSyncTest() = runTest {
        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                with(request.path!!) {
                    return when {
                        contains("subscription/edit") -> ok("OK")
                        contains("tag/list") -> ok(folders("Tech"))
                        contains("subscription/list") -> ok(subscriptions("Tech"))
                        contains("stream/items/ids") -> ok(noIds)
                        contains("stream/contents") -> ok(items("item/1", "item/2"))
                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }
        }

        repository().insertNewFeeds(
            newFeeds = listOf(Feed(url = "https://news.ycombinator.com/rss")),
            onUpdate = {}
        )

        val feedId = database.feedDao().selectFeeds(account.id).first().id
        assertEquals(2, database.itemDao().selectItems(feedId).size)

        // the next synchronization returns the very same items
        repository().synchronize()

        assertEquals(
            "items already inserted must not be inserted a second time",
            2, database.itemDao().selectItems(feedId).size
        )
    }
}
