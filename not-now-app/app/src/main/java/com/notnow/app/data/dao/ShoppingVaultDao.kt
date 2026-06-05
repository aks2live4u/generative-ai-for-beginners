package com.notnow.app.data.dao

import androidx.room.*
import com.notnow.app.data.entity.ShoppingVaultItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingVaultDao {
    @Query("SELECT * FROM shopping_vault WHERE isPurchased = 0 ORDER BY savedAt DESC")
    fun observeActive(): Flow<List<ShoppingVaultItem>>

    @Query("SELECT * FROM shopping_vault WHERE isPurchased = 1 ORDER BY savedAt DESC")
    fun observePurchased(): Flow<List<ShoppingVaultItem>>

    @Query("""
        SELECT * FROM shopping_vault 
        WHERE isPurchased = 0 
        AND reminderSentAt = 0 
        AND savedAt < :cutoffMs
    """)
    suspend fun getPendingReminders(cutoffMs: Long): List<ShoppingVaultItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingVaultItem): Long

    @Update
    suspend fun update(item: ShoppingVaultItem)

    @Query("UPDATE shopping_vault SET isPurchased = 1 WHERE id = :id")
    suspend fun markPurchased(id: Long)

    @Query("UPDATE shopping_vault SET reminderSentAt = :ts WHERE id = :id")
    suspend fun markReminderSent(id: Long, ts: Long)

    @Delete
    suspend fun delete(item: ShoppingVaultItem)
}
