package com.personalfinanceai.data.repository

import com.personalfinanceai.core.model.Transaction
import com.personalfinanceai.data.db.dao.MerchantDao
import com.personalfinanceai.data.db.dao.TransactionDao
import com.personalfinanceai.data.db.entity.MerchantEntity
import com.personalfinanceai.data.db.entity.toDomain
import com.personalfinanceai.data.db.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration

/**
 * Single entry point for persisting and reading transactions. Also de-duplicates near-identical
 * transactions that can arrive from two sources at once (e.g. a bank SMS and the matching app
 * notification for the same UPI payment).
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantDao: MerchantDao
) {
    fun observeAll(): Flow<List<Transaction>> = transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Transaction> = transactionDao.getAll().map { it.toDomain() }

    /** Inserts [transaction] unless a transaction with the same merchant/amount already exists within [dedupeWindow]. */
    suspend fun insertIfNotDuplicate(transaction: Transaction, dedupeWindow: Duration = Duration.ofMinutes(10)): Boolean {
        val entity = transaction.toEntity()
        val windowStart = entity.dateEpochMillis - dedupeWindow.toMillis()
        val windowEnd = entity.dateEpochMillis + dedupeWindow.toMillis()
        val duplicates = transactionDao.countPossibleDuplicates(entity.merchant, entity.amountMinor, windowStart, windowEnd)
        if (duplicates > 0) return false

        transactionDao.insert(entity)
        bumpMerchant(transaction)
        return true
    }

    private suspend fun bumpMerchant(transaction: Transaction) {
        val existing = merchantDao.getByName(transaction.merchant)
        merchantDao.upsert(
            MerchantEntity(
                name = transaction.merchant,
                category = transaction.category.name,
                transactionCount = (existing?.transactionCount ?: 0) + 1
            )
        )
    }
}
