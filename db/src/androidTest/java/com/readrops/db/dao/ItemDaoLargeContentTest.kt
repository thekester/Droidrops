package com.readrops.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Item
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
}
