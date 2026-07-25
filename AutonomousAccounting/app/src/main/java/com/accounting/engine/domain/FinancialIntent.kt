package com.accounting.engine.domain

/** The financial meaning inferred from a raw natural-language transaction description. */
sealed class FinancialIntent {
    object SalaryReceived : FinancialIntent()
    data class LoanGiven(val recipient: String) : FinancialIntent()
    data class CreditCardExpense(val category: String) : FinancialIntent()
    object CreditCardBillPayment : FinancialIntent()
    data class PaymentReceived(val sender: String) : FinancialIntent()

    /** "Set aside X% for contingency": reallocates retained earnings into a protected reserve. */
    data class ContingencyAllocation(val percentage: Double) : FinancialIntent()
}

data class TransactionInput(
    val rawText: String,
    val amount: Double,
    val intent: FinancialIntent,
    val counterparty: String? = null
)
