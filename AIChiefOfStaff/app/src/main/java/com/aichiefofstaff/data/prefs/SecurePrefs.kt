package com.aichiefofstaff.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the OpenAI API key using Android Keystore-backed encryption.
 * The key never leaves the device except as part of an outbound HTTPS
 * request to OpenAI's API when the user explicitly uses an AI feature.
 */
class SecurePrefs(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ai_chief_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var openAiApiKey: String?
        get() = prefs.getString(KEY_OPENAI_API_KEY, null)
        set(value) = prefs.edit().putString(KEY_OPENAI_API_KEY, value).apply()

    var pinCode: String?
        get() = prefs.getString(KEY_PIN_CODE, null)
        set(value) = prefs.edit().putString(KEY_PIN_CODE, value).apply()

    fun hasApiKey(): Boolean = !openAiApiKey.isNullOrBlank()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_PIN_CODE = "app_pin_code"
    }
}
