package com.prettycountdown.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prettycountdown.data.EventRepository
import com.prettycountdown.data.SettingsRepository
import com.prettycountdown.data.ThemeMode
import com.prettycountdown.data.model.BackupData
import com.prettycountdown.data.model.CountdownFormat
import com.prettycountdown.data.model.WidgetStyle
import com.prettycountdown.util.UserStats
import com.prettycountdown.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsViewModel(
    private val context: Context,
    private val repository: EventRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val defaultWidgetStyle: StateFlow<WidgetStyle> = settings.defaultWidgetStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WidgetStyle.default)

    val defaultCountdownFormat: StateFlow<CountdownFormat> = settings.defaultCountdownFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CountdownFormat.default)

    val userStats: StateFlow<UserStats> = combine(settings.eventsCreatedCount, settings.openStreak) { created, streak ->
        UserStats(eventsCreated = created, openStreak = streak)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserStats(0, 0))

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setDefaultWidgetStyle(style: WidgetStyle) {
        viewModelScope.launch { settings.setDefaultWidgetStyle(style) }
    }

    fun setDefaultCountdownFormat(format: CountdownFormat) {
        viewModelScope.launch { settings.setDefaultCountdownFormat(format) }
    }

    /** Serializes every event, checklist item and collection to a JSON string for export. */
    suspend fun exportBackupJson(): String = json.encodeToString(repository.exportBackup())

    /** Replaces all local data with the contents of [content]. Returns true on success. */
    suspend fun restoreBackupJson(content: String): Boolean = runCatching {
        val data = json.decodeFromString<BackupData>(content)
        repository.restoreBackup(data)
        WidgetUpdater.refreshAll(context)
    }.isSuccess

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                SettingsViewModel(appContext, EventRepository.getInstance(appContext), SettingsRepository.getInstance(appContext))
            }
        }
    }
}
