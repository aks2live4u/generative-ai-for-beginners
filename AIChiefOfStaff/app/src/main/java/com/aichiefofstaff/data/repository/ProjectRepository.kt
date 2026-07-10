package com.aichiefofstaff.data.repository

import com.aichiefofstaff.data.db.dao.ProjectDao
import com.aichiefofstaff.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    fun observeActive(): Flow<List<ProjectEntity>> = projectDao.observeActive()

    fun observeAll(): Flow<List<ProjectEntity>> = projectDao.observeAll()

    suspend fun getProject(id: Long): ProjectEntity? = projectDao.getById(id)

    suspend fun search(query: String): List<ProjectEntity> = projectDao.search(query)

    suspend fun save(project: ProjectEntity): Long = projectDao.upsert(project)

    suspend fun delete(project: ProjectEntity) = projectDao.delete(project)

    suspend fun archive(project: ProjectEntity) = projectDao.update(project.copy(archived = true))
}
