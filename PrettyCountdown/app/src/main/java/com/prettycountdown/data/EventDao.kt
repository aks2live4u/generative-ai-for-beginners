package com.prettycountdown.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.prettycountdown.data.model.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY targetDateTime ASC")
    fun observeAll(): Flow<List<Event>>

    @Query("SELECT * FROM events ORDER BY targetDateTime ASC")
    suspend fun getAll(): List<Event>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeById(id: Long): Flow<Event?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): Event?

    @Insert
    suspend fun insert(event: Event): Long

    @Insert
    suspend fun insertAll(events: List<Event>)

    @Update
    suspend fun update(event: Event)

    @Delete
    suspend fun delete(event: Event)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM events")
    fun observeCount(): Flow<Int>
}
