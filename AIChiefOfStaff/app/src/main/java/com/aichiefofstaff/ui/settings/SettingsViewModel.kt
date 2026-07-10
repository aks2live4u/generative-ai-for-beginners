package com.aichiefofstaff.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichiefofstaff.AppContainer
import com.aichiefofstaff.data.prefs.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = container.settingsDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    private val _hasApiKey = MutableStateFlow(container.securePrefs.hasApiKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.settingsDataStore.setThemeMode(mode) }
    }

    fun saveApiKey(key: String) {
        container.securePrefs.openAiApiKey = key.trim().ifBlank { null }
        _hasApiKey.value = container.securePrefs.hasApiKey()
    }

    fun clearApiKey() {
        container.securePrefs.openAiApiKey = null
        _hasApiKey.value = false
    }

    fun deleteAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            container.wipeAllData()
            _hasApiKey.value = false
            onDone()
        }
    }
}
