package com.finsight.data.repository

import com.finsight.core.ai.FinanceDataProvider
import com.finsight.core.ai.MerchantPurposeRule
import com.finsight.core.model.Subscription
import com.finsight.core.model.Transaction
import com.finsight.data.PurposeRuleManager
import kotlinx.coroutines.runBlocking

/**
 * Bridges the encrypted on-device Room store to the pure-Kotlin [FinanceDataProvider] interface
 * that the AI engine (financial health score, savings detector, chat assistant) is built against.
 * Snapshot-based (no Flow) by design: the AI queries are run on demand, not continuously observed.
 */
class RoomFinanceDataProvider(
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val purposeRuleManager: PurposeRuleManager? = null
) : FinanceDataProvider {
    override fun allTransactions(): List<Transaction> = runBlocking { transactionRepository.getAll() }
    override fun subscriptions(): List<Subscription> = runBlocking { subscriptionRepository.getAll() }
    override fun purposeRules(): List<MerchantPurposeRule> = purposeRuleManager?.allRules() ?: emptyList()
}
