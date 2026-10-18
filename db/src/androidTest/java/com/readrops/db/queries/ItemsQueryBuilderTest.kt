package com.readrops.db.queries

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Item
import com.readrops.db.entities.Tag
import com.readrops.db.entities.TagJoin
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import com.readrops.db.filters.MainFilter
import com.readrops.db.filters.OrderField
import com.readrops.db.filters.OrderType
import com.readrops.db.filters.QueryFilters
import com.readrops.db.filters.SubFilter
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import androidx.paging.PagingSource
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ItemsQueryBuilderTest {

    private lateinit var database: Database

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, Database::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun noFilterDefaultSortCaseTest() {
        val queryFilters = QueryFilters(accountId = 1)

        val query = ItemsQueryBuilder.buildItemsQuery(queryFilters)

        database.query(query)

        with(query.sql) {
            assertTrue(contains("Feed.account_id = 1"))
            assertTrue(contains("pub_date DESC"))

            assertFalse(contains("read = 0 And"))
        }

    }

    @Test
    fun feedFilterCaseTest() {
        val queryFilters = QueryFilters(
            accountId = 1,
            subFilter = SubFilter.FEED,
            feedId = 15
        )

        val query = ItemsQueryBuilder.buildItemsQuery(queryFilters)
        database.query(query)

        assertTrue(query.sql.contains("feed_id = 15"))
    }

    @Test
    fun tagFilterReturnsOnlyArticlesAssignedToSelectedTag() = runBlocking {
        val accountId = database.accountDao().insert(Account(type = AccountType.LOCAL)).toInt()
        val feedId = database.feedDao().insert(
            Feed(name = "Tag test", url = "https://example.org/feed", accountId = accountId)
        ).toInt()
        val taggedItemId = database.itemDao().insert(
            Item(title = "Tagged article", link = "https://example.org/tagged", remoteId = "tagged", feedId = feedId)
        ).toInt()
        database.itemDao().insert(
            Item(title = "Other article", link = "https://example.org/other", remoteId = "other", feedId = feedId)
        )
        val tagId = database.tagDao().insert(Tag(name = "Research", accountId = accountId)).toInt()
        database.tagJoinDao().insert(TagJoin(tagId = tagId, itemId = taggedItemId))

        val result = database.itemDao().selectAll(
            ItemsQueryBuilder.buildItemsQuery(
                QueryFilters(accountId = accountId, subFilter = SubFilter.TAG, tagId = tagId)
            )
        ).load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        assertEquals(
            listOf("Tagged article"),
            (result as PagingSource.LoadResult.Page).data.map { it.item.title }
        )
    }

    @Test
    fun itemIdsQueryKeepsSelectedReadItemInUnreadSessionOrder() = runBlocking {
        val accountId = database.accountDao().insert(Account(type = AccountType.LOCAL)).toInt()
        val feedId = database.feedDao().insert(
            Feed(name = "Pager anchor", url = "https://example.org/pager", accountId = accountId)
        ).toInt()
        val newestId = database.itemDao().insert(
            Item(title = "Newest", remoteId = "newest", feedId = feedId,
                pubDate = LocalDateTime.parse("2026-10-17T12:00:00"))
        ).toInt()
        val selectedReadId = database.itemDao().insert(
            Item(title = "Selected", remoteId = "selected", feedId = feedId, isRead = true,
                pubDate = LocalDateTime.parse("2026-10-17T11:00:00"))
        ).toInt()
        val olderId = database.itemDao().insert(
            Item(title = "Older", remoteId = "older", feedId = feedId,
                pubDate = LocalDateTime.parse("2026-10-17T10:00:00"))
        ).toInt()
        database.itemDao().insert(
            Item(title = "Other read", remoteId = "other-read", feedId = feedId, isRead = true,
                pubDate = LocalDateTime.parse("2026-10-17T09:00:00"))
        )

        val ids = database.itemDao().selectItemIds(
            ItemsQueryBuilder.buildItemIdsQuery(
                QueryFilters(
                    accountId = accountId,
                    showReadItems = false,
                    recentlyReadItemIds = listOf(selectedReadId)
                ),
                separateState = false
            )
        )

        assertEquals(listOf(newestId, selectedReadId, olderId), ids)
    }

    @Test
    fun starsFilterCaseTest() {
        val queryFilters = QueryFilters(accountId = 1, mainFilter = MainFilter.STARS)

        val query = ItemsQueryBuilder.buildItemsQuery(queryFilters)
        database.query(query)

        assertTrue(query.sql.contains("starred = 1"))
    }

    @Test
    fun favoritesIncludeReadItemsWhenReadItemsAreHiddenGlobally() {
        val query = ItemsQueryBuilder.buildItemsQuery(
            QueryFilters(accountId = 1, mainFilter = MainFilter.STARS, showReadItems = false)
        )

        database.query(query)
        assertTrue(query.sql.contains("starred = 1"))
        assertFalse(query.sql.contains("read = 0"))
    }

    @Test
    fun newFilterCaseTest() {
        val queryFilters = QueryFilters(accountId = 1, mainFilter = MainFilter.NEW)

        val query = ItemsQueryBuilder.buildItemsQuery(queryFilters)
        database.query(query)

        assertTrue(query.sql.contains("Between DateTime(DateTime(\"now\"), \"-24 hour\") And DateTime(\"now\")"))
    }

    @Test
    fun folderFilterCaseTest() {
        val queryFilters = QueryFilters(accountId = 1, subFilter = SubFilter.FOLDER, folderId = 1)

        val query = ItemsQueryBuilder.buildItemsQuery(queryFilters)
        database.query(query)

        assertTrue(query.sql.contains("folder_id = ${queryFilters.folderId}"))
    }

    @Test
    fun oldestSortCaseTest() {
        val queryFilters = QueryFilters(
            accountId = 1,
            orderType = OrderType.ASC,
            orderField = OrderField.DATE,
            showReadItems = false
        )

        val query = ItemsQueryBuilder.buildItemsQuery(queryFilters)
        database.query(query)

        with(query.sql) {
            assertTrue(contains("read = 0"))
            assertTrue(contains("pub_date ASC"))
        }
    }

    @Test
    fun unreadFilterKeepsRecentlyReadItemsVisible() {
        val query = ItemsQueryBuilder.buildItemsQuery(
            QueryFilters(
                accountId = 1,
                showReadItems = false,
                recentlyReadItemIds = listOf(12, 34)
            )
        )

        assertTrue(query.sql.contains("(Item.read = 0 Or Item.id In (12,34))"))
        database.query(query)
    }

    @Test
    fun recentlyReadOnlyLimitsTimelineToSessionArticleIds() {
        val query = ItemsQueryBuilder.buildItemsQuery(
            QueryFilters(
                accountId = 1,
                recentlyReadOnly = true,
                recentlyReadItemIds = listOf(12, 34)
            )
        )

        assertTrue(query.sql.contains("Item.id In (12,34)"))
        assertFalse(query.sql.contains("Item.read = 0"))
        database.query(query)
    }

    @Test
    fun recentlyReadOnlyWithNoSessionItemsReturnsNoArticles() {
        val query = ItemsQueryBuilder.buildItemsQuery(
            QueryFilters(accountId = 1, recentlyReadOnly = true)
        )

        assertTrue(query.sql.contains("And 1 = 0"))
        database.query(query)
    }

    @Test
    fun separateStateUnreadFilterKeepsRecentlyReadItemsVisible() {
        val query = ItemsQueryBuilder.buildItemsQuery(
            QueryFilters(
                accountId = 1,
                showReadItems = false,
                recentlyReadItemIds = listOf(12)
            ),
            separateState = true
        )

        assertTrue(query.sql.contains("(ItemState.read = 0 Or Item.id In (12))"))
        database.query(query)
    }

    @Test
    fun newestSortCaseTest() {
        val queryFilters = QueryFilters(
            accountId = 1,
            orderType = OrderType.DESC,
            orderField = OrderField.ID
        )

        val query = ItemsQueryBuilder.buildItemsQuery(queryFilters)
        database.query(query)

        with(query.sql) {
            assertTrue(contains("Item.id DESC"))
        }
    }

    @Test
    fun searchFiltersTitleAndArticleTextUsingBoundValues() {
        val query = ItemsQueryBuilder.buildItemsQuery(
            QueryFilters(accountId = 1, searchQuery = "graph_ 100% ' OR 1=1 --")
        )

        assertTrue(query.sql.contains("Item.title LIKE ?"))
        assertTrue(query.sql.contains("Item.content LIKE ?"))
        assertTrue(query.sql.contains("Item.description LIKE ?"))
        assertEquals(3, query.argCount)

        // Malicious-looking input remains a bound value and cannot break the SQL.
        database.query(query)
    }

    @Test
    fun separateStateTest() {
        val queryFilters = QueryFilters(
            accountId = 1,
            showReadItems = false,
            mainFilter = MainFilter.ALL
        )

        val query = ItemsQueryBuilder.buildItemsQuery(queryFilters, true)
        database.query(query)

        with(query.sql) {
            assertFalse(contains("read, starred"))
            assertTrue(contains("ItemState.read = 0"))
        }
    }

    @Test
    fun favoritesWithSeparateStateIncludeReadItemsWhenReadItemsAreHiddenGlobally() {
        val query = ItemsQueryBuilder.buildItemsQuery(
            QueryFilters(accountId = 1, showReadItems = false, mainFilter = MainFilter.STARS),
            separateState = true
        )

        database.query(query)
        assertTrue(query.sql.contains("ItemState.starred = 1"))
        assertFalse(query.sql.contains("ItemState.read = 0"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun accountIdExceptionTest() {
        val queryFilters = QueryFilters()
        ItemsQueryBuilder.buildItemsQuery(queryFilters)
    }

    @Test(expected = IllegalArgumentException::class)
    fun filterFeedIdExceptionTest() {
        val queryFilters = QueryFilters(accountId = 1, subFilter = SubFilter.FEED)
        ItemsQueryBuilder.buildItemsQuery(queryFilters)
    }

    @Test
    fun folderFilterExceptionTest() {
        val queryFilters = QueryFilters(accountId = 1, subFilter = SubFilter.FOLDER)
        assertThrows(IllegalArgumentException::class.java) {
            ItemsQueryBuilder.buildItemsQuery(queryFilters)
        }
    }
}
