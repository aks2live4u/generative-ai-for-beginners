package com.dosemate.data

import android.content.Context
import com.dosemate.R

object ReminderSoundPrefs {
    private const val PREFS_NAME = "dosemate_settings"
    private const val KEY_SOUND_URI = "reminder_sound_uri"

    fun defaultSoundUri(context: Context): String =
        "android.resource://${context.packageName}/${R.raw.medicine_notification}"

    fun getSoundUri(context: Context): String? {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SOUND_URI, null)
        return stored ?: defaultSoundUri(context)
    }

    fun setSoundUri(context: Context, uri: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SOUND_URI, uri)
            .apply()
    }
}
