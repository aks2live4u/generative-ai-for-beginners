package com.personalfinanceai.data.repository

import com.personalfinanceai.core.model.Goal
import com.personalfinanceai.data.db.dao.GoalDao
import com.personalfinanceai.data.db.entity.toDomain
import com.personalfinanceai.data.db.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepository(private val goalDao: GoalDao) {
    fun observeAll(): Flow<List<Goal>> = goalDao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun upsert(goal: Goal) = goalDao.upsert(goal.toEntity())
}
