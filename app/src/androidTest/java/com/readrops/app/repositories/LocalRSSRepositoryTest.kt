package com.readrops.app.repositories

import android.content.SharedPreferences
import androidx.paging.PagingSource
import com.readrops.api.utils.ApiUtils
import com.readrops.app.testutil.ReadropsTestRule
import com.readrops.app.testutil.TestUtils
import com.readrops.app.util.Preferences
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Item
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import com.readrops.db.filters.QueryFilters
import com.readrops.db.queries.ItemSelectionQueryBuilder
import com.readrops.db.queries.ItemsQueryBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject
import java.net.HttpURLConnection
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalRSSRepositoryTest : KoinTest {

    private val mockServer = MockWebServer()
    private val database: Database by inject()
    private val preferences: Preferences by inject()

    private val account = Account(type = AccountType.LOCAL)
    private lateinit var repository: LocalRSSRepository
    private lateinit var feeds: List<Feed>

    @get:Rule
    val koinTest = ReadropsTestRule()

    @Before
    fun before() = runTest {
        mockServer.start()
        val url = mockServer.url("/rss")

        account.id = database.accountDao().insert(account).toInt()
        feeds = listOf(
            Feed(
                name = "feedTest",
                url = url.toString(),
                accountId = account.id,
            ),
        )

        database.feedDao().insert(feeds).apply {
            feeds.first().id = first().toInt()
        }

        repository = get<BaseRepository> { parametersOf(account) } as LocalRSSRepository
    }

    @After
    fun after() {
        mockServer.shutdown()
    }

    @Test
    fun synchronizeTest() = runTest {
        val stream = TestUtils.loadResource("rss_feed.xml")

        mockServer.enqueue(
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader(ApiUtils.CONTENT_TYPE_HEADER, "application/xml; charset=UTF-8")
                .setBody(Buffer().readFrom(stream))
        )

        val result = repository.synchronize(listOf()) {
            assertEquals(it.name, feeds.first().name)
        }

        assertTrue { result.first.items.isNotEmpty() }
        assertTrue {
            database.itemDao().itemExists(result.first.items.first().remoteId!!, account.id)
        }
    }

    @Test
    fun synchronizeWithFeedsTest(): Unit = runBlocking {
        val stream = TestUtils.loadResource("rss_feed.xml")

        mockServer.enqueue(
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader(ApiUtils.CONTENT_TYPE_HEADER, "application/xml; charset=UTF-8")
                .setBody(Buffer().readFrom(stream))
        )

        val result = repository.synchronize(feeds) {
            assertEquals(it.name, feeds.first().name)
        }

        assertTrue { result.first.items.isNotEmpty() }
        assertTrue {
            database.itemDao().itemExists(result.first.items.first().remoteId!!, account.id)
        }
    }

    @Test
    fun atomEntryWithoutLinkIsStoredAndMarkupOnlyDescriptionIsOmitted() = runTest {
        mockServer.enqueue(
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader(ApiUtils.CONTENT_TYPE_HEADER, "application/atom+xml")
                .setBody("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <feed xmlns="http://www.w3.org/2005/Atom">
                      <id>https://example.org/feed</id><title>Example</title>
                      <entry><id>tag:example.org,2026:item-1</id><title>Image only</title>
                        <summary type="html">&lt;img src="https://example.org/chart.png"/&gt;</summary>
                      </entry>
                    </feed>
                """.trimIndent())
        )

        val result = repository.synchronize(feeds) { }
        val item = result.first.items.single()

        assertEquals("tag:example.org,2026:item-1", item.remoteId)
        assertTrue(item.link == null)
        assertTrue(item.cleanDescription == null)
        assertTrue(item.readTime == 0.0)
        assertTrue(item.content == null)
    }

    @Test
    fun authenticatedLocalHttpFeedSendsBasicAuthorizationHeader() = runTest {
        val feed = feeds.first()
        val encryptedPreferences = get<SharedPreferences>()
        encryptedPreferences.edit()
            .putString(feed.loginKey, "feed-user")
            .putString(feed.passwordKey, "feed-password")
            .apply()

        try {
            mockServer.enqueue(
                MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                    .addHeader(ApiUtils.CONTENT_TYPE_HEADER, "application/rss+xml")
                    .setBody("""
                        <rss version="2.0"><channel><title>Authenticated feed</title>
                        <link>https://example.org</link>
                        <item><title>Protected story</title><link>https://example.org/story</link>
                        <guid>protected-story</guid></item></channel></rss>
                    """.trimIndent())
            )

            val result = repository.synchronize(listOf(feed)) { }
            val request = mockServer.takeRequest()

            assertEquals("Basic ZmVlZC11c2VyOmZlZWQtcGFzc3dvcmQ=", request.getHeader("Authorization"))
            assertTrue(result.first.items.any { it.remoteId == "protected-story" })
        } finally {
            encryptedPreferences.edit()
                .remove(feed.loginKey)
                .remove(feed.passwordKey)
                .apply()
        }
    }

    @Test
    fun synchronizationAppliesConfiguredLocalFeedLimitAndKeepsStarredHistory() = runTest {
        preferences.maxItemsPerFeed.write(2)

        val starred = Item(
            title = "Saved favorite",
            link = "https://example.org/favorite",
            remoteId = "favorite",
            feedId = feeds.first().id,
            pubDate = java.time.LocalDateTime.of(2020, 1, 1, 0, 0),
            isStarred = true
        )
        database.itemDao().insert(starred)

        mockServer.enqueue(
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader(ApiUtils.CONTENT_TYPE_HEADER, "application/rss+xml")
                .setBody("""
                    <rss version="2.0"><channel><title>Retention</title><link>https://example.org</link>
                    <item><title>Old incoming</title><link>https://example.org/old</link><guid>old</guid><pubDate>Wed, 01 Jan 2020 00:00:00 GMT</pubDate></item>
                    <item><title>Latest one</title><link>https://example.org/new-1</link><guid>new-1</guid><pubDate>Wed, 01 Jan 2025 00:00:00 GMT</pubDate></item>
                    <item><title>Latest two</title><link>https://example.org/new-2</link><guid>new-2</guid><pubDate>Thu, 02 Jan 2025 00:00:00 GMT</pubDate></item>
                    </channel></rss>
                """.trimIndent())
        )

        repository.synchronize(feeds) { }

        val retained = database.itemDao().selectItems(feeds.first().id)
        assertEquals(
            setOf("Saved favorite", "Latest one", "Latest two"),
            retained.map { it.title }.toSet()
        )
    }

    @Test
    fun oversizedArticleCanBeReadWithoutExceedingCursorWindow(): Unit = runTest {
        val oversizedText = "x".repeat(1_100_000)
        val itemId = database.itemDao().insert(
            Item(
                title = "Oversized article",
                description = oversizedText,
                cleanDescription = oversizedText,
                content = oversizedText,
                link = "https://example.org/article",
                remoteId = "oversized-article",
                feedId = feeds.first().id
            )
        ).toInt()

        val directItem = database.itemDao().select(itemId)
        assertEquals(100_001, directItem.content!!.length)
        assertTrue(directItem.content!!.endsWith("…"))
        assertEquals(100_001, database.itemDao().selectItems(feeds.first().id).single().description!!.length)

        val article = database.itemDao()
            .selectItemById(ItemSelectionQueryBuilder.buildQuery(itemId, separateState = false))
            .first()
        assertEquals(100_001, article.item.content!!.length)

        val timelinePage = database.itemDao()
            .selectAll(ItemsQueryBuilder.buildItemsQuery(QueryFilters(accountId = account.id), false))
            .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false))

        assertTrue(timelinePage is PagingSource.LoadResult.Page)
        assertEquals(100_001, (timelinePage as PagingSource.LoadResult.Page).data.single().item.cleanDescription!!.length)
    }
}
