package com.personalfinanceai.core.ai

import com.personalfinanceai.core.categorize.CategoryEngine
import com.personalfinanceai.core.model.Category
import com.personalfinanceai.core.model.Transaction
import com.personalfinanceai.core.model.TransactionType
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

    fun answer(question: String, data: FinanceDataProvider, now: LocalDate = LocalDate.now()): String {
        val lower = question.lowercase()
        return when {
            "subscription" in lower -> answerSubscriptions(data)
            "predict" in lower -> answerPrediction(data, now)
            "biggest" in lower || "largest" in lower || "top expense" in lower -> answerBiggestExpenses(data, lower, now)
            "where" in lower && ("save" in lower || "overspend" in lower) -> answerSavingsAdvice(data, now)
            "how much can i save" in lower || ("save money" in lower) -> answerSavingsAdvice(data, now)
            "summar" in lower -> answerSummary(data, now)
            "emi" in lower -> answerEmiPayments(data)
            else -> answerSpendQuery(data, lower, now)
        }
    }

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
        if (expenses.isEmpty()) return "I couldn't find any expenses in that period."
        val lines = expenses.joinToString("\n") { "- ${it.merchant}: Rs.${"%.0f".format(it.amount)} (${it.category.displayName})" }
        return "Your biggest expenses:\n$lines"
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

    private fun answerEmiPayments(data: FinanceDataProvider): String {
        val emis = data.allTransactions().filter { it.category == Category.EMI }
        if (emis.isEmpty()) return "You don't have any EMI payments on record."
        val total = emis.sumOf { it.amount }
        val lines = emis.sortedByDescending { it.date }.take(10)
            .joinToString("\n") { "- ${it.merchant}: Rs.${"%.0f".format(it.amount)}" }
        return "EMI payments on record (Rs.${"%.0f".format(total)} total):\n$lines"
    }

    private val groupKeywords: Map<String, com.personalfinanceai.core.model.CategoryGroup> = mapOf(
        "food" to com.personalfinanceai.core.model.CategoryGroup.FOOD,
        "shopping" to com.personalfinanceai.core.model.CategoryGroup.SHOPPING,
        "travel" to com.personalfinanceai.core.model.CategoryGroup.TRANSPORTATION,
        "transport" to com.personalfinanceai.core.model.CategoryGroup.TRANSPORTATION,
        "utilities" to com.personalfinanceai.core.model.CategoryGroup.UTILITIES,
        "utility" to com.personalfinanceai.core.model.CategoryGroup.UTILITIES,
        "entertainment" to com.personalfinanceai.core.model.CategoryGroup.ENTERTAINMENT,
        "health" to com.personalfinanceai.core.model.CategoryGroup.HEALTH,
        "education" to com.personalfinanceai.core.model.CategoryGroup.EDUCATION
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
        if (filtered.isEmpty()) return "I couldn't find any spending on $label for that period."
        return "You spent Rs.${"%.0f".format(total)} on $label."
    }

    private fun findMerchantKeywordInQuestion(question: String): String? {
        val knownMerchants = listOf("amazon", "flipkart", "myntra", "swiggy", "zomato", "uber", "ola", "netflix")
        return knownMerchants.find { question.contains(it) }
    }

    private data class PeriodRange(val start: LocalDate, val end: LocalDate, val explicit: Boolean)

    private fun resolvePeriodRange(question: String, now: LocalDate): PeriodRange = when {
        "today" in question -> PeriodRange(now, now, true)
        "this week" in question -> PeriodRange(now.with(DayOfWeek.MONDAY), now, true)
        "last week" in question -> {
            val lastWeekMonday = now.with(DayOfWeek.MONDAY).minusWeeks(1)
            PeriodRange(lastWeekMonday, lastWeekMonday.plusDays(6), true)
        }
        "last month" in question -> {
            val month = java.time.YearMonth.from(now).minusMonths(1)
            PeriodRange(month.atDay(1), month.atEndOfMonth(), true)
        }
        "this month" in question -> {
            val month = java.time.YearMonth.from(now)
            PeriodRange(month.atDay(1), month.atEndOfMonth(), true)
        }
        "last year" in question -> PeriodRange(LocalDate.of(now.year - 1, 1, 1), LocalDate.of(now.year - 1, 12, 31), true)
        "this year" in question -> PeriodRange(LocalDate.of(now.year, 1, 1), now, true)
        else -> PeriodRange(LocalDate.MIN, now, false)
    }

    private fun filterByPeriod(transactions: List<Transaction>, range: PeriodRange): List<Transaction> {
        if (!range.explicit) return transactions
        return transactions.filter { tx ->
            val date = tx.date.atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(range.start) && !date.isAfter(range.end)
        }
    }
}
