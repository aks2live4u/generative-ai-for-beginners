package com.notnow.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.AppCategory
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.ui.screen.overlay.CountdownOverlayActivity
import com.notnow.app.ui.screen.overlay.ShoppingOverlayActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

class GuardrailAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastBlockedPackage: String = ""
    private var lastBlockedTime: Long = 0L

    private val app get() = application as NotNowApplication

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == "android" || pkg == "com.android.systemui") return

        // Debounce: don't re-trigger within 2 seconds for same package
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPackage && now - lastBlockedTime < 2000L) return

        scope.launch {
            checkAndBlock(pkg)
        }
    }

    private suspend fun checkAndBlock(packageName: String) {
        val prefs = app.preferences
        val ruleRepo = app.appRuleRepository

        // Emergency unlock active → allow everything
        val emergencyUntil = prefs.emergencyUnlockUntil.first()
        if (System.currentTimeMillis() < emergencyUntil) return

        val rule = ruleRepo.getRuleForPackage(packageName) ?: return
        if (!rule.isEnabled) return

        val mode = prefs.operatingMode.first()
        val nightEnabled = prefs.nightLockdownEnabled.first()

        val isNight = nightEnabled && isNightTime(
            prefs.nightStartHour.first(),
            prefs.nightEndHour.first()
        )

        // Night lockdown — hard block, no countdown
        if (isNight && rule.blockedAtNight) {
            if (rule.frictionLevel == FrictionLevel.LEVEL_4_BLOCKED || isNight) {
                triggerNightBlock(packageName, rule)
                return
            }
        }

        // Focus mode blocks
        if (mode == "FOCUS" && rule.blockedInFocusMode) {
            triggerBlock(packageName, rule)
            return
        }

        // Life mode — apply friction levels
        if (rule.frictionLevel != FrictionLevel.LEVEL_4_BLOCKED) {
            triggerBlock(packageName, rule)
        }
    }

    private fun triggerBlock(packageName: String, rule: AppRule) {
        lastBlockedPackage = packageName
        lastBlockedTime = System.currentTimeMillis()

        val intent = when (rule.category) {
            AppCategory.SHOPPING -> Intent(this, ShoppingOverlayActivity::class.java)
            else -> Intent(this, CountdownOverlayActivity::class.java)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("package_name", packageName)
            putExtra("app_name", rule.appName)
            putExtra("delay_seconds", rule.frictionLevel.delaySeconds)
            putExtra("category", rule.category.name)
        }
        startActivity(intent)
    }

    private fun triggerNightBlock(packageName: String, rule: AppRule) {
        lastBlockedPackage = packageName
        lastBlockedTime = System.currentTimeMillis()

        val intent = Intent(this, CountdownOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("package_name", packageName)
            putExtra("app_name", rule.appName)
            putExtra("delay_seconds", -1L) // signals night block (no countdown)
            putExtra("is_night_block", true)
        }
        startActivity(intent)
    }

    private fun isNightTime(startHour: Int, endHour: Int): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour > endHour) {
            hour >= startHour || hour < endHour
        } else {
            hour >= startHour && hour < endHour
        }
    }

    override fun onInterrupt() {
        scope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(context.packageName + "/" + GuardrailAccessibilityService::class.java.name)
        }
    }
}
