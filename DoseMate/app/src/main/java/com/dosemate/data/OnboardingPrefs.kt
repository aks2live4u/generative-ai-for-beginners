package com.dosemate.data

import android.content.Context

object OnboardingPrefs {
    private const val PREFS_NAME = "dosemate_onboarding"
    private const val KEY_SETUP_COMPLETE = "setup_complete"

    fun isSetupComplete(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SETUP_COMPLETE, false)

    fun setSetupComplete(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SETUP_COMPLETE, true)
            .apply()
    }
}
