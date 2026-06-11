package com.prettycountdown.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.prettycountdown.data.model.ChecklistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items WHERE eventId = :eventId ORDER BY position ASC, id ASC")
    fun observeForEvent(eventId: Long): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllOnce(): List<ChecklistItem>

    @Insert
    suspend fun insert(item: ChecklistItem): Long

    @Insert
    suspend fun insertAll(items: List<ChecklistItem>)

    @Update
    suspend fun update(item: ChecklistItem)

    @Delete
    suspend fun delete(item: ChecklistItem)
}
