package com.finsight.core.ai

import com.finsight.core.categorize.CategoryEngine
import com.finsight.core.model.Category
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Template/rule-based natural-language assistant. Matches the question against a fixed set of
 * intents (spend-by-category, subscriptions, biggest expenses, savings advice, prediction,
 * summary, EMI) and answers by querying [FinanceDataProvider] — entirely offline, no LLM call,
 * deterministic and unit-testable.
 */
object ChatAssistantEngine {

    private val emiWordBoundary = Regex("\\bemi\\b")

    fun answer(question: String, data: FinanceDataProvider, now: LocalDate = LocalDate.now()): String {
        val lower = question.lowercase()
        return when {
            "subscription" in lower -> answerSubscriptions(data)
            "predict" in lower -> answerPrediction(data, now)
            "biggest" in lower || "largest" in lower || "top expense" in lower -> answerBiggestExpenses(data, lower, now)
            "where" in lower && ("save" in lower || "overspend" in lower) -> answerSavingsAdvice(data, now)
            "how much can i save" in lower || ("save money" in lower) -> answerSavingsAdvice(data, now)
            "summar" in lower -> answerSummary(data, now)
            // Word-boundary match: "emi" as a plain substring also hits "premium", "anemia",
            // "academic", etc., misrouting unrelated questions to the EMI answer.
            emiWordBoundary.containsMatchIn(lower) -> answerEmiPayments(data, lower, now)
            looksLikeFinanceQuestion(lower) -> answerSpendQuery(data, lower, now)
            else -> answerFallback()
        }
    }

    // Generic chit-chat ("hi", "shutup", "why are you saying that") used to silently fall through
    // to answerSpendQuery, which always recomputed and repeated a spend total - making the
    // assistant look broken/repetitive for anything it didn't recognize. Only route to the spend
    // query when the question actually contains a money/spending signal, a known category/group
    // name, a known merchant, or an explicit time-period phrase.
    private val financeSignalKeywords = listOf(
        "spend", "spent", "spending", "cost", "costs", "expense", "expenses", "income", "earn",
        "money", "amount", "transaction", "bill", "bills", "paid", "pay", "budget", "balance",
        "saving", "savings", "rs.", "rs ", "inr", "₹", "total", "afford", "much did i", "how much"
    )

    private fun looksLikeFinanceQuestion(question: String): Boolean {
        if (financeSignalKeywords.any { question.contains(it) }) return true
        if (groupKeywords.keys.any { question.contains(it) }) return true
        if (Category.entries.any { question.contains(it.displayName.lowercase()) }) return true
        if (findMerchantKeywordInQuestion(question) != null) return true
        val periodPhrases = listOf(
            "all time", "overall", "ever", "today", "this week", "last week",
            "last month", "this month", "last year", "this year"
        )
        return periodPhrases.any { question.contains(it) }
    }

    private fun answerFallback(): String =
        "I can help with your finances - try asking things like \"How much did I spend on food this month?\", " +
            "\"What are my subscriptions?\", \"Where can I save money?\", or \"Predict next month's expenses.\""

    private fun answerSubscriptions(data: FinanceDataProvider): String {
        val subs = data.subscriptions()
        if (subs.isEmpty()) return "I didn't find any recurring subscriptions yet."
        val total = subs.sumOf { it.monthlyCost }
        val lines = subs.joinToString("\n") { "- ${it.serviceName}: Rs.${"%.0f".format(it.monthlyCost)}/month (renews ${it.renewalDate})" }
        return "You're paying for ${subs.size} subscriptions, Rs.${"%.0f".format(total)}/month in total:\n$lines"
    }

    private fun answerPrediction(data: FinanceDataProvider, now: LocalDate): String {
        val last3Months = (1..3).map { now.minusMonths(it.toLong()) }
            .map { java.time.YearMonth.from(it) }
        val totals = last3Months.mapNotNull { month ->
            val total = data.transactionsForMonth(month).filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            if (total > 0) total else null
        }
        if (totals.isEmpty()) return "I don't have enough history yet to predict next month's expenses."
        val avg = totals.average()
        return "Based on your last ${totals.size} month(s), I predict you'll spend around Rs.${"%.0f".format(avg)} next month."
    }

    private fun answerBiggestExpenses(data: FinanceDataProvider, question: String, now: LocalDate): String {
        val range = resolvePeriodRange(question, now)
        val expenses = filterByPeriod(data.allTransactions(), range)
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.amount }
            .take(5)
        if (expenses.isEmpty()) return "I couldn't find any expenses for ${range.label}."
        val lines = expenses.joinToString("\n") { "- ${it.merchant}: Rs.${"%.0f".format(it.amount)} (${it.category.displayName})" }
        return "Your biggest expenses (${range.label}):\n$lines"
    }

    private fun answerSavingsAdvice(data: FinanceDataProvider, now: LocalDate): String {
        val opportunities = SavingsDetector.detect(data.allTransactions(), data.subscriptions(), now)
        if (opportunities.isEmpty()) return "I don't see any obvious savings opportunities right now — nice work!"
        val totalAnnual = opportunities.sumOf { it.estimatedAnnualSavings }
        val lines = opportunities.take(5).joinToString("\n") { "- ${it.title}: save up to Rs.${"%.0f".format(it.estimatedAnnualSavings)}/year" }
        return "Here's where you could save (up to Rs.${"%.0f".format(totalAnnual)}/year total):\n$lines"
    }

    private fun answerSummary(data: FinanceDataProvider, now: LocalDate): String {
        val month = java.time.YearMonth.from(now)
        val txs = data.transactionsForMonth(month)
        val income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val savings = income - expense
        val rate = if (income > 0) (savings / income * 100) else 0.0
        return "This month: income Rs.${"%.0f".format(income)}, expenses Rs.${"%.0f".format(expense)}, " +
            "net savings Rs.${"%.0f".format(savings)} (${"%.0f".format(rate)}% savings rate)."
    }

    private fun answerEmiPayments(data: FinanceDataProvider, question: String, now: LocalDate): String {
        val range = resolvePeriodRange(question, now)
        val emis = filterByPeriod(data.allTransactions(), range).filter { it.category == Category.EMI }
        if (emis.isEmpty()) return "You don't have any EMI payments on record for ${range.label}."
        val total = emis.sumOf { it.amount }
        val lines = emis.sortedByDescending { it.date }.take(10)
            .joinToString("\n") { "- ${it.merchant}: Rs.${"%.0f".format(it.amount)}" }
        return "EMI payments for ${range.label} (Rs.${"%.0f".format(total)} total). Ask \"EMI this year\" or \"EMI all time\" for a different period:\n$lines"
    }

    private val groupKeywords: Map<String, com.finsight.core.model.CategoryGroup> = mapOf(
        "food" to com.finsight.core.model.CategoryGroup.FOOD,
        "shopping" to com.finsight.core.model.CategoryGroup.SHOPPING,
        "travel" to com.finsight.core.model.CategoryGroup.TRANSPORTATION,
        "transport" to com.finsight.core.model.CategoryGroup.TRANSPORTATION,
        "utilities" to com.finsight.core.model.CategoryGroup.UTILITIES,
        "utility" to com.finsight.core.model.CategoryGroup.UTILITIES,
        "entertainment" to com.finsight.core.model.CategoryGroup.ENTERTAINMENT,
        "health" to com.finsight.core.model.CategoryGroup.HEALTH,
        "education" to com.finsight.core.model.CategoryGroup.EDUCATION
    )

    private fun answerSpendQuery(data: FinanceDataProvider, question: String, now: LocalDate): String {
        val range = resolvePeriodRange(question, now)
        val transactions = filterByPeriod(data.allTransactions(), range).filter { it.type == TransactionType.EXPENSE }

        val matchedGroup = groupKeywords.entries.find { question.contains(it.key) }?.value
        val matchedCategory = Category.entries.find { question.contains(it.displayName.lowercase()) }
        val merchantMatch = transactions.map { it.merchant }.distinct()
            .find { question.contains(it.lowercase()) }
            ?: findMerchantKeywordInQuestion(question)

        val filtered = when {
            merchantMatch != null -> transactions.filter { it.merchant.equals(merchantMatch, ignoreCase = true) || it.merchant.lowercase().contains(merchantMatch.lowercase()) }
            matchedCategory != null -> transactions.filter { it.category == matchedCategory }
            matchedGroup != null -> transactions.filter { it.category.group == matchedGroup }
            else -> transactions
        }

        val total = filtered.sumOf { it.amount }
        val label = merchantMatch ?: matchedCategory?.displayName ?: matchedGroup?.name?.lowercase() ?: "in total"
        if (filtered.isEmpty()) return "I couldn't find any spending on $label for ${range.label}."
        return "You spent Rs.${"%.0f".format(total)} on $label (${range.label})."
    }

    private fun findMerchantKeywordInQuestion(question: String): String? {
        val knownMerchants = listOf("amazon", "flipkart", "myntra", "swiggy", "zomato", "uber", "ola", "netflix")
        return knownMerchants.find { question.contains(it) }
    }

    private data class PeriodRange(val start: LocalDate, val end: LocalDate, val label: String)

    private fun resolvePeriodRange(question: String, now: LocalDate): PeriodRange = when {
        "all time" in question || "overall" in question || "ever" in question ->
            PeriodRange(LocalDate.MIN, now, "all time")
        "today" in question -> PeriodRange(now, now, "today")
        "this week" in question -> PeriodRange(now.with(DayOfWeek.MONDAY), now, "this week")
        "last week" in question -> {
            val lastWeekMonday = now.with(DayOfWeek.MONDAY).minusWeeks(1)
            PeriodRange(lastWeekMonday, lastWeekMonday.plusDays(6), "last week")
        }
        "last month" in question -> {
            val month = java.time.YearMonth.from(now).minusMonths(1)
            PeriodRange(month.atDay(1), month.atEndOfMonth(), "last month")
        }
        "this month" in question -> {
            val month = java.time.YearMonth.from(now)
            PeriodRange(month.atDay(1), month.atEndOfMonth(), "this month")
        }
        "last year" in question -> PeriodRange(LocalDate.of(now.year - 1, 1, 1), LocalDate.of(now.year - 1, 12, 31), "last year")
        "this year" in question -> PeriodRange(LocalDate.of(now.year, 1, 1), now, "this year")
        else -> {
            // No period keyword mentioned - default to "this month" so totals match the rest of
            // the app (Dashboard/Insights are always scoped to the current month) instead of
            // silently summing every transaction ever recorded, which previously made chat
            // answers (e.g. "EMI payments") wildly larger than the equivalent dashboard figure.
            val month = java.time.YearMonth.from(now)
            PeriodRange(month.atDay(1), month.atEndOfMonth(), "this month")
        }
    }

    private fun filterByPeriod(transactions: List<Transaction>, range: PeriodRange): List<Transaction> {
        return transactions.filter { tx ->
            val date = tx.date.atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(range.start) && !date.isAfter(range.end)
        }
    }
}
