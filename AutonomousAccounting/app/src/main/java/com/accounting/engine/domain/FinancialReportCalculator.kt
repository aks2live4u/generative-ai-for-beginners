package com.accounting.engine.domain

import com.accounting.engine.data.entity.AccountEntity
import com.accounting.engine.data.entity.AccountType
import com.accounting.engine.data.entity.LineItemEntity
import com.accounting.engine.data.entity.LineType

data class AccountBalance(val account: AccountEntity, val balance: Double)

data class ProfitAndLossStatement(
    val revenueBalances: List<AccountBalance>,
    val expenseBalances: List<AccountBalance>
) {
    val totalRevenue: Double get() = revenueBalances.sumOf { it.balance }
    val totalExpense: Double get() = expenseBalances.sumOf { it.balance }
    val netProfit: Double get() = totalRevenue - totalExpense
}

data class BalanceSheet(
    val assetBalances: List<AccountBalance>,
    val liabilityBalances: List<AccountBalance>,
    val equityBalances: List<AccountBalance>,
    /** Undistributed profit/loss for the period, folded into equity for the integrity check. */
    val netProfit: Double
) {
    val totalAssets: Double get() = assetBalances.sumOf { it.balance }
    val totalLiabilities: Double get() = liabilityBalances.sumOf { it.balance }
    val totalEquity: Double get() = equityBalances.sumOf { it.balance } + netProfit

    /** True when Assets == Liabilities + Equity within floating-point tolerance. */
    val isBalanced: Boolean get() = kotlin.math.abs(totalAssets - (totalLiabilities + totalEquity)) < 0.01
}

/**
 * Stage 4 of the pipeline: recomputes ledger balances and derived statements from the raw
 * accounts + line-item ledger. Pure, stateless, and side-effect free so it can be recalculated
 * on every ledger change and safely called from a Flow combine operator.
 */
object FinancialReportCalculator {

    fun computeBalances(accounts: List<AccountEntity>, lineItems: List<LineItemEntity>): List<AccountBalance> {
        val netDebitByAccount: Map<String, Double> = lineItems
            .groupBy { it.accountId }
            .mapValues { (_, items) ->
                items.sumOf { if (it.type == LineType.DEBIT) it.amount else -it.amount }
            }

        return accounts.map { account ->
            val netDebit = netDebitByAccount[account.id] ?: 0.0
            val balance = if (account.type.isDebitNormal) netDebit else -netDebit
            AccountBalance(account, balance)
        }
    }

    fun computeProfitAndLoss(balances: List<AccountBalance>): ProfitAndLossStatement {
        return ProfitAndLossStatement(
            revenueBalances = balances.filter { it.account.type == AccountType.REVENUE },
            expenseBalances = balances.filter { it.account.type == AccountType.EXPENSE }
        )
    }

    fun computeBalanceSheet(balances: List<AccountBalance>): BalanceSheet {
        val netProfit = computeProfitAndLoss(balances).netProfit
        return BalanceSheet(
            assetBalances = balances.filter { it.account.type == AccountType.ASSET },
            liabilityBalances = balances.filter { it.account.type == AccountType.LIABILITY },
            equityBalances = balances.filter { it.account.type == AccountType.EQUITY },
            netProfit = netProfit
        )
    }
}
