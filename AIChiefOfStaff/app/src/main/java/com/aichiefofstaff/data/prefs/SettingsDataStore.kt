package com.aichiefofstaff.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "ai_chief_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsDataStore(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val voiceEngineEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[VOICE_ENABLED_KEY] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    suspend fun setVoiceEngineEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[VOICE_ENABLED_KEY] = enabled }
    }

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val VOICE_ENABLED_KEY = booleanPreferencesKey("voice_enabled")
    }
}
