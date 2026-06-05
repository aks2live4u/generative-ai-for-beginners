package com.notnow.app.data.repository

import com.notnow.app.data.dao.ShoppingVaultDao
import com.notnow.app.data.entity.ShoppingVaultItem
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class ShoppingVaultRepository(private val dao: ShoppingVaultDao) {

    fun getActiveItems(): Flow<List<ShoppingVaultItem>> = dao.getActiveItems()

    suspend fun save(item: ShoppingVaultItem): Long = dao.insert(item)

    suspend fun markPurchased(id: Long) = dao.markPurchased(id)

    suspend fun markRemoved(id: Long) = dao.markRemoved(id)

    suspend fun markReminderSent(id: Long) = dao.markReminderSent(id, System.currentTimeMillis())

    suspend fun getItemsDueForReminder(): List<ShoppingVaultItem> {
        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        return dao.getItemsDueForReminder(cutoff)
    }

    suspend fun getById(id: Long): ShoppingVaultItem? = dao.getById(id)

    suspend fun countSavedThisWeek(): Int {
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return dao.countSavedSince(weekAgo)
    }
}
