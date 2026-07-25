package com.accounting.engine.repository

import com.accounting.engine.data.AppDatabase
import com.accounting.engine.data.entity.AccountEntity
import com.accounting.engine.data.entity.JournalEntryEntity
import com.accounting.engine.data.entity.LineItemEntity
import com.accounting.engine.data.entity.TransactionEntity
import com.accounting.engine.data.entity.TransactionSource
import com.accounting.engine.domain.AccountBalance
import com.accounting.engine.domain.AccountingEngine
import com.accounting.engine.domain.BalanceSheet
import com.accounting.engine.domain.FinancialIntent
import com.accounting.engine.domain.FinancialReportCalculator
import com.accounting.engine.domain.IntentParser
import com.accounting.engine.domain.ProfitAndLossStatement
import com.accounting.engine.domain.TransactionInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Single entry point the UI (and the [com.accounting.engine.worker.ContingencyReserveWorker])
 * talks to. Wraps [IntentParser] + [AccountingEngine] for writes and re-derives every financial
 * statement reactively from the ledger for reads, satisfying the "real-time statement update"
 * pipeline stage without any statement-specific persisted state.
 */
class AccountingRepository(
    database: AppDatabase,
    private val engine: AccountingEngine
) {
    private val dao = database.accountingDao()

    val accounts: Flow<List<AccountEntity>> = dao.observeAccounts()
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()

    val accountBalances: Flow<List<AccountBalance>> =
        combine(dao.observeAccounts(), dao.observeLineItems()) { accounts, lineItems ->
            FinancialReportCalculator.computeBalances(accounts, lineItems)
        }

    val profitAndLoss: Flow<ProfitAndLossStatement> =
        accountBalances.map { balances -> FinancialReportCalculator.computeProfitAndLoss(balances) }

    val balanceSheet: Flow<BalanceSheet> =
        accountBalances.map { balances -> FinancialReportCalculator.computeBalanceSheet(balances) }

    /** Live-preview support for the Quick Input Bar: parse without touching the database. */
    fun preview(rawText: String) = IntentParser.parse(rawText)

    sealed class SubmitResult {
        data class Success(val intent: FinancialIntent) : SubmitResult()
        object UnrecognizedInput : SubmitResult()
        data class Failure(val message: String) : SubmitResult()
    }

    suspend fun submitTransaction(
        rawText: String,
        source: TransactionSource = TransactionSource.TEXT_INPUT
    ): SubmitResult {
        val input = IntentParser.parse(rawText) ?: return SubmitResult.UnrecognizedInput
        return try {
            engine.processTransaction(input, source)
            SubmitResult.Success(input.intent)
        } catch (e: IllegalArgumentException) {
            SubmitResult.Failure(e.message ?: "Unbalanced entry")
        }
    }

    /** A point-in-time snapshot of the whole ledger, used by [com.accounting.engine.export.ReportExporter]. */
    data class LedgerSnapshot(
        val accounts: List<AccountEntity>,
        val journalEntries: List<JournalEntryEntity>,
        val lineItems: List<LineItemEntity>
    )

    suspend fun getLedgerSnapshot(): LedgerSnapshot = LedgerSnapshot(
        accounts = dao.getAllAccounts(),
        journalEntries = dao.getAllJournalEntries(),
        lineItems = dao.getAllLineItems()
    )

    suspend fun runContingencyAllocation(percentage: Double) {
        engine.processTransaction(
            TransactionInput(
                rawText = "Automated contingency reserve allocation (${percentage}%)",
                amount = 0.0,
                intent = FinancialIntent.ContingencyAllocation(percentage)
            ),
            source = TransactionSource.TEXT_INPUT
        )
    }
}
