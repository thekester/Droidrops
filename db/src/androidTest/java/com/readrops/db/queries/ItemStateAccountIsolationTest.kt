package com.readrops.db.queries

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Item
import com.readrops.db.entities.ItemState
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import com.readrops.db.filters.QueryFilters
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemStateAccountIsolationTest {

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
    fun sameRemoteIdInAnotherAccountDoesNotDuplicateTimelineItem() = runTest {
        val firstAccount = Account(type = AccountType.LOCAL)
        firstAccount.id = database.accountDao().insert(firstAccount).toInt()
        val secondAccount = Account(type = AccountType.LOCAL)
        secondAccount.id = database.accountDao().insert(secondAccount).toInt()

        val firstFeed = Feed(name = "First", accountId = firstAccount.id)
        firstFeed.id = database.feedDao().insert(firstFeed).toInt()
        val secondFeed = Feed(name = "Second", accountId = secondAccount.id)
        secondFeed.id = database.feedDao().insert(secondFeed).toInt()

        val firstItem = Item(title = "First article", remoteId = "shared-id", feedId = firstFeed.id)
        firstItem.id = database.itemDao().insert(firstItem).toInt()
        val secondItem = Item(title = "Second article", remoteId = "shared-id", feedId = secondFeed.id)
        database.itemDao().insert(secondItem)

        database.itemStateDao().insert(
            ItemState(read = true, remoteId = "shared-id", accountId = firstAccount.id)
        )
        database.itemStateDao().insert(
            ItemState(read = false, remoteId = "shared-id", accountId = secondAccount.id)
        )

        val query = ItemsQueryBuilder.buildItemsQuery(QueryFilters(accountId = firstAccount.id), separateState = true)
        val result = database.itemDao().selectAll(query).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
        )

        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.data.size)
        assertEquals(firstItem.id, page.data.single().item.id)
        assertEquals(true, page.data.single().isRead)
    }
}
