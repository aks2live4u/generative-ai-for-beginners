package com.accounting.engine.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.accounting.engine.data.AppDatabase
import com.accounting.engine.data.entity.AccountType
import com.accounting.engine.data.entity.JournalEntryEntity
import com.accounting.engine.data.entity.LineItemEntity
import com.accounting.engine.data.entity.LineType
import com.accounting.engine.data.entity.TransactionEntity
import com.accounting.engine.data.entity.TransactionSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Phase 1 requirement: "Write Unit Tests validating zero-variance debit/credit balancing."
 * Runs against a real (unencrypted, in-memory) Room instance since SQLCipher's native library
 * and Room's generated SQL both require an actual Android SQLite provider.
 */
@RunWith(AndroidJUnit4::class)
class AccountingEngineInstrumentedTest {

    private lateinit var database: AppDatabase
    private lateinit var engine: AccountingEngine

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        engine = AccountingEngine(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun assertBalanced(journalEntryId: String) = runBlocking {
        val lines = database.accountingDao().getLineItemsForJournalEntry(journalEntryId)
        val debits = lines.filter { it.type == LineType.DEBIT }.sumOf { it.amount }
        val credits = lines.filter { it.type == LineType.CREDIT }.sumOf { it.amount }
        assertEquals(debits, credits, 0.0001)
    }

    @Test
    fun everyIntentProducesABalancedJournalEntry() = runBlocking {
        val inputs = listOf(
            TransactionInput("Salary received of 1 lakh", 100_000.0, FinancialIntent.SalaryReceived),
            TransactionInput("Gave a loan of 2,002 to Y", 2_002.0, FinancialIntent.LoanGiven("Y")),
            TransactionInput("Spent 2,000 on credit card for dining", 2_000.0, FinancialIntent.CreditCardExpense("dining")),
            TransactionInput("Paid credit card bill 2,000", 2_000.0, FinancialIntent.CreditCardBillPayment),
            TransactionInput("Received 2,000 from X", 2_000.0, FinancialIntent.PaymentReceived("X"))
        )

        inputs.forEach { engine.processTransaction(it) }

        val journalEntries = database.accountingDao().getAllJournalEntries()
        assertEquals(inputs.size, journalEntries.size)
        journalEntries.forEach { assertBalanced(it.id) }
    }

    @Test
    fun contingencyAllocationReservesAPercentOfRetainedEarnings() = runBlocking {
        val dao = database.accountingDao()

        // Seed Retained Earnings with a 100,000 credit balance, as a prior period's closing
        // entry would (paired here with a Bank debit purely to keep the seed entry balanced).
        val retainedEarnings = dao.getOrCreateAccount("Retained Earnings", AccountType.EQUITY)
        val bank = dao.getOrCreateAccount("Bank Account", AccountType.ASSET)
        val seedEntryId = UUID.randomUUID().toString()
        dao.insertTransactionRecord(
            TransactionEntity(seedEntryId, "Prior period closing entry", 100_000.0, System.currentTimeMillis(), TransactionSource.TEXT_INPUT)
        )
        dao.insertJournalEntry(JournalEntryEntity(seedEntryId, seedEntryId, System.currentTimeMillis(), "Prior period closing entry"))
        dao.insertLineItems(
            listOf(
                LineItemEntity(UUID.randomUUID().toString(), seedEntryId, bank.id, LineType.DEBIT, 100_000.0),
                LineItemEntity(UUID.randomUUID().toString(), seedEntryId, retainedEarnings.id, LineType.CREDIT, 100_000.0)
            )
        )

        engine.processTransaction(TransactionInput("Set aside 10% for contingency", 0.0, FinancialIntent.ContingencyAllocation(10.0)))

        val contingencyReserve = dao.getOrCreateAccount("Contingency Reserve", AccountType.EQUITY)
        val reserveCredits = dao.getAllLineItems()
            .filter { it.accountId == contingencyReserve.id && it.type == LineType.CREDIT }
            .sumOf { it.amount }

        assertEquals(10_000.0, reserveCredits, 0.0001) // 10% of the 100,000 retained earnings balance
        dao.getAllJournalEntries().forEach { assertBalanced(it.id) }
    }
}
