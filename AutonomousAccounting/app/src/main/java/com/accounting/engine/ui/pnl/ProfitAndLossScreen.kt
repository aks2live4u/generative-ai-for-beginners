package com.accounting.engine.ui.pnl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.accounting.engine.domain.AccountBalance
import com.accounting.engine.ui.AccountingViewModelFactory
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProfitAndLossScreen(viewModelFactory: AccountingViewModelFactory) {
    val viewModel: ProfitAndLossViewModel = viewModel(factory = viewModelFactory)
    val statement by viewModel.profitAndLoss.collectAsState()
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Profit & Loss", style = MaterialTheme.typography.titleLarge)
        Text(
            "Loan movements and credit-card bill settlements never appear here - only revenue and expense.",
            style = MaterialTheme.typography.bodySmall
        )

        if (statement == null) return@Column
        val pnl = statement!!

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            item { SectionHeader("Revenue") }
            items(pnl.revenueBalances) { BalanceRow(it, currency) }
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SectionHeader("Expenses") }
            items(pnl.expenseBalances) { BalanceRow(it, currency) }
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    "Net Profit: ${currency.format(pnl.netProfit)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (pnl.netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun BalanceRow(balance: AccountBalance, currency: NumberFormat) {
    Text("${balance.account.name}: ${currency.format(balance.balance)}")
}
