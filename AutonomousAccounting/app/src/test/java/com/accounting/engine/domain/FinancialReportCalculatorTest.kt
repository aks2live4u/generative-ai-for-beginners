package com.accounting.engine.domain

import com.accounting.engine.data.entity.AccountEntity
import com.accounting.engine.data.entity.AccountType
import com.accounting.engine.data.entity.LineItemEntity
import com.accounting.engine.data.entity.LineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialReportCalculatorTest {

    private fun account(name: String, type: AccountType) =
        AccountEntity(id = name, name = name, type = type, parentAccountId = null, createdAt = 0L)

    private fun line(journalEntryId: String, accountId: String, type: LineType, amount: Double) =
        LineItemEntity(id = "$journalEntryId-$accountId-$type", journalEntryId = journalEntryId, accountId = accountId, type = type, amount = amount)

    @Test
    fun `salary posting increases net profit and bank balance`() {
        val bank = account("Bank Account", AccountType.ASSET)
        val salary = account("Salary Income", AccountType.REVENUE)

        val lineItems = listOf(
            line("je1", bank.id, LineType.DEBIT, 100_000.0),
            line("je1", salary.id, LineType.CREDIT, 100_000.0)
        )

        val balances = FinancialReportCalculator.computeBalances(listOf(bank, salary), lineItems)
        val pnl = FinancialReportCalculator.computeProfitAndLoss(balances)

        assertEquals(100_000.0, balances.first { it.account.id == bank.id }.balance, 0.0001)
        assertEquals(100_000.0, pnl.netProfit, 0.0001)
    }

    @Test
    fun `loan given has zero P&L impact and keeps the balance sheet balanced`() {
        val bank = account("Bank Account", AccountType.ASSET)
        val loan = account("Loans - Y", AccountType.ASSET)

        val lineItems = listOf(
            line("je1", loan.id, LineType.DEBIT, 2_002.0),
            line("je1", bank.id, LineType.CREDIT, 2_002.0)
        )

        val balances = FinancialReportCalculator.computeBalances(listOf(bank, loan), lineItems)
        val pnl = FinancialReportCalculator.computeProfitAndLoss(balances)
        val sheet = FinancialReportCalculator.computeBalanceSheet(balances)

        assertEquals(0.0, pnl.netProfit, 0.0001)
        assertEquals(-2_002.0, balances.first { it.account.id == bank.id }.balance, 0.0001)
        assertEquals(2_002.0, balances.first { it.account.id == loan.id }.balance, 0.0001)
        assertTrue(sheet.isBalanced)
    }

    @Test
    fun `unbalanced ledger fails the integrity check`() {
        val asset = account("Bank Account", AccountType.ASSET)
        val liability = account("Credit Card Payable", AccountType.LIABILITY)

        // Deliberately lopsided posting (as if a line item were dropped) to exercise the alert.
        val lineItems = listOf(line("je1", asset.id, LineType.DEBIT, 500.0))

        val balances = FinancialReportCalculator.computeBalances(listOf(asset, liability), lineItems)
        val sheet = FinancialReportCalculator.computeBalanceSheet(balances)

        assertFalse(sheet.isBalanced)
    }
}
