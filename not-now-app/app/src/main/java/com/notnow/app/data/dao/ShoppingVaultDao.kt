package com.notnow.app.data.dao

import androidx.room.*
import com.notnow.app.data.entity.ShoppingVaultItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingVaultDao {
    @Query("SELECT * FROM shopping_vault WHERE isRemoved = 0 ORDER BY savedAt DESC")
    fun getActiveItems(): Flow<List<ShoppingVaultItem>>

    @Query("SELECT * FROM shopping_vault WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ShoppingVaultItem?

    @Query("SELECT * FROM shopping_vault WHERE reminderSentAt = 0 AND isRemoved = 0 AND savedAt <= :cutoff")
    suspend fun getItemsDueForReminder(cutoff: Long): List<ShoppingVaultItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingVaultItem): Long

    @Update
    suspend fun update(item: ShoppingVaultItem)

    @Query("UPDATE shopping_vault SET isRemoved = 1 WHERE id = :id")
    suspend fun markRemoved(id: Long)

    @Query("UPDATE shopping_vault SET isPurchased = 1 WHERE id = :id")
    suspend fun markPurchased(id: Long)

    @Query("UPDATE shopping_vault SET reminderSentAt = :timestamp WHERE id = :id")
    suspend fun markReminderSent(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM shopping_vault WHERE isRemoved = 0 AND savedAt >= :since")
    suspend fun countSavedSince(since: Long): Int
}
