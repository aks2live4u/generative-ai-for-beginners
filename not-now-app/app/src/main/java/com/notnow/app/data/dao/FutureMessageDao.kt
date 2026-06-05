package com.notnow.app.data.dao

import androidx.room.*
import com.notnow.app.data.entity.FutureMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface FutureMessageDao {
    @Query("SELECT * FROM future_messages WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveMessages(): Flow<List<FutureMessage>>

    @Query("SELECT * FROM future_messages WHERE isActive = 1 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomMessage(): FutureMessage?

    @Insert
    suspend fun insert(message: FutureMessage): Long

    @Query("UPDATE future_messages SET isActive = 0 WHERE id = :id")
    suspend fun delete(id: Long)
}
