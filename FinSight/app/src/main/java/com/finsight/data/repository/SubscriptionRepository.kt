package com.finsight.data.repository

import com.finsight.core.model.Subscription
import com.finsight.data.db.dao.SubscriptionDao
import com.finsight.data.db.entity.toDomain
import com.finsight.data.db.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubscriptionRepository(private val subscriptionDao: SubscriptionDao) {
    fun observeAll(): Flow<List<Subscription>> = subscriptionDao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun getAll(): List<Subscription> = subscriptionDao.getAll().map { it.toDomain() }
    suspend fun upsert(subscription: Subscription) = subscriptionDao.upsert(subscription.toEntity())
}
