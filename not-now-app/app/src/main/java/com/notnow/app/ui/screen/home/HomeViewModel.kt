package com.notnow.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.data.preferences.OperatingMode
import com.notnow.app.data.repository.AppRuleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val operatingMode: OperatingMode = OperatingMode.LIFE,
    val guardrailEnabled: Boolean = true,
    val nightLockdownEnabled: Boolean = true,
    val nightLockdownActive: Boolean = false,
    val rules: List<AppRule> = emptyList(),
    val emergencyUnlockActive: Boolean = false,
    val emergencyUnlockMinutesLeft: Int = 0
)

class HomeViewModel(
    private val ruleRepository: AppRuleRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.operatingMode,
                prefs.guardrailEnabled,
                prefs.nightLockdownEnabled,
                prefs.nightLockdownActive,
                prefs.emergencyUnlockUntil,
                ruleRepository.getAllRules()
            ) { values ->
                val mode = values[0] as OperatingMode
                val guardrail = values[1] as Boolean
                val nightEnabled = values[2] as Boolean
                val nightActive = values[3] as Boolean
                val emergencyUntil = values[4] as Long
                @Suppress("UNCHECKED_CAST")
                val rules = values[5] as List<AppRule>
                val now = System.currentTimeMillis()
                val emergencyActive = now < emergencyUntil
                val minutesLeft = if (emergencyActive) ((emergencyUntil - now) / 60000).toInt() + 1 else 0
                HomeUiState(mode, guardrail, nightEnabled, nightActive, rules, emergencyActive, minutesLeft)
            }.collect { _state.value = it }
        }
    }

    fun setOperatingMode(mode: OperatingMode) = viewModelScope.launch {
        prefs.setOperatingMode(mode)
    }

    fun toggleGuardrail(enabled: Boolean) = viewModelScope.launch {
        prefs.setGuardrailEnabled(enabled)
    }

    fun toggleNightLockdown(enabled: Boolean) = viewModelScope.launch {
        prefs.setNightLockdownEnabled(enabled)
    }

    fun toggleAppRule(packageName: String, enabled: Boolean) = viewModelScope.launch {
        ruleRepository.toggleRule(packageName, enabled)
    }

    fun cancelEmergencyUnlock() = viewModelScope.launch {
        prefs.setEmergencyUnlockUntil(0L)
    }
}
