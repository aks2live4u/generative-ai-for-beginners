package com.notnow.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(context: Context) {
    private val store = context.applicationContext.dataStore

    companion object {
        val KEY_OPERATING_MODE = stringPreferencesKey("operating_mode")
        val KEY_NIGHT_LOCKDOWN_ENABLED = booleanPreferencesKey("night_lockdown_enabled")
        val KEY_NIGHT_START_HOUR = intPreferencesKey("night_start_hour")
        val KEY_NIGHT_END_HOUR = intPreferencesKey("night_end_hour")
        val KEY_EMERGENCY_UNLOCK_UNTIL = longPreferencesKey("emergency_unlock_until")
        val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val KEY_PROTECTED_TIME_SECONDS = longPreferencesKey("protected_time_seconds")
        val KEY_SPENDING_AVOIDED = longPreferencesKey("spending_avoided_paise")
        val KEY_WORK_MODE_ENABLED = booleanPreferencesKey("work_mode_enabled")
        val KEY_WORK_DAYS = stringPreferencesKey("work_days")
        val KEY_WORK_START_HOUR = intPreferencesKey("work_start_hour")
        val KEY_WORK_END_HOUR = intPreferencesKey("work_end_hour")
        val KEY_WORK_EMERGENCY_USED_AT = longPreferencesKey("work_emergency_used_at")

        /** Default work days: Monday(2) through Friday(6), using Calendar.DAY_OF_WEEK values. */
        const val DEFAULT_WORK_DAYS = "2,3,4,5,6"
    }

    val operatingMode: Flow<String> = store.data.safeCatch().map {
        it[KEY_OPERATING_MODE] ?: "LIFE"
    }

    val nightLockdownEnabled: Flow<Boolean> = store.data.safeCatch().map {
        it[KEY_NIGHT_LOCKDOWN_ENABLED] ?: true
    }

    val nightStartHour: Flow<Int> = store.data.safeCatch().map {
        it[KEY_NIGHT_START_HOUR] ?: 23
    }

    val nightEndHour: Flow<Int> = store.data.safeCatch().map {
        it[KEY_NIGHT_END_HOUR] ?: 7
    }

    val emergencyUnlockUntil: Flow<Long> = store.data.safeCatch().map {
        it[KEY_EMERGENCY_UNLOCK_UNTIL] ?: 0L
    }

    val isFirstLaunch: Flow<Boolean> = store.data.safeCatch().map {
        it[KEY_IS_FIRST_LAUNCH] ?: true
    }

    val workModeEnabled: Flow<Boolean> = store.data.safeCatch().map {
        it[KEY_WORK_MODE_ENABLED] ?: false
    }

    val workDays: Flow<Set<Int>> = store.data.safeCatch().map {
        (it[KEY_WORK_DAYS] ?: DEFAULT_WORK_DAYS)
            .split(",")
            .mapNotNull { day -> day.trim().toIntOrNull() }
            .toSet()
    }

    val workStartHour: Flow<Int> = store.data.safeCatch().map {
        it[KEY_WORK_START_HOUR] ?: 14
    }

    val workEndHour: Flow<Int> = store.data.safeCatch().map {
        it[KEY_WORK_END_HOUR] ?: 23
    }

    val workEmergencyUsedAt: Flow<Long> = store.data.safeCatch().map {
        it[KEY_WORK_EMERGENCY_USED_AT] ?: 0L
    }

    suspend fun setOperatingMode(mode: String) = store.edit {
        it[KEY_OPERATING_MODE] = mode
    }

    suspend fun setNightLockdownEnabled(enabled: Boolean) = store.edit {
        it[KEY_NIGHT_LOCKDOWN_ENABLED] = enabled
    }

    suspend fun setNightHours(start: Int, end: Int) = store.edit {
        it[KEY_NIGHT_START_HOUR] = start
        it[KEY_NIGHT_END_HOUR] = end
    }

    suspend fun setEmergencyUnlockUntil(timestampMs: Long) = store.edit {
        it[KEY_EMERGENCY_UNLOCK_UNTIL] = timestampMs
    }

    suspend fun setFirstLaunchDone() = store.edit {
        it[KEY_IS_FIRST_LAUNCH] = false
    }

    suspend fun addProtectedTimeSeconds(seconds: Long) = store.edit { prefs ->
        prefs[KEY_PROTECTED_TIME_SECONDS] = (prefs[KEY_PROTECTED_TIME_SECONDS] ?: 0L) + seconds
    }

    suspend fun setWorkModeEnabled(enabled: Boolean) = store.edit {
        it[KEY_WORK_MODE_ENABLED] = enabled
    }

    suspend fun setWorkDays(days: Set<Int>) = store.edit {
        it[KEY_WORK_DAYS] = days.sorted().joinToString(",")
    }

    suspend fun setWorkHours(start: Int, end: Int) = store.edit {
        it[KEY_WORK_START_HOUR] = start
        it[KEY_WORK_END_HOUR] = end
    }

    suspend fun setWorkEmergencyUsedAt(timestampMs: Long) = store.edit {
        it[KEY_WORK_EMERGENCY_USED_AT] = timestampMs
    }

    private fun Flow<Preferences>.safeCatch() = catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }
}
