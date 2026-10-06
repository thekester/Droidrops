package com.readrops.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.readrops.db.entities.Tag
import com.readrops.db.entities.account.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao : BaseDao<Tag> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConflict(tags: List<Tag>)

    @Query("Select * From Tag Where account_id = :accountId")
    suspend fun selectAll(accountId: Int): List<Tag>

    @Query("Select * From Tag Where account_id = :accountId Order By name ASC")
    fun selectAllFlow(accountId: Int): Flow<List<Tag>>

    @Query("Select Tag.* From Tag Inner Join TagJoin on Tag.id = TagJoin.tag_id Where item_id = :itemId")
    suspend fun selectAllByItem(itemId: Int): List<Tag>

    @Query("Select remote_id From Tag Where account_id = :accountId")
    suspend fun selectTagRemoteIds(accountId: Int): List<String>

    @Query("Update Tag Set name = :name Where remote_id = :remoteId And account_id = :accountId")
    suspend fun updateTagName(name: String, remoteId: String, accountId: Int)

    @Query("Delete From Tag Where remote_id in (:ids) And account_id = :accountId")
    suspend fun deleteByIds(ids: List<String>, accountId: Int)

    /**
     * Insert, update and delete tags by account
     *
     * This method must always be called with the full [tags] list
     *
     * @param tags   feeds to insert or update
     * @param account owner of the feeds
     * @return the new tags list
     */
    @Transaction
    suspend fun upsertTags(tags: List<Tag>, account: Account): List<Tag> {
        val localTagIds = selectTagRemoteIds(account.id)

        val tagsToInsert =
            tags.filter { tag -> localTagIds.none { localTagId -> tag.remoteId == localTagId } }
        val tagsToDelete =
            localTagIds.filter { localTagId -> tags.none { tag -> localTagId == tag.remoteId } }

        for (tag in tags) {
            updateTagName(tag.name, tag.remoteId!!, tag.accountId)
        }

        if (tagsToDelete.isNotEmpty()) {
            deleteByIds(tagsToDelete, account.id)
        }

        if (tagsToInsert.isNotEmpty()) {
            insert(tagsToInsert)
        }

        return selectAll(account.id)
    }

}