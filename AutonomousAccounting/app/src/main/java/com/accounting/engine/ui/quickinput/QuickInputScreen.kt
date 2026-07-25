package com.accounting.engine.ui.quickinput

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.accounting.engine.domain.FinancialIntent
import com.accounting.engine.domain.TransactionInput
import com.accounting.engine.ui.AccountingViewModelFactory
import java.text.NumberFormat
import java.util.Locale

@Composable
fun QuickInputScreen(viewModelFactory: AccountingViewModelFactory) {
    val viewModel: QuickInputViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Quick Input",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Describe a transaction in plain language - the engine infers the journal entry.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.text,
                onValueChange = viewModel::onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. Salary received of 1 lakh") },
                singleLine = true
            )
            IconButton(onClick = viewModel::submit) {
                Icon(Icons.Filled.Send, contentDescription = "Submit transaction")
            }
        }

        uiState.livePreview?.let { preview ->
            JournalPreviewCard(preview)
        }

        when (val feedback = uiState.feedback) {
            is SubmitFeedback.Posted -> Text(
                "Posted: \"${feedback.description}\"",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            is SubmitFeedback.Unrecognized -> Text(
                "Couldn't understand that transaction - try rephrasing.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            is SubmitFeedback.Error -> Text(
                feedback.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            SubmitFeedback.Idle -> Unit
        }
    }
}

@Composable
private fun JournalPreviewCard(preview: TransactionInput) {
    val (debitLabel, creditLabel) = describeJournalLines(preview.intent)
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Live preview", style = MaterialTheme.typography.labelMedium)
            Text("Dr  $debitLabel  ${currency.format(preview.amount)}")
            Text("Cr  $creditLabel  ${currency.format(preview.amount)}")
        }
    }
}

/** Human-readable debit/credit account labels for the live preview - mirrors [com.accounting.engine.domain.AccountingEngine]. */
private fun describeJournalLines(intent: FinancialIntent): Pair<String, String> = when (intent) {
    is FinancialIntent.SalaryReceived -> "Bank Account" to "Salary Income"
    is FinancialIntent.LoanGiven -> "Loans - ${intent.recipient}" to "Bank Account"
    is FinancialIntent.CreditCardExpense -> intent.category to "Credit Card Payable"
    is FinancialIntent.CreditCardBillPayment -> "Credit Card Payable" to "Bank Account"
    is FinancialIntent.PaymentReceived -> "Bank Account" to "AR - ${intent.sender}"
    is FinancialIntent.ContingencyAllocation -> "Retained Earnings" to "Contingency Reserve"
}
