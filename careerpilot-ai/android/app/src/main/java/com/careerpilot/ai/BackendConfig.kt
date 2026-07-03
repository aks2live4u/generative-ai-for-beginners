package com.careerpilot.ai

import android.content.Context

/** Small SharedPreferences wrapper for the one setting this app has. */
object BackendConfig {
    private const val PREFS = "careerpilot_prefs"
    private const val KEY_API_BASE_URL = "api_base_url"

    fun getApiBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_BASE_URL, "") ?: ""
    }

    fun setApiBaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API_BASE_URL, url.trim().trimEnd('/')).apply()
    }

    fun isConfigured(context: Context): Boolean = getApiBaseUrl(context).isNotBlank()
}
