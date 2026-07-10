package com.aichiefofstaff.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aichiefofstaff.data.db.TaskStatus
import com.aichiefofstaff.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueAt IS NULL, dueAt ASC, priority DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status != :done ORDER BY dueAt IS NULL, dueAt ASC, priority DESC")
    fun observeOpen(done: TaskStatus): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY dueAt IS NULL, dueAt ASC")
    fun observeForProject(projectId: Long): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE status != :done AND dueAt IS NOT NULL AND dueAt <= :endOfDay " +
            "ORDER BY dueAt ASC"
    )
    fun observeDueToday(endOfDay: Long, done: TaskStatus): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}
