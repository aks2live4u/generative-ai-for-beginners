package com.nexus.launcher.data

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NexusSettings(
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val userName: String = "",
    val reducedMotion: Boolean = false,
    val classicGrid: Boolean = false,
    val sounds: Boolean = true,
    val voiceReplies: Boolean = true,
    val webSearch: Boolean = true,
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}

/**
 * Settings live in private app storage. The API key never leaves the device except
 * in requests to generativelanguage.googleapis.com (the Gemini API).
 */
class SettingsStore(private val context: Context) {
    private val sp = context.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<NexusSettings> = _state.asStateFlow()

    fun update(transform: (NexusSettings) -> NexusSettings) {
        val next = transform(_state.value)
        sp.edit()
            .putString("geminiApiKey", next.apiKey.trim())
            .putString("geminiModel", next.model.trim().ifBlank { NexusSettings.DEFAULT_MODEL })
            .putString("userName", next.userName.trim())
            .putBoolean("reducedMotion", next.reducedMotion)
            .putBoolean("classicGrid", next.classicGrid)
            .putBoolean("sounds", next.sounds)
            .putBoolean("voiceReplies", next.voiceReplies)
            .putBoolean("webSearch", next.webSearch)
            .apply()
        _state.value = next
    }

    private fun read(): NexusSettings {
        // Honour the system "remove animations" setting the first time NEXUS runs.
        val systemNoAnim = Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
        ) == 0f
        return NexusSettings(
            apiKey = sp.getString("geminiApiKey", "") ?: "",
            model = sp.getString("geminiModel", NexusSettings.DEFAULT_MODEL) ?: NexusSettings.DEFAULT_MODEL,
            userName = sp.getString("userName", "") ?: "",
            reducedMotion = sp.getBoolean("reducedMotion", systemNoAnim),
            classicGrid = sp.getBoolean("classicGrid", false),
            sounds = sp.getBoolean("sounds", true),
            voiceReplies = sp.getBoolean("voiceReplies", true),
            webSearch = sp.getBoolean("webSearch", true),
        )
    }
}
