package com.accounting.engine.domain

/**
 * Stage 1 of the pipeline: turns a raw, single-sentence natural-language transaction
 * description into a [TransactionInput] (amount + inferred [FinancialIntent] + counterparty),
 * entirely on-device via keyword/regex matching - no network call, no database access.
 */
object IntentParser {

    private val AMOUNT_REGEX = Regex(
        """(?i)(\d[\d,]*(?:\.\d+)?)\s*(lakh|lakhs|crore|crores|k|thousand)?"""
    )
    private val AFTER_TO_REGEX = Regex("""(?i)\bto\s+([A-Za-z][A-Za-z .'-]*)""")
    private val AFTER_FROM_REGEX = Regex("""(?i)\bfrom\s+([A-Za-z][A-Za-z .'-]*)""")

    fun parse(rawText: String): TransactionInput? {
        val text = rawText.trim()
        if (text.isEmpty()) return null
        val lower = text.lowercase()

        val isContingency = lower.contains("contingency") || (lower.contains("set aside") && lower.contains("reserve"))
        val explicitPercentage = if (isContingency) extractPercentage(lower) else null

        val intent: FinancialIntent = when {
            isContingency -> FinancialIntent.ContingencyAllocation(percentage = explicitPercentage ?: 10.0)

            lower.contains("salary") && (lower.contains("received") || lower.contains("credited")) ->
                FinancialIntent.SalaryReceived

            (lower.contains("loan") || lower.contains("lent")) &&
                (lower.contains("gave") || lower.contains("given") || lower.contains("lent")) ->
                FinancialIntent.LoanGiven(recipient = extractName(text, AFTER_TO_REGEX) ?: "Unknown")

            lower.contains("credit card") && lower.contains("bill") ->
                FinancialIntent.CreditCardBillPayment

            lower.contains("credit card") && (lower.contains("spent") || lower.contains("paid")) ->
                FinancialIntent.CreditCardExpense(category = extractCategory(text) ?: "Miscellaneous")

            lower.contains("received") && lower.contains("from") ->
                FinancialIntent.PaymentReceived(sender = extractName(text, AFTER_FROM_REGEX) ?: "Unknown")

            else -> return null
        }

        // A bare "10%" describes a share of retained earnings, not a rupee amount - the engine
        // computes the actual figure itself. Any other contingency phrasing ("set aside 5000...")
        // does carry an explicit rupee amount.
        val amount = if (isContingency && explicitPercentage != null) 0.0 else (extractAmount(lower) ?: 0.0)

        if (amount <= 0.0 && intent !is FinancialIntent.ContingencyAllocation) return null

        val counterparty = when (intent) {
            is FinancialIntent.LoanGiven -> intent.recipient
            is FinancialIntent.PaymentReceived -> intent.sender
            else -> null
        }

        return TransactionInput(rawText = text, amount = amount, intent = intent, counterparty = counterparty)
    }

    private fun extractAmount(lower: String): Double? {
        val match = AMOUNT_REGEX.find(lower) ?: return null
        val numeric = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2].lowercase()) {
            "lakh", "lakhs" -> 100_000.0
            "crore", "crores" -> 10_000_000.0
            "k", "thousand" -> 1_000.0
            else -> 1.0
        }
        return numeric * multiplier
    }

    private fun extractPercentage(lower: String): Double? {
        val match = Regex("""(\d+(?:\.\d+)?)\s*%""").find(lower)
            ?: Regex("""(\d+(?:\.\d+)?)\s*percent""").find(lower)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractName(text: String, regex: Regex): String? {
        val match = regex.find(text) ?: return null
        return match.groupValues[1].trim().trimEnd('.', ',').ifEmpty { null }
    }

    /**
     * Picks the trailing "for X" / "on X" clause, e.g. "...on credit card for dining" -> "dining".
     * Uses the *last* occurrence of either keyword so an incidental "on credit card" earlier in
     * the sentence isn't mistaken for the category clause.
     */
    private fun extractCategory(text: String): String? {
        val lower = text.lowercase()
        val forIndex = lower.lastIndexOf(" for ")
        val onIndex = lower.lastIndexOf(" on ")
        val keywordLength = if (forIndex >= onIndex) 5 else 4
        val index = maxOf(forIndex, onIndex)
        if (index == -1) return null
        return text.substring(index + keywordLength).trim().trimEnd('.', ',').ifEmpty { null }
    }
}
