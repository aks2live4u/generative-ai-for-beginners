package com.notnow.app.data.repository

import com.notnow.app.data.dao.ShoppingVaultDao
import com.notnow.app.data.entity.ShoppingVaultItem
import kotlinx.coroutines.flow.Flow

class ShoppingVaultRepository(private val dao: ShoppingVaultDao) {

    val activeItems: Flow<List<ShoppingVaultItem>> = dao.observeActive()
    val purchasedItems: Flow<List<ShoppingVaultItem>> = dao.observePurchased()

    suspend fun save(item: ShoppingVaultItem): Long = dao.insert(item)

    suspend fun markPurchased(id: Long) = dao.markPurchased(id)

    suspend fun delete(item: ShoppingVaultItem) = dao.delete(item)

    suspend fun getPendingReminders(): List<ShoppingVaultItem> {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        return dao.getPendingReminders(cutoff)
    }

    suspend fun markReminderSent(id: Long) =
        dao.markReminderSent(id, System.currentTimeMillis())
}
