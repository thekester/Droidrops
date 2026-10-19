package com.readrops.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.readrops.db.entities.TagJoin

@Dao
interface TagJoinDao : BaseDao<TagJoin> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConflict(tagJoins: List<TagJoin>)

    @Query("Select * From TagJoin Where item_id In (:itemIds)")
    suspend fun selectByItems(itemIds: List<Int>): List<TagJoin>
}