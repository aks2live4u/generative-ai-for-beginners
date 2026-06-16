package com.notnow.app.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.notnow.app.data.entity.AccessOutcome
import com.notnow.app.data.entity.AppCategory
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.ShoppingVaultItem
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
    private val usageRepo: UsageRepository,
    private val vaultRepo: ShoppingVaultRepository
) {
    private val wm: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private var currentView: ComposeView? = null
    private var currentLifecycle: ServiceLifecycleOwner? = null
    private var currentWorkOverlay: WorkOverlayType? = null
    private var currentParams: WindowManager.LayoutParams? = null
    private var bannerYOffset = 0

    enum class WorkOverlayType { WARNING, LOCKDOWN, PHONE_GRACE }

    fun adjustBannerY(deltaY: Float) {
        val params = currentParams ?: return
        val view = currentView ?: return
        bannerYOffset += deltaY.toInt()
        params.y = bannerYOffset
        try { wm.updateViewLayout(view, params) } catch (_: Exception) {}
    }

    fun show(packageName: String, rule: AppRule, isNight: Boolean) {
        if (currentView != null) return

        val emergencyAvailable = !GuardrailAccessibilityService.isEmergencyOnCooldown()

        addOverlayView {
            when {
                isNight -> NightBlockContent(
                    appName = rule.appName,
                    onBack  = { recordAndDismiss(packageName, rule.appName, AccessOutcome.NIGHT_BLOCKED, 0) }
                )
                rule.category == AppCategory.SHOPPING -> ShoppingPauseContent(
                    appName      = rule.appName,
                    delayMinutes = rule.frictionLevel.delaySeconds / 60,
                    onBuyNow     = {
                        dismiss()
                        scope.launch(Dispatchers.Main) {
                            val countdownRule = rule.copy(category = AppCategory.OTHER)
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
                    appName            = rule.appName,
                    totalSec           = rule.frictionLevel.delaySeconds,
                    messageRepo        = messageRepo,
                    emergencyAvailable = emergencyAvailable,
                    onOpen      = {
                        // Grant 30-minute session so the app stays unblocked
                        GuardrailAccessibilityService.grantSession(packageName)
                        recordAndDismiss(packageName, rule.appName, AccessOutcome.WAITED, rule.frictionLevel.delaySeconds)
                        launchApp(packageName)
                    },
                    onGoBack    = { recordAndDismiss(packageName, rule.appName, AccessOutcome.WENT_BACK, 0) },
                    onEmergency = {
                        // Grant 15-minute access for this app only; starts 8-hour cooldown
                        GuardrailAccessibilityService.grantEmergency(packageName)
                        recordAndDismiss(packageName, rule.appName, AccessOutcome.EMERGENCY_UNLOCKED, 0)
                        launchApp(packageName)
                    }
                )
            }
        }
    }

    /**
     * Shows a small non-blocking banner warning that lockdown is starting soon.
     * The underlying app stays usable (so the user can save/finish their work)
     * and touches/calls pass through to whatever is behind it. No-op if already showing.
     */
    fun showWorkWarning(secondsLeft: Int) {
        if (currentWorkOverlay == WorkOverlayType.WARNING) return
        dismiss()
        currentWorkOverlay = WorkOverlayType.WARNING
        addOverlayView(fullScreen = false) {
            WorkWarningContent(
                initialSecondsLeft = secondsLeft,
                onVerticalDrag = { delta -> adjustBannerY(delta) }
            )
        }
    }

    /** Shows the full-screen Work Lockdown overlay. No-op if already showing. */
    fun showWorkLockdown(
        remainingMs: Long,
        emergencyAvailable: Boolean,
        emergencyCooldownRemainingMs: Long,
        onEmergency: () -> Unit
    ) {
        if (currentWorkOverlay == WorkOverlayType.LOCKDOWN) return
        dismiss()
        currentWorkOverlay = WorkOverlayType.LOCKDOWN
        addOverlayView {
            WorkLockdownContent(
                initialRemainingMs = remainingMs,
                emergencyAvailable = emergencyAvailable,
                emergencyCooldownRemainingMs = emergencyCooldownRemainingMs,
                onEmergency = onEmergency,
                onOpenPhone = { openPhoneApp() }
            )
        }
    }

    /**
     * Dismisses the current overlay, starts the phone-call grace period (so Work Mode
     * won't immediately re-lock), and opens the device's Phone app so the user can
     * see recents/contacts and call back.
     */
    private fun openPhoneApp() {
        GuardrailAccessibilityService.markPhoneCallActive()
        GuardrailAccessibilityService.markDialerOpened()
        dismiss()
        val intent = try {
            val dialerPkg = (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage
            dialerPkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
        } catch (_: Exception) { null } ?: Intent(Intent.ACTION_DIAL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    /** Shows a non-blocking banner counting down to when Work Lockdown resumes. No-op if already showing. */
    fun showPhoneGraceBanner(secondsLeft: Int) {
        if (currentWorkOverlay == WorkOverlayType.PHONE_GRACE) return
        dismiss()
        currentWorkOverlay = WorkOverlayType.PHONE_GRACE
        addOverlayView(fullScreen = false) {
            PhoneGraceBannerContent(
                initialSecondsLeft = secondsLeft,
                onVerticalDrag = { delta -> adjustBannerY(delta) }
            )
        }
    }

    /** Dismisses a Work Mode overlay (warning or lockdown) if one is showing. */
    fun dismissWorkOverlay() {
        if (currentWorkOverlay != null) {
            currentWorkOverlay = null
            dismiss()
        }
    }

    private fun addOverlayView(fullScreen: Boolean = true, content: @Composable () -> Unit) {
        if (currentView != null) return

        val lifecycle = ServiceLifecycleOwner()
        currentLifecycle = lifecycle

        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)
            setContent {
                NotNowTheme {
                    content()
                }
            }
        }

        val params = if (fullScreen) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE
            )
        } else {
            // Non-blocking banner: lets touches/calls pass through to the app underneath.
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP; y = bannerYOffset }
        }

        if (!fullScreen) currentParams = params

        try {
            wm.addView(view, params)
            currentView = view
        } catch (_: Exception) {
            try {
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                wm.addView(view, params)
                currentView = view
            } catch (_: Exception) {
                lifecycle.destroy()
                currentLifecycle = null
            }
        }
    }

    private fun launchApp(packageName: String) {
        if (packageName.startsWith("web:")) {
            // Website — just open Chrome; user is already on the correct domain
            val chromeIntent = context.packageManager
                .getLaunchIntentForPackage("com.android.chrome")
            if (chromeIntent != null) {
                chromeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                try { context.startActivity(chromeIntent) } catch (_: Exception) {}
            }
            return
        }
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
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
        currentWorkOverlay = null
        currentParams = null
        bannerYOffset = 0
    }
}
