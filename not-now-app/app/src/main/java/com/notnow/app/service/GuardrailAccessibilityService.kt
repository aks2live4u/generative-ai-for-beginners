package com.notnow.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.notnow.app.data.database.AppDatabase
import com.notnow.app.data.entity.AppCategory
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.data.preferences.OperatingMode
import com.notnow.app.data.repository.AppRuleRepository
import com.notnow.app.ui.screen.overlay.CountdownOverlayActivity
import com.notnow.app.ui.screen.overlay.ShoppingOverlayActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class GuardrailAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var ruleRepository: AppRuleRepository
    private lateinit var prefs: AppPreferences

    private var lastBlockedPackage: String = ""
    private var lastBlockTime: Long = 0L

    companion object {
        var isRunning = false
            private set
        private const val DEBOUNCE_MS = 2000L
    }

    override fun onServiceConnected() {
        isRunning = true
        val db = AppDatabase.getInstance(applicationContext)
        ruleRepository = AppRuleRepository(db.appRuleDao())
        prefs = AppPreferences.getInstance(applicationContext)
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (packageName == "android" || packageName == "com.android.systemui") return

        scope.launch { evaluatePackage(packageName) }
    }

    private suspend fun evaluatePackage(packageName: String) {
        val guardrailEnabled = prefs.guardrailEnabled.first()
        if (!guardrailEnabled) return

        val emergencyUnlockUntil = prefs.emergencyUnlockUntil.first()
        if (System.currentTimeMillis() < emergencyUnlockUntil) return

        val rule = ruleRepository.getRuleForPackage(packageName) ?: return
        if (!rule.isEnabled) return

        if (rule.frictionLevel == FrictionLevel.LEVEL_4_BLOCKED) {
            val nightActive = prefs.nightLockdownActive.first()
            val nightEnabled = prefs.nightLockdownEnabled.first()
            if (nightEnabled && nightActive) {
                showBlockingOverlay(rule)
            }
            return
        }

        val mode = prefs.operatingMode.first()
        if (mode == OperatingMode.FOCUS && rule.blockedDuringFocusMode) {
            showOverlayForRule(rule)
            return
        }

        if (mode == OperatingMode.LIFE) {
            val nightActive = prefs.nightLockdownActive.first()
            val nightEnabled = prefs.nightLockdownEnabled.first()
            if (nightEnabled && nightActive && rule.blockedDuringNightLockdown) {
                showBlockingOverlay(rule)
                return
            }
            showOverlayForRule(rule)
        }
    }

    private fun showOverlayForRule(rule: AppRule) {
        val now = System.currentTimeMillis()
        if (rule.packageName == lastBlockedPackage && now - lastBlockTime < DEBOUNCE_MS) return
        lastBlockedPackage = rule.packageName
        lastBlockTime = now

        val intent = if (rule.category == AppCategory.SHOPPING) {
            Intent(applicationContext, ShoppingOverlayActivity::class.java)
        } else {
            Intent(applicationContext, CountdownOverlayActivity::class.java)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(CountdownOverlayActivity.EXTRA_PACKAGE_NAME, rule.packageName)
            putExtra(CountdownOverlayActivity.EXTRA_APP_NAME, rule.appName)
            putExtra(CountdownOverlayActivity.EXTRA_DELAY_SECONDS, rule.frictionLevel.delaySeconds)
            putExtra(CountdownOverlayActivity.EXTRA_FRICTION_LEVEL, rule.frictionLevel.name)
        }
        applicationContext.startActivity(intent)
    }

    private fun showBlockingOverlay(rule: AppRule) {
        val now = System.currentTimeMillis()
        if (rule.packageName == lastBlockedPackage && now - lastBlockTime < DEBOUNCE_MS) return
        lastBlockedPackage = rule.packageName
        lastBlockTime = now

        val intent = Intent(applicationContext, CountdownOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(CountdownOverlayActivity.EXTRA_PACKAGE_NAME, rule.packageName)
            putExtra(CountdownOverlayActivity.EXTRA_APP_NAME, rule.appName)
            putExtra(CountdownOverlayActivity.EXTRA_DELAY_SECONDS, 0)
            putExtra(CountdownOverlayActivity.EXTRA_FRICTION_LEVEL, FrictionLevel.LEVEL_4_BLOCKED.name)
            putExtra(CountdownOverlayActivity.EXTRA_IS_NIGHT_LOCKDOWN, true)
        }
        applicationContext.startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }
}
