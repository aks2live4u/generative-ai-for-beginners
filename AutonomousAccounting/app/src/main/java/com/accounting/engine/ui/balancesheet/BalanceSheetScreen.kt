package com.accounting.engine.ui.balancesheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.accounting.engine.domain.BalanceSheet
import com.accounting.engine.ui.AccountingViewModelFactory
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BalanceSheetScreen(viewModelFactory: AccountingViewModelFactory) {
    val viewModel: BalanceSheetViewModel = viewModel(factory = viewModelFactory)
    val sheet by viewModel.balanceSheet.collectAsState()
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Balance Sheet", style = MaterialTheme.typography.titleLarge)

        val currentSheet = sheet ?: return@Column
        IntegrityBanner(currentSheet)

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            item { SectionHeader("Assets - ${currency.format(currentSheet.totalAssets)}") }
            items(currentSheet.assetBalances) { BalanceRow(it, currency) }
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SectionHeader("Liabilities - ${currency.format(currentSheet.totalLiabilities)}") }
            items(currentSheet.liabilityBalances) { BalanceRow(it, currency) }
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SectionHeader("Equity - ${currency.format(currentSheet.totalEquity)}") }
            items(currentSheet.equityBalances) { BalanceRow(it, currency) }
            item { Text("Current period net profit: ${currency.format(currentSheet.netProfit)}") }
        }
    }
}

@Composable
private fun IntegrityBanner(sheet: BalanceSheet) {
    if (sheet.isBalanced) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            "Integrity audit alert: Assets do not equal Liabilities + Equity.",
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
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
