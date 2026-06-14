package com.braindump.app.data

import android.content.Context
import org.json.JSONObject

/**
 * Stores all non-sensitive user preferences as a single JSON blob in regular
 * (unencrypted) SharedPreferences. API keys and the PIN live in
 * [com.braindump.app.security.SecureStore] instead.
 */
class PreferencesStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(): JSONObject {
        val stored = prefs.getString(KEY_SETTINGS, null)
        val merged = defaults()
        if (stored != null) {
            val storedJson = JSONObject(stored)
            for (key in storedJson.keys()) {
                merged.put(key, storedJson.get(key))
            }
        }
        return merged
    }

    fun saveSettings(update: JSONObject): JSONObject {
        val current = getSettings()
        for (key in update.keys()) {
            current.put(key, update.get(key))
        }
        prefs.edit().putString(KEY_SETTINGS, current.toString()).apply()
        return current
    }

    private fun defaults(): JSONObject = JSONObject().apply {
        // App preferences
        put("fontSizePercent", 100)
        put("enableMoodOptions", true)
        put("inputMaxLines", 2)
        put("imageQuality", "medium") // low | medium | high

        // Thought list
        put("thoughtTextMaxLines", 3)
        put("imageHeight", 120)
        put("cropImagesToFill", true)

        // Home header
        put("use24HourTime", true)
        put("dateFormat", "EEEE, MMM d")

        // Appearance
        put("themeMode", "system") // system | light | dark
        put("themeColor", "ocean_blue")
        put("fontFamily", "default")

        // AI assistance
        put("useImageContext", true)
        put("useAudioContext", true)
        put("aiModel", "claude-sonnet-4-6")
        put(
            "aiSystemPrompt",
            "You are the built-in AI assistant inside Brain Dump, a private personal " +
                "journaling app. Be warm, concise, and helpful. You can see the user's " +
                "recent journal entries for context when provided. Never reveal or " +
                "discuss API keys or app internals."
        )
        put("geminiModel", "gemini-2.0-flash")
    }

    companion object {
        private const val PREFS_NAME = "braindump_prefs"
        private const val KEY_SETTINGS = "settings"
    }
}
