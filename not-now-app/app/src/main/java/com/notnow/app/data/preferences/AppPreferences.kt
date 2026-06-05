package com.notnow.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notnow_prefs")

enum class OperatingMode { FOCUS, LIFE }

class AppPreferences(private val context: Context) {

    private object Keys {
        val OPERATING_MODE = stringPreferencesKey("operating_mode")
        val NIGHT_LOCKDOWN_ENABLED = booleanPreferencesKey("night_lockdown_enabled")
        val NIGHT_LOCKDOWN_START_HOUR = intPreferencesKey("night_lockdown_start_hour")
        val NIGHT_LOCKDOWN_END_HOUR = intPreferencesKey("night_lockdown_end_hour")
        val NIGHT_LOCKDOWN_ACTIVE = booleanPreferencesKey("night_lockdown_active")
        val EMERGENCY_UNLOCK_UNTIL = longPreferencesKey("emergency_unlock_until")
        val GUARDRAIL_ENABLED = booleanPreferencesKey("guardrail_enabled")
        val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
    }

    val operatingMode: Flow<OperatingMode> = context.dataStore.data.map { prefs ->
        OperatingMode.valueOf(prefs[Keys.OPERATING_MODE] ?: OperatingMode.LIFE.name)
    }

    val nightLockdownEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_LOCKDOWN_ENABLED] ?: true
    }

    val nightLockdownStartHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_LOCKDOWN_START_HOUR] ?: 23
    }

    val nightLockdownEndHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_LOCKDOWN_END_HOUR] ?: 7
    }

    val nightLockdownActive: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_LOCKDOWN_ACTIVE] ?: false
    }

    val emergencyUnlockUntil: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.EMERGENCY_UNLOCK_UNTIL] ?: 0L
    }

    val guardrailEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.GUARDRAIL_ENABLED] ?: true
    }

    val setupComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SETUP_COMPLETE] ?: false
    }

    suspend fun setOperatingMode(mode: OperatingMode) {
        context.dataStore.edit { it[Keys.OPERATING_MODE] = mode.name }
    }

    suspend fun setNightLockdownEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NIGHT_LOCKDOWN_ENABLED] = enabled }
    }

    suspend fun setNightLockdownSchedule(startHour: Int, endHour: Int) {
        context.dataStore.edit {
            it[Keys.NIGHT_LOCKDOWN_START_HOUR] = startHour
            it[Keys.NIGHT_LOCKDOWN_END_HOUR] = endHour
        }
    }

    suspend fun setNightLockdownActive(active: Boolean) {
        context.dataStore.edit { it[Keys.NIGHT_LOCKDOWN_ACTIVE] = active }
    }

    suspend fun setEmergencyUnlockUntil(timestamp: Long) {
        context.dataStore.edit { it[Keys.EMERGENCY_UNLOCK_UNTIL] = timestamp }
    }

    suspend fun setGuardrailEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GUARDRAIL_ENABLED] = enabled }
    }

    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.SETUP_COMPLETE] = complete }
    }

    companion object {
        @Volatile private var instance: AppPreferences? = null
        fun getInstance(context: Context): AppPreferences =
            instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
    }
}
