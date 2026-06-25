package com.personalfinanceai.data.repository

import com.personalfinanceai.core.ai.FinanceDataProvider
import com.personalfinanceai.core.model.Subscription
import com.personalfinanceai.core.model.Transaction
import kotlinx.coroutines.runBlocking

/**
 * Bridges the encrypted on-device Room store to the pure-Kotlin [FinanceDataProvider] interface
 * that the AI engine (financial health score, savings detector, chat assistant) is built against.
 * Snapshot-based (no Flow) by design: the AI queries are run on demand, not continuously observed.
 */
class RoomFinanceDataProvider(
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository
) : FinanceDataProvider {
    override fun allTransactions(): List<Transaction> = runBlocking { transactionRepository.getAll() }
    override fun subscriptions(): List<Subscription> = runBlocking { subscriptionRepository.getAll() }
}
