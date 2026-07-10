package com.aichiefofstaff.data.repository

import com.aichiefofstaff.data.db.TaskStatus
import com.aichiefofstaff.data.db.dao.TaskDao
import com.aichiefofstaff.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(private val taskDao: TaskDao) {

    fun observeOpenTasks(): Flow<List<TaskEntity>> = taskDao.observeOpen(TaskStatus.DONE)

    fun observeAllTasks(): Flow<List<TaskEntity>> = taskDao.observeAll()

    fun observeTasksForProject(projectId: Long): Flow<List<TaskEntity>> =
        taskDao.observeForProject(projectId)

    fun observeDueToday(): Flow<List<TaskEntity>> {
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        return taskDao.observeDueToday(endOfDay, TaskStatus.DONE)
    }

    suspend fun getTask(id: Long): TaskEntity? = taskDao.getById(id)

    suspend fun search(query: String): List<TaskEntity> = taskDao.search(query)

    suspend fun save(task: TaskEntity): Long = taskDao.upsert(task)

    suspend fun delete(task: TaskEntity) = taskDao.delete(task)

    suspend fun markComplete(task: TaskEntity) {
        taskDao.update(task.copy(status = TaskStatus.DONE, completedAt = System.currentTimeMillis()))
    }

    suspend fun markIncomplete(task: TaskEntity) {
        taskDao.update(task.copy(status = TaskStatus.TODO, completedAt = null))
    }
}
