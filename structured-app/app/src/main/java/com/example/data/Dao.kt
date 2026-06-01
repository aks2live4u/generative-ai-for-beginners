package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineTaskDao {
    @Query("SELECT * FROM timeline_tasks WHERE dayDate = :day ORDER BY timeSlotStart ASC")
    fun getTasksForDay(day: String): Flow<List<TimelineTask>>

    @Query("SELECT * FROM timeline_tasks ORDER BY dayDate DESC, timeSlotStart ASC")
    fun getAllTasks(): Flow<List<TimelineTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TimelineTask): Long

    @Update
    suspend fun updateTask(task: TimelineTask)

    @Delete
    suspend fun deleteTask(task: TimelineTask)

    @Query("DELETE FROM timeline_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)
}

@Dao
interface BrainDumpDao {
    @Query("SELECT * FROM brain_dumps WHERE processedStatus != 'Trash' ORDER BY createdAt DESC")
    fun getActiveBrainDumps(): Flow<List<BrainDump>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrainDump(dump: BrainDump): Long

    @Update
    suspend fun updateBrainDump(dump: BrainDump)

    @Delete
    suspend fun deleteBrainDump(dump: BrainDump)

    @Query("DELETE FROM brain_dumps WHERE id = :id")
    suspend fun deleteBrainDumpById(id: Int)
}

@Dao
interface MemoryItemDao {
    @Query("SELECT * FROM memory_vault ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memory_vault WHERE title LIKE '%' || :query || '%' OR details LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(item: MemoryItem): Long

    @Delete
    suspend fun deleteMemory(item: MemoryItem)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)
}

@Dao
interface MoodCheckinDao {
    @Query("SELECT * FROM mood_checkins ORDER BY timestamp DESC")
    fun getAllCheckins(): Flow<List<MoodCheckin>>

    @Query("SELECT * FROM mood_checkins ORDER BY timestamp DESC LIMIT 10")
    fun getRecentCheckins(): Flow<List<MoodCheckin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckin(checkin: MoodCheckin): Long
}

@Dao
interface TimeBlockDao {
    @Query("SELECT * FROM time_blocks ORDER BY startTime ASC")
    fun getAllTimeBlocks(): Flow<List<TimeBlock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeBlock(block: TimeBlock): Long

    @Delete
    suspend fun deleteTimeBlock(block: TimeBlock)
}
