package com.accounting.engine.data.entity

/**
 * The five root classifications of the accounting equation:
 * Assets = Liabilities + Equity, with Revenue/Expense rolling into Equity via P&L.
 */
enum class AccountType {
    ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE;

    /** Whether a debit posting increases (true) or decreases (false) this account's balance. */
    val isDebitNormal: Boolean
        get() = this == ASSET || this == EXPENSE

    companion object {
        /** Name of the master root ledger account every sub-account ultimately rolls up under. */
        fun rootAccountName(type: AccountType): String = when (type) {
            ASSET -> "Assets"
            LIABILITY -> "Liabilities"
            EQUITY -> "Equity"
            REVENUE -> "Revenue"
            EXPENSE -> "Expenses"
        }
    }
}

enum class LineType { DEBIT, CREDIT }

enum class TransactionSource { TEXT_INPUT, VOICE, SMS_PARSER }
