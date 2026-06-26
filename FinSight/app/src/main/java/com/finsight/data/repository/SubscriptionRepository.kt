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

    /**
     * Every row in this table is currently auto-detected (there's no manual "add a subscription"
     * UI) so re-running detection should fully replace the set rather than only upsert new finds -
     * otherwise a subscription that detection used to (incorrectly) flag stays in the database
     * forever even after the detection logic that created it is fixed, since upsert never removes
     * a row that's no longer detected.
     */
    suspend fun replaceAutoDetected(subscriptions: List<Subscription>) {
        subscriptionDao.deleteAll()
        subscriptions.forEach { subscriptionDao.upsert(it.toEntity()) }
    }
}
