package com.notnow.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.data.repository.AppRuleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val ruleRepo: AppRuleRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    val operatingMode: StateFlow<String> = prefs.operatingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "LIFE")

    val nightLockdownEnabled: StateFlow<Boolean> = prefs.nightLockdownEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val rules: StateFlow<List<AppRule>> = ruleRepo.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyUnlockUntil: StateFlow<Long> = prefs.emergencyUnlockUntil
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setMode(mode: String) = viewModelScope.launch {
        prefs.setOperatingMode(mode)
    }

    fun toggleNightLockdown(enabled: Boolean) = viewModelScope.launch {
        prefs.setNightLockdownEnabled(enabled)
    }

    fun toggleRule(packageName: String, enabled: Boolean) = viewModelScope.launch {
        ruleRepo.setEnabled(packageName, enabled)
    }

    fun clearEmergencyUnlock() = viewModelScope.launch {
        prefs.setEmergencyUnlockUntil(0L)
    }

    class Factory(private val ruleRepo: AppRuleRepository, private val prefs: AppPreferences) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(ruleRepo, prefs) as T
    }
}
