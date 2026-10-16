package com.readrops.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.ItemState
import com.readrops.db.pojo.ExistingItem
import com.readrops.db.pojo.ItemWithFeed
import com.readrops.db.queries.ItemContentProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao : BaseDao<Item> {
    @Query("Select Count(id) From Item")
    suspend fun count(): Int

    @Query("Select ${ItemContentProjection.ITEM_COLUMNS} From Item Where id = :itemId")
    suspend fun select(itemId: Int): Item

    @Query("Select ${ItemContentProjection.ITEM_COLUMNS} From Item Limit 1")
    suspend fun selectFirst(): Item

    @Query("Select ${ItemContentProjection.ITEM_COLUMNS} From Item Where feed_id = :feedId")
    suspend fun selectItems(feedId: Int): List<Item>

    @RawQuery(observedEntities = [Item::class, Feed::class, Folder::class, ItemState::class])
    fun selectAll(query: SupportSQLiteQuery): PagingSource<Int, ItemWithFeed>

    @RawQuery(observedEntities = [Item::class, ItemState::class])
    fun selectItemById(query: SupportSQLiteQuery): Flow<ItemWithFeed>

    @Query("Update Item Set read = :read Where id = :itemId")
    suspend fun updateReadState(itemId: Int, read: Boolean)

    @Query("Delete From Item Where id In (:itemIds)")
    suspend fun deleteByIds(itemIds: List<Int>)

    @Query("""Delete From Item Where feed_id = :feedId And starred = 0 And id Not In
        (Select id From Item Where feed_id = :feedId Order By pub_date Desc, id Desc Limit :keepCount)""")
    suspend fun pruneUnstarredItems(feedId: Int, keepCount: Int)

    @Query("Update Item Set starred = :starred Where id = :itemId")
    suspend fun updateStarState(itemId: Int, starred: Boolean)

    @Query("Update Item set read = :read, starred = :starred Where remote_id = :remoteId")
    suspend fun updateReadAndStarState(remoteId: String, read: Boolean, starred: Boolean)

    @Query("Update Item set read = 1 Where feed_id IN (Select id From Feed Where account_id = :accountId)")
    suspend fun setAllItemsRead(accountId: Int)

    @Query("Update Item set read = 1 Where id IN (:ids)")
    suspend fun setAllItemsRead(ids: List<Int>)

    @Query("Update Item set read = 1 Where starred = 1 And feed_id IN (Select id From Feed Where account_id = :accountId)")
    suspend fun setAllStarredItemsRead(accountId: Int)

    @Query("Update Item set read = 1 Where DateTime(Round(pub_date / 1000), 'unixepoch') " +
            "Between DateTime(DateTime(\"now\"), \"-24 hour\") And DateTime(\"now\") " +
            "And feed_id IN (Select id From Feed Where account_id = :accountId)")
    suspend fun setAllNewItemsRead(accountId: Int)

    @Query("Update Item set read = 1 Where feed_id IN " +
            "(Select id From Feed Where id = :feedId And account_id = :accountId)")
    suspend fun setAllItemsReadByFeed(feedId: Int, accountId: Int)

    @Query("Update Item set read = 1 Where feed_id IN (Select Feed.id From Feed Inner Join Folder " +
            "On Feed.folder_id = Folder.id Where Folder.id = :folderId And Folder.account_id = :accountId)")
    suspend fun setAllItemsReadByFolder(folderId: Int, accountId: Int)

    @Query("""Select count(*) From Item Inner Join Feed On Item.feed_id = Feed.id Where read = 0 
        And account_id = :accountId And DateTime(Round(Item.pub_date / 1000), 'unixepoch') 
        Between DateTime(DateTime("now"), "-24 hour") And DateTime("now")""")
    fun selectUnreadNewItemsCount(accountId: Int): Flow<Int>

    @Query("""Select count(*) From ItemState Inner Join Item On Item.remote_id = ItemState.remote_id
        Inner Join Feed On Feed.id = Item.feed_id And Feed.account_id = ItemState.account_id
        Where ItemState.read = 0 And Feed.account_id = :accountId And DateTime(Round(Item.pub_date / 1000), 'unixepoch')
        Between DateTime(DateTime("now"), "-24 hour") And DateTime("now")""")
    fun selectUnreadNewItemsCountByItemState(accountId: Int): Flow<Int>

    @RawQuery(observedEntities = [Item::class, ItemState::class])
    fun selectFeedUnreadItemsCount(query: SupportSQLiteQuery):
            Flow<Map<@MapColumn(columnName = "feed_id") Int, @MapColumn(columnName = "item_count") Int>>

    @Query("""Select case When Exists(Select 1 From Item Inner Join Feed on Item.feed_id = Feed.id
        Where Item.remote_id = :remoteId And account_id = :accountId) Then 1 else 0 end""")
    suspend fun itemExists(remoteId: String, accountId: Int): Boolean

    @Query("""Select Item.remote_id From Item Inner Join Feed On Item.feed_id = Feed.id
        Where account_id = :accountId And Item.remote_id In (:remoteIds)""")
    suspend fun selectExistingRemoteIdsChunk(remoteIds: List<String>, accountId: Int): List<String>

    @Query("""Select Item.id, Item.remote_id, Item.feed_id, Item.read, Item.starred
        From Item Inner Join Feed On Item.feed_id = Feed.id
        Where account_id = :accountId And Item.remote_id In (:remoteIds)""")
    suspend fun selectExistingItemsChunk(remoteIds: List<String>, accountId: Int): List<ExistingItem>

    /**
     * @return among [remoteIds], the ones already inserted for [accountId]
     */
    suspend fun selectExistingRemoteIds(remoteIds: List<String>, accountId: Int): Set<String> =
        remoteIds.chunked(SQLITE_MAX_VARIABLES)
            .flatMap { chunk -> selectExistingRemoteIdsChunk(chunk, accountId) }
            .toSet()

    suspend fun selectExistingItems(remoteIds: List<String>, accountId: Int): List<ExistingItem> =
        remoteIds.chunked(SQLITE_MAX_VARIABLES)
            .flatMap { chunk -> selectExistingItemsChunk(chunk, accountId) }
}

// SQLite refuses more than 999 variables per statement before version 3.32
private const val SQLITE_MAX_VARIABLES = 900
