package com.finsight.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.finsight.core.parser.MerchantRule
import org.json.JSONObject

/**
 * Stores the user's "Personal Merchant Dictionary" - rules taught via the Chat screen (e.g. "ATM
 * SBI Main Branch is Mother Support") so the app keeps applying them to every future transaction
 * from that merchant. Same Keystore-backed EncryptedSharedPreferences pattern as
 * [com.finsight.llm.GeminiSettingsManager]; avoids a Room schema change entirely.
 */
class MerchantRuleManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun allRules(): List<MerchantRule> {
        val json = prefs.getString(KEY_RULES, null) ?: return emptyList()
        val obj = JSONObject(json)
        return obj.keys().asSequence().map { merchantKey -> MerchantRule(merchantKey, obj.getString(merchantKey)) }.toList()
    }

    fun addRule(merchantKey: String, label: String) {
        val obj = JSONObject(prefs.getString(KEY_RULES, null) ?: "{}")
        obj.put(merchantKey, label)
        prefs.edit().putString(KEY_RULES, obj.toString()).apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "finsight_merchant_rules"
        private const val KEY_RULES = "rules_json"
    }
}
