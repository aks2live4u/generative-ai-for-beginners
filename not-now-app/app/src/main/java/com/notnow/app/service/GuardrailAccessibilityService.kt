package com.notnow.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.FrictionLevel
import kotlinx.coroutines.*
import java.util.Calendar

class GuardrailAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var overlayManager: OverlayManager? = null

    // Cached state — updated by background collectors so onAccessibilityEvent() is fully synchronous
    @Volatile private var ruleCache: Map<String, AppRule> = emptyMap()
    @Volatile private var currentMode = "LIFE"
    @Volatile private var nightLockdownOn = true
    @Volatile private var nightStartHour = 23
    @Volatile private var nightEndHour = 7
    @Volatile private var emergencyUnlockUntil = 0L

    private var lastBlockedPackage = ""
    private var lastBlockedTime = 0L

    private val app get() = application as NotNowApplication

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            overlayManager = OverlayManager(
                context     = this,
                scope       = scope,
                messageRepo = app.futureMessageRepository,
                prefs       = app.preferences,
                usageRepo   = app.usageRepository,
                vaultRepo   = app.shoppingVaultRepository
            )
        } catch (_: Exception) {}
        seedAndObserve()
    }

    private fun seedAndObserve() {
        // Rules
        scope.launch {
            try {
                val a = app
                // Re-seed if the DB was cleared (e.g. app data wipe)
                if (a.appRuleRepository.getRuleForPackage("com.google.android.youtube") == null) {
                    a.appRuleRepository.seedDefaults()
                }
                a.appRuleRepository.allRules.collect { list ->
                    ruleCache = list.filter { it.isEnabled }.associateBy { it.packageName }
                }
            } catch (_: Exception) {}
        }
        // Preferences — one collector per key so they update independently
        scope.launch {
            try { app.preferences.operatingMode.collect { currentMode = it } } catch (_: Exception) {}
        }
        scope.launch {
            try { app.preferences.nightLockdownEnabled.collect { nightLockdownOn = it } } catch (_: Exception) {}
        }
        scope.launch {
            try { app.preferences.nightStartHour.collect { nightStartHour = it } } catch (_: Exception) {}
        }
        scope.launch {
            try { app.preferences.nightEndHour.collect { nightEndHour = it } } catch (_: Exception) {}
        }
        scope.launch {
            try { app.preferences.emergencyUnlockUntil.collect { emergencyUnlockUntil = it } } catch (_: Exception) {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Skip our own app and core system UI
        if (pkg == packageName) return
        if (pkg == "android" || pkg == "com.android.systemui") return
        // Skip system packages (com.android.*) but not YouTube (com.google.android.youtube)
        if (pkg.startsWith("com.android.") && !pkg.contains("youtube")) return

        // Fast synchronous lookup from in-memory cache — no DB/DataStore reads
        val rule = ruleCache[pkg] ?: return

        // Emergency unlock bypass
        if (System.currentTimeMillis() < emergencyUnlockUntil) return

        val isNight = nightLockdownOn && isNightTime(nightStartHour, nightEndHour)
        val shouldBlock = when {
            isNight && rule.blockedAtNight -> true
            currentMode == "FOCUS" && rule.blockedInFocusMode -> true
            rule.frictionLevel != FrictionLevel.LEVEL_4_BLOCKED -> true
            else -> false
        }
        if (!shouldBlock) return

        // 1-second debounce — prevents duplicate events, but short enough to re-block quickly
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPackage && now - lastBlockedTime < 1000L) return
        lastBlockedPackage = pkg
        lastBlockedTime = now

        // Send to home IMMEDIATELY — fires right here on the event thread, zero coroutine delay
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Show the guardrail overlay on the Main thread
        val showNight = isNight && rule.blockedAtNight
        scope.launch(Dispatchers.Main) {
            overlayManager?.show(pkg, rule, isNight = showNight)
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
