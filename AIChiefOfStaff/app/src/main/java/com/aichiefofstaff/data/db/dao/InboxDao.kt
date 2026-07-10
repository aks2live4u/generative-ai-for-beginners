package com.aichiefofstaff.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aichiefofstaff.data.db.entity.InboxItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InboxItemEntity>>

    @Query("SELECT * FROM inbox_items WHERE processed = 0 ORDER BY createdAt DESC")
    fun observeUnprocessed(): Flow<List<InboxItemEntity>>

    @Query("SELECT * FROM inbox_items WHERE content LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<InboxItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InboxItemEntity): Long

    @Update
    suspend fun update(item: InboxItemEntity)

    @Delete
    suspend fun delete(item: InboxItemEntity)
}
