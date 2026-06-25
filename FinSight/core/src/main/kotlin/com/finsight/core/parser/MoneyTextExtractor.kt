package com.finsight.core.parser

import com.finsight.core.model.PaymentMethod
import com.finsight.core.model.TransactionType

/**
 * Shared text-extraction helpers used by the SMS, email and notification parsers.
 * Kept regex-based (no ML model) so categorization/extraction is fully offline and deterministic.
 */
object MoneyTextExtractor {

    private val amountRegex = Regex(
        """(?:rs\.?|inr|₹)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val debitKeywords = listOf(
        "debited", "spent", "paid", "withdrawn", "purchase of", "debit of", "sent", "deducted"
    )
    private val creditKeywords = listOf(
        "credited", "received", "deposited", "credit of", "refunded", "cashback of"
    )

    // Marketing/scam SMS, push and email templates frequently mention an amount (loan limit,
    // discount, prize) without describing an actual transaction. These phrases are common across
    // such messages and rarely appear in genuine bank/merchant transaction alerts, so any match
    // disqualifies the message before amount/type extraction runs.
    private val promotionalKeywords = listOf(
        "loan offer", "personal loan offer", "pre-approved", "pre approved", "instant loan",
        "get a loan", "eligible for a loan", "apply now", "click here", "click the link",
        "claim now", "claim your", "congratulations", "you have won", "you've won", "you won",
        "lucky draw", "lottery", "win cash", "limited period offer", "limited time offer",
        "exclusive offer", "t&c apply", "t&amp;c apply", "terms and conditions apply",
        "subscribe now", "unsubscribe", "promo code", "% off", "sale is live", "register now",
        "get upto", "get up to", "free gift", "act now", "this offer expires", "offer expires"
    )

    // Bank SMS/email templates routinely mention a second amount alongside the actual transaction
    // value - available balance, credit limit, minimum/total due - and that figure is often far
    // larger than what was actually debited/credited. A naive "first amount in the text" extraction
    // picks these up and reports wildly inflated numbers. Any amount whose immediately preceding
    // text matches one of these phrases is skipped in favour of the next (or first) candidate.
    private val nonTransactionAmountContext = listOf(
        "avl bal", "available balance", "avl limit", "available limit", "avl lmt",
        "credit limit", "total due", "minimum due", "min due", "total amt due",
        "outstanding amount", "outstanding balance", "outstanding bal", "previous balance",
        "current balance", "balance is", "limit is", "bal is"
    )

    /**
     * True when [text] reads like marketing/scam content rather than a real transaction alert.
     * Used as a guardrail so promotional SMS ("you can get a personal loan offer of Rs 4,00,000")
     * isn't mistaken for an actual credit/debit just because it mentions an amount.
     */
    fun isPromotionalOrScam(text: String): Boolean {
        val lower = text.lowercase()
        return promotionalKeywords.any { lower.contains(it) }
    }

    // Banks send these when a standing instruction / e-mandate is *registered* or *activated* on
    // a card/account (e.g. for autopay on a subscription or cloud bill). The SMS always mentions a
    // "Maximum Amount" - the future authorization ceiling the merchant is allowed to charge up to -
    // not an amount that has actually been debited yet. Without this guard, that ceiling gets
    // picked up by extractAmount() and recorded as a real expense (e.g. a Rs.75,000 standing
    // instruction registration showing up as a Rs.75,000 debit that never happened).
    private val mandateRegistrationKeywords = listOf(
        "standing instruction", "e-mandate", "emandate", "mandate has been", "mandate id",
        "mandate registered", "mandate activated", "si activated", "si has been activated",
        "autopay has been set up", "autopay registered"
    )

    /**
     * True when [text] is a standing-instruction/e-mandate registration notice rather than an
     * actual transaction - see [mandateRegistrationKeywords] for why this needs its own guard
     * distinct from [isPromotionalOrScam].
     */
    fun isMandateRegistration(text: String): Boolean {
        val lower = text.lowercase()
        if (!(lower.contains("standing instruction") || lower.contains("mandate"))) return false
        val activationVerbs = listOf("activated", "registered", "set up", "created", "approved")
        return activationVerbs.any { lower.contains(it) } || mandateRegistrationKeywords.any { lower.contains(it) }
    }

    // Bank reminders for an *upcoming* payment (EMI/SI/bill due tomorrow, auto-debit scheduled)
    // mention an amount and often a debit-adjacent word ("due", "debited on") without describing
    // a transaction that has already happened. Distinct from isMandateRegistration (which is about
    // a one-time mandate *setup* notice) - this covers recurring "don't forget to keep balance
    // ready" nudges that arrive every billing cycle.
    private val paymentReminderKeywords = listOf(
        "is due tomorrow", "is due on", "will be due", "due date is", "scheduled for debit",
        "will be debited on", "auto-debit will be initiated", "auto debit will be initiated",
        "upcoming due", "payment is due", "kindly maintain sufficient balance",
        "ensure sufficient balance", "please maintain balance", "will be presented for payment",
        "reminder:", "payment reminder"
    )

    /**
     * True when [text] is reminding the user about a payment that hasn't happened yet (EMI/SI/bill
     * due soon), rather than confirming one that already did. See [paymentReminderKeywords].
     */
    fun isPaymentReminder(text: String): Boolean {
        val lower = text.lowercase()
        return paymentReminderKeywords.any { lower.contains(it) }
    }

    /**
     * Extracts the currency amount that best represents the actual transaction, or null if no
     * amount is present. Prefers the first match that isn't immediately preceded by a
     * balance/limit/due-amount phrase (see [nonTransactionAmountContext]); falls back to the very
     * first amount in the text when every candidate is disqualified (or there's only one).
     */
    fun extractAmount(text: String): Double? {
        val matches = amountRegex.findAll(text).toList()
        if (matches.isEmpty()) return null
        val lower = text.lowercase()
        val chosen = matches.firstOrNull { match ->
            val windowStart = (match.range.first - 25).coerceAtLeast(0)
            val precedingText = lower.substring(windowStart, match.range.first)
            nonTransactionAmountContext.none { precedingText.contains(it) }
        } ?: matches.first()
        val numeric = chosen.groupValues[1].replace(",", "")
        return numeric.toDoubleOrNull()
    }

    /** Infers whether the text describes money coming in or going out. */
    fun extractTransactionType(text: String): TransactionType? {
        val lower = text.lowercase()
        val isDebit = debitKeywords.any { lower.contains(it) }
        val isCredit = creditKeywords.any { lower.contains(it) }
        return when {
            isDebit && !isCredit -> TransactionType.EXPENSE
            isCredit && !isDebit -> TransactionType.INCOME
            // Ambiguous (both or neither keyword present): prefer whichever keyword occurs first.
            isDebit && isCredit -> {
                val debitIdx = debitKeywords.minOf { kw -> lower.indexOf(kw).let { if (it < 0) Int.MAX_VALUE else it } }
                val creditIdx = creditKeywords.minOf { kw -> lower.indexOf(kw).let { if (it < 0) Int.MAX_VALUE else it } }
                if (debitIdx <= creditIdx) TransactionType.EXPENSE else TransactionType.INCOME
            }
            else -> null
        }
    }

    fun extractPaymentMethod(text: String): PaymentMethod {
        val lower = text.lowercase()
        return when {
            lower.contains("upi") || lower.contains("vpa") -> PaymentMethod.UPI
            lower.contains("credit card") || Regex("""\bcc\b""").containsMatchIn(lower) -> PaymentMethod.CREDIT_CARD
            lower.contains("debit card") -> PaymentMethod.DEBIT_CARD
            lower.contains("net banking") || lower.contains("netbanking") -> PaymentMethod.NET_BANKING
            lower.contains("wallet") -> PaymentMethod.WALLET
            lower.contains("cash") -> PaymentMethod.CASH
            else -> PaymentMethod.UNKNOWN
        }
    }

    /**
     * Best-effort merchant extraction: looks for text following common prepositions used by
     * Indian bank/UPI SMS templates ("to X", "at X", "via UPI to X", "VPA x@bank").
     * Falls back to null when no recognizable merchant token is found.
     */
    fun extractMerchant(text: String): String? {
        val patterns = listOf(
            Regex("""(?:vpa)\s+([a-zA-Z0-9._-]+)@""", RegexOption.IGNORE_CASE),
            Regex("""(?:to|at|in favou?r of)\s+([A-Za-z0-9 .&'_-]{2,40}?)(?:\s+on\b|\s+ref\b|\s+via\b|[.,]|$)""", RegexOption.IGNORE_CASE),
            // Some bank UPI templates name the payee right before "credited" with no preposition,
            // e.g. "...debited for Rs 50.00 on 25-Jun-26; HUMANAMAINA NIK credited. UPI:...".
            // Without this, extractMerchant() returns null for these and the transaction falls
            // through to "Unknown"/Miscellaneous despite the payee name being right there.
            Regex("""[;:]\s*([A-Za-z0-9 .&'_-]{2,40}?)\s+credited\b""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val raw = match.groupValues[1].trim()
                if (raw.isNotBlank()) return raw
            }
        }
        return null
    }
}
