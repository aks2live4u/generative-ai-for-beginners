package com.finsight.data.repository

import com.finsight.core.model.Category
import com.finsight.core.model.Transaction
import com.finsight.core.parser.MerchantMatcher
import com.finsight.core.parser.MerchantRuleBook
import com.finsight.data.MerchantRuleManager
import com.finsight.data.db.dao.MerchantDao
import com.finsight.data.db.dao.TransactionDao
import com.finsight.data.db.entity.MerchantEntity
import com.finsight.data.db.entity.TransactionEntity
import com.finsight.data.db.entity.toDomain
import com.finsight.data.db.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration

/**
 * Single entry point for persisting and reading transactions. Also de-duplicates near-identical
 * transactions that can arrive from two sources at once (e.g. a bank SMS and the matching app
 * notification for the same UPI payment). Matching uses fuzzy merchant comparison ([MerchantMatcher])
 * rather than an exact string match, since the same purchase often reaches each source with a
 * slightly different merchant spelling ("Swiggy" vs "SWIGGY*ORDER"); when a second source confirms
 * an existing transaction, that's recorded in its notes instead of inserting a second row. Also
 * applies the user's taught [MerchantRuleManager] rules (the "Personal Merchant Dictionary") to
 * every newly inserted transaction's merchant name.
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantDao: MerchantDao,
    private val merchantRuleManager: MerchantRuleManager? = null
) {
    fun observeAll(): Flow<List<Transaction>> = transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Transaction> = transactionDao.getAll().map { it.toDomain() }

    /** Inserts [transaction] unless a matching transaction already exists within [dedupeWindow]. */
    suspend fun insertIfNotDuplicate(transaction: Transaction, dedupeWindow: Duration = Duration.ofMinutes(10)): Boolean {
        val taughtLabel = merchantRuleManager?.allRules()?.let { MerchantRuleBook.resolveLabel(it, transaction.merchant) }
        val labeledTransaction = if (taughtLabel != null) transaction.copy(merchant = taughtLabel) else transaction

        val entity = labeledTransaction.toEntity()
        val windowStart = entity.dateEpochMillis - dedupeWindow.toMillis()
        val windowEnd = entity.dateEpochMillis + dedupeWindow.toMillis()
        val candidates = transactionDao.getCandidateDuplicates(entity.amountMinor, windowStart, windowEnd)
        val match = candidates.firstOrNull { MerchantMatcher.isSameMerchant(it.merchant, entity.merchant) }
        if (match != null) {
            linkAsMultiSourceConfirmation(match, entity)
            return false
        }

        transactionDao.insert(entity)
        bumpMerchant(labeledTransaction)
        return true
    }

    /** Tap-to-reclassify: applies a user-chosen category to a single transaction (e.g. fixing a wrong "Miscellaneous" guess, or flagging cash as "Given to Family"). */
    suspend fun reclassify(transactionId: Long, category: Category) {
        val entity = transactionDao.getById(transactionId) ?: return
        transactionDao.update(entity.copy(category = category.name))
    }

    /** Bulk tap-to-reclassify from a category card: re-tags every transaction currently under [from] as [to] (e.g. moving every wrongly-bucketed "Miscellaneous" cash withdrawal to "ATM Withdrawal"). */
    suspend fun reclassifyCategory(from: Category, to: Category) {
        transactionDao.getAll()
            .filter { it.category == from.name }
            .forEach { transactionDao.update(it.copy(category = to.name)) }
    }

    /** Retroactively renames every past transaction matching [merchantKey] to [label] (e.g. after teaching a new rule). */
    suspend fun relabelPastTransactions(merchantKey: String, label: String) {
        transactionDao.getAll()
            .filter { MerchantMatcher.isSameMerchant(it.merchant, merchantKey) && it.merchant != label }
            .forEach { transactionDao.update(it.copy(merchant = label)) }
    }

    /** When a different source reports the same purchase, note the confirmation instead of inserting a duplicate row. */
    private suspend fun linkAsMultiSourceConfirmation(existing: TransactionEntity, incoming: TransactionEntity) {
        if (existing.source == incoming.source) return
        val confirmationTag = "Confirmed via ${incoming.source}"
        if (existing.notes?.contains(confirmationTag) == true) return
        val updatedNotes = listOfNotNull(existing.notes, confirmationTag).joinToString("; ")
        transactionDao.update(existing.copy(notes = updatedNotes))
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
