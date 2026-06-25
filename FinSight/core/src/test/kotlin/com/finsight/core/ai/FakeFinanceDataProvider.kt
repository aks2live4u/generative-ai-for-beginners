package com.finsight.core.ai

import com.finsight.core.model.Subscription
import com.finsight.core.model.Transaction

class FakeFinanceDataProvider(
    private val transactions: List<Transaction>,
    private val subscriptions: List<Subscription> = emptyList()
) : FinanceDataProvider {
    override fun allTransactions(): List<Transaction> = transactions
    override fun subscriptions(): List<Subscription> = subscriptions
}
