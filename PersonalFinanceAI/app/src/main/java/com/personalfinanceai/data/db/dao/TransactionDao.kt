package com.personalfinanceai.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personalfinanceai.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query(
        "SELECT COUNT(*) FROM transactions WHERE merchant = :merchant AND amountMinor = :amountMinor " +
            "AND dateEpochMillis BETWEEN :startMillis AND :endMillis"
    )
    suspend fun countPossibleDuplicates(merchant: String, amountMinor: Long, startMillis: Long, endMillis: Long): Int
}
