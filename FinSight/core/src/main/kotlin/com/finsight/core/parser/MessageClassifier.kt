package com.finsight.core.parser

/** What kind of message this is, before any amount/merchant extraction is attempted. */
enum class MessageType { TRANSACTION, REMINDER, SPAM, OTP, UNRECOGNIZED }

/**
 * Result of [MessageClassifier.classify]: a message type plus a 0-100 confidence score and a
 * human-readable reason, surfaced as the [com.finsight.core.parser.ParseResult.NotFinancial]
 * reason (or used by the caller to decide how much to trust a [MessageType.TRANSACTION] match).
 */
data class MessageClassification(val type: MessageType, val confidence: Int, val reason: String)

/**
 * Single entry point the SMS/email/notification parsers funnel through before attempting amount/
 * merchant extraction. Consolidates the OTP/promo/mandate/reminder guards that used to be
 * duplicated (and drifting) across each parser into one ordered classification with a confidence
 * score, so "is this message actually describing a completed transaction?" is answered once.
 */
object MessageClassifier {

    private val otpKeywords = listOf("otp", "one time password", "do not share")

    fun classify(text: String): MessageClassification {
        val lower = text.lowercase()
        return when {
            otpKeywords.any { lower.contains(it) } ->
                MessageClassification(MessageType.OTP, 99, "OTP message")

            MoneyTextExtractor.isPromotionalOrScam(text) ->
                MessageClassification(MessageType.SPAM, 95, "Promotional/scam message")

            MoneyTextExtractor.isMandateRegistration(text) ->
                MessageClassification(
                    MessageType.REMINDER,
                    90,
                    "Standing instruction/mandate registration, not an actual transaction"
                )

            MoneyTextExtractor.isPaymentReminder(text) ->
                MessageClassification(
                    MessageType.REMINDER,
                    85,
                    "Upcoming payment reminder, not a completed transaction"
                )

            MoneyTextExtractor.isCardPaymentConfirmation(text) ->
                MessageClassification(
                    MessageType.REMINDER,
                    90,
                    "Card issuer confirming receipt of a bill payment already recorded as a debit elsewhere, not new income"
                )

            else -> classifyAsTransaction(text)
        }
    }

    private fun classifyAsTransaction(text: String): MessageClassification {
        val amount = MoneyTextExtractor.extractAmount(text)
        val type = MoneyTextExtractor.extractTransactionType(text)
        if (amount == null) return MessageClassification(MessageType.UNRECOGNIZED, 5, "No amount found")
        if (type == null) return MessageClassification(MessageType.UNRECOGNIZED, 10, "No debit/credit keyword found")

        val merchant = MoneyTextExtractor.extractMerchant(text)
        // Base confidence for "has a clear amount + unambiguous direction"; extracting a merchant
        // on top of that is the strongest remaining signal that this is a genuine, well-formed
        // transaction alert rather than a borderline match.
        var confidence = 70
        if (merchant != null) confidence += 25
        confidence = confidence.coerceAtMost(99)

        val reason = if (merchant != null) "Amount and direction found, merchant=$merchant" else "Amount and direction found, no merchant"
        return MessageClassification(MessageType.TRANSACTION, confidence, reason)
    }
}
