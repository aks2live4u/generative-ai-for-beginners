package com.dosemate.data

import android.content.Context

object ReminderSoundPrefs {
    private const val PREFS_NAME = "dosemate_settings"
    private const val KEY_SOUND_URI = "reminder_sound_uri"

    fun getSoundUri(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_SOUND_URI, null)

    fun setSoundUri(context: Context, uri: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SOUND_URI, uri)
            .apply()
    }
}
