package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.parser.MerchantRuleParser

/** A chat message recognized as an instruction to mutate stored data, rather than a question to answer. */
sealed class ChatCommand {
    /** Re-tag every transaction currently under [from] as [to] (e.g. "ATM Withdrawal is Given to Family"). */
    data class ReclassifyCategory(val from: Category, val to: Category) : ChatCommand()

    /** Re-tag every transaction from [merchantKey] as [category] (e.g. "Swiggy is Food Delivery"). */
    data class ReclassifyMerchant(val merchantKey: String, val category: Category) : ChatCommand()
}

/**
 * Recognizes "X is/as Y" chat messages where Y names a real [Category] (e.g. "ATM withdrawal is
 * given to family", "tag Swiggy as food delivery") and turns them into an actual reclassification
 * command instead of [com.finsight.core.parser.MerchantRuleBook]'s cosmetic display-label rule -
 * so the matching transactions move into the real category bucket and feed the savings rate and
 * health score correctly, not just look different in the transaction list. Reuses
 * [MerchantRuleParser]'s already-tested sentence splitting; only the resolution of the right-hand
 * side against [Category.fromDisplayName] differs.
 */
object ChatCommandParser {
    fun parse(text: String): ChatCommand? {
        val rule = MerchantRuleParser.parse(text) ?: return null
        val targetCategory = Category.fromDisplayName(rule.label) ?: return null
        val sourceCategory = Category.fromDisplayName(rule.merchantKey)
        return if (sourceCategory != null) {
            ChatCommand.ReclassifyCategory(sourceCategory, targetCategory)
        } else {
            ChatCommand.ReclassifyMerchant(rule.merchantKey, targetCategory)
        }
    }
}
