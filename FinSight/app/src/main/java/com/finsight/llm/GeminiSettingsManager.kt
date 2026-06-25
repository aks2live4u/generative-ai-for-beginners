package com.finsight.llm

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the user's own Gemini API key and the AI-features opt-in flag. Same Keystore-backed
 * EncryptedSharedPreferences pattern as [com.finsight.security.PinManager] - the key never leaves
 * this device except in the literal sense of being used as a query parameter on calls the user
 * has explicitly opted into (see [GeminiClient]).
 */
class GeminiSettingsManager(context: Context) {

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

    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    // Off by default - the rule-based engine keeps working with zero network calls until the
    // user explicitly turns this on after reading the privacy disclosure in Settings.
    var aiFeaturesEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    fun isConfigured(): Boolean = aiFeaturesEnabled && !apiKey.isNullOrBlank()

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).putBoolean(KEY_ENABLED, false).apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "finsight_gemini_prefs"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_ENABLED = "ai_features_enabled"
    }
}
