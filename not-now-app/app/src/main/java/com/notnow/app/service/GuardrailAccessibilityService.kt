package com.notnow.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.data.preferences.AppPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

class GuardrailAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var overlayManager: OverlayManager? = null
    private var lastBlockedPackage = ""
    private var lastBlockedTime = 0L

    private val app get() = application as NotNowApplication

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(
            context     = this,
            scope       = scope,
            messageRepo = app.futureMessageRepository,
            prefs       = app.preferences,
            usageRepo   = app.usageRepository,
            vaultRepo   = app.shoppingVaultRepository
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Ignore our own app and system UI
        if (pkg == packageName) return
        if (pkg == "android" || pkg == "com.android.systemui") return
        if (pkg.startsWith("com.android.") && !pkg.contains("youtube")) return

        // Debounce: same app can't re-trigger within 3 seconds
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPackage && now - lastBlockedTime < 3000L) return

        scope.launch { evaluate(pkg) }
    }

    private suspend fun evaluate(packageName: String) {
        val prefs = app.preferences

        // Emergency unlock active → let everything through
        if (System.currentTimeMillis() < prefs.emergencyUnlockUntil.first()) return

        val rule = app.appRuleRepository.getRuleForPackage(packageName) ?: return
        if (!rule.isEnabled) return

        val mode       = prefs.operatingMode.first()
        val nightOn    = prefs.nightLockdownEnabled.first()
        val isNight    = nightOn && isNightTime(prefs.nightStartHour.first(), prefs.nightEndHour.first())

        val shouldBlock = when {
            // Night lockdown: hard block all flagged apps
            isNight && rule.blockedAtNight -> true
            // Focus mode: block all focus-flagged apps
            mode == "FOCUS" && rule.blockedInFocusMode -> true
            // Life mode: block apps with any friction level except LEVEL_4 (handled above)
            rule.frictionLevel != FrictionLevel.LEVEL_4_BLOCKED -> true
            else -> false
        }

        if (!shouldBlock) return

        lastBlockedPackage = packageName
        lastBlockedTime = System.currentTimeMillis()

        // Guaranteed block — sends user to home screen with zero permissions required.
        // This fires even if the overlay fails for any reason.
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Must run on main thread — WindowManager requires it
        withContext(Dispatchers.Main) {
            overlayManager?.show(packageName, rule, isNight = isNight && rule.blockedAtNight)
        }
    }

    private fun isNightTime(startHour: Int, endHour: Int): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour > endHour) hour >= startHour || hour < endHour
        else hour >= startHour && hour < endHour
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.dismiss()
        scope.cancel()
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.contains(context.packageName + "/" + GuardrailAccessibilityService::class.java.name)
        }
    }
}
