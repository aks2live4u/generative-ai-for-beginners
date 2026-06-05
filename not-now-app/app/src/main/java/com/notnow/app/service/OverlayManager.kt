package com.notnow.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.notnow.app.data.entity.AccessOutcome
import com.notnow.app.data.entity.AppCategory
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.ShoppingVaultItem
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.data.repository.FutureMessageRepository
import com.notnow.app.data.repository.ShoppingVaultRepository
import com.notnow.app.data.repository.UsageRepository
import com.notnow.app.ui.theme.NotNowTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverlayManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val messageRepo: FutureMessageRepository,
    private val prefs: AppPreferences,
    private val usageRepo: UsageRepository,
    private val vaultRepo: ShoppingVaultRepository
) {
    private val wm: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private var currentView: ComposeView? = null
    private var currentLifecycle: ServiceLifecycleOwner? = null

    fun show(packageName: String, rule: AppRule, isNight: Boolean) {
        if (currentView != null) return

        val lifecycle = ServiceLifecycleOwner()
        currentLifecycle = lifecycle

        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            ViewTreeLifecycleOwner.set(this, lifecycle)
            ViewTreeSavedStateRegistryOwner.set(this, lifecycle)
            setContent {
                NotNowTheme {
                    when {
                        isNight -> NightBlockContent(
                            appName = rule.appName,
                            onBack  = { recordAndDismiss(packageName, rule.appName, AccessOutcome.NIGHT_BLOCKED, 0) }
                        )
                        rule.category == AppCategory.SHOPPING -> ShoppingPauseContent(
                            appName        = rule.appName,
                            delayMinutes   = rule.frictionLevel.delaySeconds / 60,
                            onBuyNow       = {
                                // Dismiss shopping overlay then show countdown
                                dismiss()
                                scope.launch(Dispatchers.Main) {
                                    val countdownRule = rule.copy(category = com.notnow.app.data.entity.AppCategory.OTHER)
                                    show(packageName, countdownRule, false)
                                }
                            },
                            onSaveForLater = { title, url, price ->
                                scope.launch {
                                    vaultRepo.save(ShoppingVaultItem(title = title, url = url, price = price))
                                }
                                recordAndDismiss(packageName, rule.appName, AccessOutcome.WENT_BACK, 0)
                            },
                            onGoBack = { recordAndDismiss(packageName, rule.appName, AccessOutcome.WENT_BACK, 0) }
                        )
                        else -> CountdownContent(
                            appName    = rule.appName,
                            totalSec   = rule.frictionLevel.delaySeconds,
                            messageRepo = messageRepo,
                            onComplete = { recordAndDismiss(packageName, rule.appName, AccessOutcome.WAITED, rule.frictionLevel.delaySeconds) },
                            onGoBack   = { recordAndDismiss(packageName, rule.appName, AccessOutcome.WENT_BACK, 0) },
                            onEmergency = {
                                scope.launch {
                                    prefs.setEmergencyUnlockUntil(System.currentTimeMillis() + 15 * 60 * 1000L)
                                }
                                recordAndDismiss(packageName, rule.appName, AccessOutcome.EMERGENCY_UNLOCKED, 0)
                            }
                        )
                    }
                }
            }
        }

        // TYPE_ACCESSIBILITY_OVERLAY bypasses SYSTEM_ALERT_WINDOW; works for any active AccessibilityService
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        )

        try {
            wm.addView(view, params)
            currentView = view
        } catch (_: Exception) {
            // Fall back to TYPE_APPLICATION_OVERLAY (requires SYSTEM_ALERT_WINDOW permission)
            try {
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                wm.addView(view, params)
                currentView = view
            } catch (e: Exception) {
                lifecycle.destroy()
                currentLifecycle = null
            }
        }
    }

    private fun recordAndDismiss(pkg: String, appName: String, outcome: AccessOutcome, delaySec: Long) {
        scope.launch { usageRepo.record(pkg, appName, outcome, delaySec) }
        dismiss()
    }

    fun dismiss() {
        currentLifecycle?.destroy()
        currentLifecycle = null
        currentView?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        currentView = null
    }
}
