package com.finsight.core.ai.llm

import com.finsight.core.ai.FinanceDataProvider
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Builds the plain-text financial context sent alongside a question/request to the LLM. Kept in
 * :core (pure Kotlin, unit-testable) so the exact data being sent off-device is reviewable and
 * testable independent of the Android HTTP/network code that actually calls the LLM API.
 */
object FinanceContextBuilder {

    /**
     * Compact summary for the chat assistant: this month's totals, category breakdown,
     * subscriptions, and recent transactions. Deliberately omits [Transaction.rawText] - the chat
     * assistant only needs merchant/amount/date/category, not the original SMS/email text.
     */
    fun buildChatContext(data: FinanceDataProvider, now: LocalDate = LocalDate.now(), maxTransactions: Int = 40): String {
        val month = YearMonth.from(now)
        val txs = data.allTransactions()
        val thisMonth = txs.filter { YearMonth.from(it.date.atZone(ZoneId.systemDefault()).toLocalDate()) == month }
        val income = thisMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = thisMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val byCategory = thisMonth.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category.displayName }
            .map { (category, list) -> category to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
        val subscriptions = data.subscriptions()

        return buildString {
            appendLine("Current date: $now")
            appendLine("This month's income: Rs.${"%.0f".format(income)}")
            appendLine("This month's expenses: Rs.${"%.0f".format(expense)}")
            if (byCategory.isNotEmpty()) {
                appendLine("Spending by category this month:")
                byCategory.forEach { (category, amount) -> appendLine("- $category: Rs.${"%.0f".format(amount)}") }
            }
            if (subscriptions.isNotEmpty()) {
                appendLine("Subscriptions:")
                subscriptions.forEach {
                    appendLine("- ${it.serviceName}: Rs.${"%.0f".format(it.monthlyCost)}/month (renews ${it.renewalDate})")
                }
            }
            appendLine("Recent transactions (most recent first):")
            txs.sortedByDescending { it.date }.take(maxTransactions).forEach { tx -> appendLine("- ${transactionLine(tx)}") }
        }
    }

    /**
     * Richer per-transaction dump used by the Smart Scan feature (duplicate/fraud detection,
     * insurance discovery), which needs the original SMS/email/notification text to reason about
     * near-duplicates across sources. [Transaction.rawText] is passed through
     * [LlmRedaction.redact] first so long account/card numbers never leave the device.
     */
    fun buildSmartScanContext(transactions: List<Transaction>, maxTransactions: Int = 150): String =
        buildString {
            transactions.sortedByDescending { it.date }.take(maxTransactions).forEach { tx ->
                appendLine("- ${transactionLine(tx)} | source=${tx.source} | text=\"${LlmRedaction.redact(tx.rawText)}\"")
            }
        }

    private fun transactionLine(tx: Transaction): String {
        val date = tx.date.atZone(ZoneId.systemDefault()).toLocalDate()
        return "$date: ${tx.type} Rs.${"%.0f".format(tx.amount)} at ${LlmRedaction.redact(tx.merchant)} " +
            "(${tx.category.displayName}, ${tx.paymentMethod})"
    }
}
