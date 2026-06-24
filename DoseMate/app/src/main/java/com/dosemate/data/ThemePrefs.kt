package com.dosemate.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

enum class ThemeMode { LIGHT, DARK }

object ThemePrefs {
    private const val PREFS_NAME = "dosemate_settings"
    private const val KEY_THEME_MODE = "theme_mode"

    /** Observable so DoseMateTheme recomposes immediately when changed from Settings. */
    val current = mutableStateOf(ThemeMode.DARK)

    fun init(context: Context) {
        current.value = readFromPrefs(context)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        current.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    private fun readFromPrefs(context: Context): ThemeMode {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_THEME_MODE, null)
        return ThemeMode.entries.find { it.name == stored } ?: ThemeMode.DARK
    }
}
