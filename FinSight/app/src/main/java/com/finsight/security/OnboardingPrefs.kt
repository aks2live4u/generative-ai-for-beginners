package com.finsight.security

import android.content.Context

/** Tracks whether onboarding (security setup + permissions + initial scan) has been completed. */
class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("finsight_onboarding", Context.MODE_PRIVATE)

    var isOnboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPLETE, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    companion object {
        private const val KEY_COMPLETE = "onboarding_complete"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}
