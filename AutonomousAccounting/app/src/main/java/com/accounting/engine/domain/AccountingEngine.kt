package com.accounting.engine.domain

import androidx.room.withTransaction
import com.accounting.engine.data.AccountingDao
import com.accounting.engine.data.AppDatabase
import com.accounting.engine.data.entity.AccountType
import com.accounting.engine.data.entity.JournalEntryEntity
import com.accounting.engine.data.entity.LineItemEntity
import com.accounting.engine.data.entity.LineType
import com.accounting.engine.data.entity.TransactionEntity
import com.accounting.engine.data.entity.TransactionSource
import java.util.UUID

/**
 * Stages 2-4 of the pipeline: given an already-parsed [TransactionInput], auto-provisions
 * any missing sub-ledgers, generates a balanced double-entry journal entry, and persists the
 * whole thing atomically. Stage 1 (text -> [TransactionInput]) lives in [IntentParser].
 *
 * [database] (rather than just the [AccountingDao]) is required so the whole operation can be
 * wrapped in [withTransaction] - Room only honors atomic, multi-statement transactions when
 * driven from the [androidx.room.RoomDatabase] itself.
 */
class AccountingEngine(private val database: AppDatabase) {

    private val dao: AccountingDao get() = database.accountingDao()

    suspend fun processTransaction(
        input: TransactionInput,
        source: TransactionSource = TransactionSource.TEXT_INPUT
    ) = database.withTransaction {
        val transactionId = UUID.randomUUID().toString()
        val journalEntryId = UUID.randomUUID().toString()

        val lineItems = mutableListOf<LineItemEntity>()

        fun debit(accountId: String, amount: Double) =
            lineItems.add(LineItemEntity(UUID.randomUUID().toString(), journalEntryId, accountId, LineType.DEBIT, amount))

        fun credit(accountId: String, amount: Double) =
            lineItems.add(LineItemEntity(UUID.randomUUID().toString(), journalEntryId, accountId, LineType.CREDIT, amount))

        when (val intent = input.intent) {
            is FinancialIntent.SalaryReceived -> {
                val bankAcc = dao.getOrCreateAccount("Bank Account", AccountType.ASSET)
                val salaryAcc = dao.getOrCreateAccount("Salary Income", AccountType.REVENUE)

                debit(bankAcc.id, input.amount)
                credit(salaryAcc.id, input.amount)
            }

            is FinancialIntent.LoanGiven -> {
                val bankAcc = dao.getOrCreateAccount("Bank Account", AccountType.ASSET)
                val loanAcc = dao.getOrCreateAccount(
                    "Loans - ${intent.recipient}",
                    AccountType.ASSET,
                    parentName = "Loans & Advances"
                )

                debit(loanAcc.id, input.amount)
                credit(bankAcc.id, input.amount)
            }

            is FinancialIntent.CreditCardExpense -> {
                val expenseAcc = dao.getOrCreateAccount(intent.category, AccountType.EXPENSE)
                val ccPayableAcc = dao.getOrCreateAccount("Credit Card Payable", AccountType.LIABILITY)

                debit(expenseAcc.id, input.amount)
                credit(ccPayableAcc.id, input.amount)
            }

            is FinancialIntent.CreditCardBillPayment -> {
                val ccPayableAcc = dao.getOrCreateAccount("Credit Card Payable", AccountType.LIABILITY)
                val bankAcc = dao.getOrCreateAccount("Bank Account", AccountType.ASSET)

                debit(ccPayableAcc.id, input.amount)
                credit(bankAcc.id, input.amount)
            }

            is FinancialIntent.PaymentReceived -> {
                val bankAcc = dao.getOrCreateAccount("Bank Account", AccountType.ASSET)
                val arAcc = dao.getOrCreateAccount(
                    "AR - ${intent.sender}",
                    AccountType.ASSET,
                    parentName = "Accounts Receivable"
                )

                debit(bankAcc.id, input.amount)
                credit(arAcc.id, input.amount)
            }

            is FinancialIntent.ContingencyAllocation -> {
                val retainedEarningsAcc = dao.getOrCreateAccount("Retained Earnings", AccountType.EQUITY)
                val contingencyAcc = dao.getOrCreateAccount("Contingency Reserve", AccountType.EQUITY)

                val allocationAmount = if (input.amount > 0.0) {
                    input.amount
                } else {
                    val netDebitBalance = dao.getNetDebitBalance(retainedEarningsAcc.id)
                    // EQUITY carries a normal credit balance, so its balance is the negation of net-debit.
                    val currentBalance = if (AccountType.EQUITY.isDebitNormal) netDebitBalance else -netDebitBalance
                    (currentBalance * intent.percentage / 100.0).coerceAtLeast(0.0)
                }

                // Nothing to reserve yet (e.g. no accumulated retained earnings): skip silently.
                if (allocationAmount <= 0.0) return@withTransaction

                debit(retainedEarningsAcc.id, allocationAmount)
                credit(contingencyAcc.id, allocationAmount)
            }
        }

        // Validate Accounting Invariant: Sum(Debits) == Sum(Credits)
        val debits = lineItems.filter { it.type == LineType.DEBIT }.sumOf { it.amount }
        val credits = lineItems.filter { it.type == LineType.CREDIT }.sumOf { it.amount }

        require(kotlin.math.abs(debits - credits) < 0.0001) {
            "Accounting Fatal Error: Unbalanced entry. Debits ($debits) != Credits ($credits)"
        }

        // Persist atomically
        dao.insertTransactionRecord(TransactionEntity(transactionId, input.rawText, input.amount, System.currentTimeMillis(), source))
        dao.insertJournalEntry(JournalEntryEntity(journalEntryId, transactionId, System.currentTimeMillis(), input.rawText))
        dao.insertLineItems(lineItems)
    }
}
