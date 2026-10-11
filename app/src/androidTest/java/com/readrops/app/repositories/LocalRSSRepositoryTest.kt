package com.readrops.app.repositories

import androidx.paging.PagingSource
import com.readrops.api.utils.ApiUtils
import com.readrops.app.testutil.ReadropsTestRule
import com.readrops.app.testutil.TestUtils
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
