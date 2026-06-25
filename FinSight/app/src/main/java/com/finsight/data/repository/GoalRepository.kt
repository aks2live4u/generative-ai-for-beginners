package com.finsight.data.repository

import com.finsight.core.model.Goal
import com.finsight.data.db.dao.GoalDao
import com.finsight.data.db.entity.toDomain
import com.finsight.data.db.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepository(private val goalDao: GoalDao) {
    fun observeAll(): Flow<List<Goal>> = goalDao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun upsert(goal: Goal) = goalDao.upsert(goal.toEntity())
}
