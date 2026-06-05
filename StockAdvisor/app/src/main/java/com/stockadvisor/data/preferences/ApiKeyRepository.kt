package com.stockadvisor.data.preferences

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME  = "indistock_prefs"
private const val KEY_API_KEY = "anthropic_api_key"

class ApiKeyRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun saveApiKey(key: String) = prefs.edit { putString(KEY_API_KEY, key.trim()) }

    fun hasApiKey(): Boolean = getApiKey().isNotBlank()
}
