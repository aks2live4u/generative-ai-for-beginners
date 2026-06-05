package com.notnow.app.data.dao

import androidx.room.*
import com.notnow.app.data.entity.FutureMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface FutureMessageDao {
    @Query("SELECT * FROM future_messages ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FutureMessage>>

    @Query("SELECT * FROM future_messages ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): FutureMessage?

    @Insert
    suspend fun insert(message: FutureMessage): Long

    @Update
    suspend fun update(message: FutureMessage)

    @Delete
    suspend fun delete(message: FutureMessage)

    @Query("UPDATE future_messages SET showCount = showCount + 1 WHERE id = :id")
    suspend fun incrementShowCount(id: Long)
}
