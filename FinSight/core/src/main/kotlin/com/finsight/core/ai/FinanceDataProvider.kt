package com.finsight.core.ai

import com.finsight.core.model.Subscription
import com.finsight.core.model.Transaction
import java.time.YearMonth

/**
 * Abstraction the chat engine queries against. The Android app implements this on top of the
 * encrypted Room database; tests implement it with an in-memory fake. Keeping this interface in
 * :core means the assistant's reasoning logic is fully unit-testable without Android/Room.
 */
interface FinanceDataProvider {
    fun allTransactions(): List<Transaction>
    fun transactionsForMonth(month: YearMonth): List<Transaction> =
        allTransactions().filter { YearMonth.from(it.date.atZone(java.time.ZoneId.systemDefault()).toLocalDate()) == month }
    fun subscriptions(): List<Subscription>

    /** Personal merchant-to-[com.finsight.core.model.Purpose] overrides taught via chat. Empty by default. */
    fun purposeRules(): List<MerchantPurposeRule> = emptyList()
}
