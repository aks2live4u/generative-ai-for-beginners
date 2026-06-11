package com.prettycountdown.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prettycountdown.data.model.CountdownFormat
import com.prettycountdown.data.model.WidgetStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "pretty_countdown_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** App-wide preferences: theme, defaults for new events/widgets, and gamification stats. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_WIDGET_STYLE = stringPreferencesKey("default_widget_style")
        val DEFAULT_COUNTDOWN_FORMAT = stringPreferencesKey("default_countdown_format")
        val EVENTS_CREATED = intPreferencesKey("events_created")
        val LAST_OPEN_EPOCH_DAY = longPreferencesKey("last_open_epoch_day")
        val OPEN_STREAK = intPreferencesKey("open_streak")
        val LAST_VIEWED_EVENT_ID = longPreferencesKey("last_viewed_event_id")
    }

    val themeMode = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val defaultWidgetStyle = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_WIDGET_STYLE]?.let { runCatching { WidgetStyle.valueOf(it) }.getOrNull() } ?: WidgetStyle.default
    }

    val defaultCountdownFormat = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_COUNTDOWN_FORMAT]?.let { runCatching { CountdownFormat.valueOf(it) }.getOrNull() } ?: CountdownFormat.default
    }

    val eventsCreatedCount = context.dataStore.data.map { it[Keys.EVENTS_CREATED] ?: 0 }
    val openStreak = context.dataStore.data.map { it[Keys.OPEN_STREAK] ?: 0 }
    val lastViewedEventId = context.dataStore.data.map { it[Keys.LAST_VIEWED_EVENT_ID] }

    suspend fun setLastViewedEvent(eventId: Long) {
        context.dataStore.edit { it[Keys.LAST_VIEWED_EVENT_ID] = eventId }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultWidgetStyle(style: WidgetStyle) {
        context.dataStore.edit { it[Keys.DEFAULT_WIDGET_STYLE] = style.name }
    }

    suspend fun setDefaultCountdownFormat(format: CountdownFormat) {
        context.dataStore.edit { it[Keys.DEFAULT_COUNTDOWN_FORMAT] = format.name }
    }

    suspend fun incrementEventsCreated() {
        context.dataStore.edit { it[Keys.EVENTS_CREATED] = (it[Keys.EVENTS_CREATED] ?: 0) + 1 }
    }

    /** Call once per app launch to update the daily-open streak used for the streak badge. */
    suspend fun recordAppOpen() {
        val today = LocalDate.now().toEpochDay()
        val prefs = context.dataStore.data.first()
        val lastOpen = prefs[Keys.LAST_OPEN_EPOCH_DAY]
        val streak = prefs[Keys.OPEN_STREAK] ?: 0
        val newStreak = when (lastOpen) {
            today -> streak
            today - 1 -> streak + 1
            else -> 1
        }
        context.dataStore.edit {
            it[Keys.LAST_OPEN_EPOCH_DAY] = today
            it[Keys.OPEN_STREAK] = newStreak
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
