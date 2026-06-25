package com.finsight.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.finsight.core.ai.MerchantPurposeRule
import com.finsight.core.model.Purpose
import org.json.JSONObject

/** Personal merchant-to-[Purpose] overrides taught via chat, e.g. "tag mom as family". */
class PurposeRuleManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context, PREFS_FILE_NAME, masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun allRules(): List<MerchantPurposeRule> {
        val json = prefs.getString(KEY_RULES, null) ?: return emptyList()
        val obj = JSONObject(json)
        return obj.keys().asSequence().mapNotNull { merchantKey ->
            Purpose.fromDisplayName(obj.getString(merchantKey))?.let { purpose -> MerchantPurposeRule(merchantKey, purpose) }
        }.toList()
    }

    fun addRule(merchantKey: String, purpose: Purpose) {
        val obj = JSONObject(prefs.getString(KEY_RULES, null) ?: "{}")
        obj.put(merchantKey, purpose.displayName)
        prefs.edit().putString(KEY_RULES, obj.toString()).apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "finsight_purpose_rules"
        private const val KEY_RULES = "rules_json"
    }
}
