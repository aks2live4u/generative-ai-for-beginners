package com.finsight.core.parser

/** A user-taught rule: any merchant matching [merchantKey] (via [MerchantMatcher]) should display as [label]. */
data class MerchantRule(val merchantKey: String, val label: String)

/**
 * The "Personal Merchant Dictionary": once a user teaches a rule (e.g. "ATM SBI Main Branch is
 * Mother Support" via chat), every transaction from that merchant - past and future - should show
 * the user's own label instead of the raw bank/UPI string. Resolution reuses [MerchantMatcher]'s
 * fuzzy comparison so close spelling variants of the same merchant still pick up the rule.
 */
object MerchantRuleBook {

    /** Returns the taught label for [merchant], or null if no rule matches. */
    fun resolveLabel(rules: List<MerchantRule>, merchant: String): String? =
        rules.firstOrNull { MerchantMatcher.isSameMerchant(it.merchantKey, merchant) }?.label
}
