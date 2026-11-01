package com.readrops.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Item
import com.readrops.db.entities.ItemState
import com.readrops.db.entities.ItemStateChange
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import com.readrops.db.queries.ItemContentProjection
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ItemDaoLargeContentTest {

    private lateinit var database: Database

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, Database::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun selectingItemWithMultiMegabyteContentReturnsBoundedPreview() = runTest {
        val account = Account(type = AccountType.LOCAL)
        account.id = database.accountDao().insert(account).toInt()
        val feed = Feed(name = "Large feed", accountId = account.id)
        feed.id = database.feedDao().insert(feed).toInt()

        val longContent = "x".repeat(3_100_000)
        val item = Item(title = "Long article", content = longContent, feedId = feed.id)
        item.id = database.itemDao().insert(item).toInt()

        val selected = database.itemDao().select(item.id)

        assertEquals(ItemContentProjection.MAX_TEXT_LENGTH + 1, selected.content!!.length)
        assertTrue(selected.content!!.startsWith("x"))
        assertTrue(selected.content!!.endsWith("…"))
    }

    @Test
    fun pruningKeepsNewestItemsAndAlwaysPreservesFavorites() = runTest {
        val account = Account(type = AccountType.LOCAL)
        account.id = database.accountDao().insert(account).toInt()
        val feed = Feed(name = "Retention feed", accountId = account.id)
        feed.id = database.feedDao().insert(feed).toInt()

        val items = listOf(
            Item(title = "Old favorite", remoteId = "favorite", feedId = feed.id,
                pubDate = LocalDateTime.of(2020, 1, 1, 0, 0), isStarred = true),
            Item(title = "Old article", remoteId = "old", feedId = feed.id,
                pubDate = LocalDateTime.of(2020, 1, 2, 0, 0)),
            Item(title = "New article 1", remoteId = "new-1", feedId = feed.id,
                pubDate = LocalDateTime.of(2025, 1, 1, 0, 0)),
            Item(title = "New article 2", remoteId = "new-2", feedId = feed.id,
                pubDate = LocalDateTime.of(2025, 1, 2, 0, 0)),
        )
        database.itemDao().insert(items)

        database.itemDao().pruneUnstarredItems(feed.id, keepCount = 2)

        val retained = database.itemDao().selectItems(feed.id)
        assertEquals(setOf("Old favorite", "New article 1", "New article 2"), retained.map { it.title }.toSet())
    }

    @Test
    fun widgetQueryReturnsNewestUnreadArticlesWithFeedAndAccount() = runTest {
        val account = Account(type = AccountType.LOCAL)
        account.id = database.accountDao().insert(account).toInt()
        val feed = Feed(name = "Widget feed", accountId = account.id)
        feed.id = database.feedDao().insert(feed).toInt()
        database.itemDao().insert(
            listOf(
                Item(title = "Old unread", feedId = feed.id,
                    pubDate = LocalDateTime.of(2024, 1, 1, 0, 0)),
                Item(title = "Newest unread", feedId = feed.id,
                    pubDate = LocalDateTime.of(2025, 1, 2, 0, 0)),
                Item(title = "Read article", feedId = feed.id, isRead = true,
                    pubDate = LocalDateTime.of(2025, 1, 3, 0, 0)),
                Item(title = "Recent unread", feedId = feed.id,
                    pubDate = LocalDateTime.of(2025, 1, 1, 0, 0)),
            )
        )

        val widgetArticles = database.itemDao().selectLatestUnreadForWidget(limit = 1)

        assertEquals(1, widgetArticles.size)
        assertEquals("Newest unread", widgetArticles.single().title)
        assertEquals("Widget feed", widgetArticles.single().feedName)
        assertEquals(account.id, widgetArticles.single().accountId)
    }

    @Test
    fun cleanupPreviewUsesRemoteReadAndFavoriteStateAndRespectsCutoff() = runTest {
        val account = Account(type = AccountType.MINIFLUX)
        account.id = database.accountDao().insert(account).toInt()
        val feed = Feed(name = "Cleanup feed", accountId = account.id)
        feed.id = database.feedDao().insert(feed).toInt()
        val oldDate = LocalDateTime.now().minusDays(60)
        val recentDate = LocalDateTime.now().minusDays(5)
        val articles = listOf(
            Item(title = "Old read", remoteId = "old-read", feedId = feed.id, pubDate = oldDate),
            Item(title = "Old unread", remoteId = "old-unread", feedId = feed.id, pubDate = oldDate),
            Item(title = "Old favorite", remoteId = "old-favorite", feedId = feed.id, pubDate = oldDate),
            Item(title = "Recent read", remoteId = "recent-read", feedId = feed.id, pubDate = recentDate)
        )
        database.itemDao().insert(articles)
        val pendingItem = Item(title = "Old pending", remoteId = "old-pending", feedId = feed.id, pubDate = oldDate)
        pendingItem.id = database.itemDao().insert(pendingItem).toInt()
        database.itemStateDao().insert(
            listOf(
                ItemState(read = true, starred = false, remoteId = "old-read", accountId = account.id),
                ItemState(read = false, starred = false, remoteId = "old-unread", accountId = account.id),
                ItemState(read = true, starred = true, remoteId = "old-favorite", accountId = account.id),
                ItemState(read = true, starred = false, remoteId = "recent-read", accountId = account.id),
                ItemState(read = true, starred = false, remoteId = "old-pending", accountId = account.id)
            )
        )
        database.itemStateChangeDao().insert(
            ItemStateChange(id = pendingItem.id, readChange = true, accountId = account.id)
        )

        val matching = database.itemDao().selectCleanupItemIdsPage(
            accountId = account.id,
            feedIds = listOf(feed.id),
            cutoff = System.currentTimeMillis() - 30L * 86_400_000L,
            includeRead = true,
            includeUnread = false,
            keepStarred = true,
            separateState = true,
            afterId = 0,
            limit = 100
        )

        val selectedArticles = database.itemDao().selectItems(feed.id).filter { it.id in matching }
        assertEquals(setOf("old-read"), selectedArticles.map { it.remoteId }.toSet())
        assertEquals(1, database.itemDao().countCleanupItems(
            accountId = account.id,
            feedIds = listOf(feed.id),
            cutoff = System.currentTimeMillis() - 30L * 86_400_000L,
            includeRead = true,
            includeUnread = false,
            keepStarred = true,
            separateState = true
        ))
    }
}
