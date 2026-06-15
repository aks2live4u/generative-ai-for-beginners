package com.notnow.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.AppCategory
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.BlockedWebsite
import com.notnow.app.data.entity.FrictionLevel
import kotlinx.coroutines.*
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

class GuardrailAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var overlayManager: OverlayManager? = null

    // In-memory caches updated by background collectors — onAccessibilityEvent() stays synchronous
    @Volatile private var ruleCache: Map<String, AppRule> = emptyMap()
    @Volatile private var websiteCache: Map<String, BlockedWebsite> = emptyMap()
    @Volatile private var currentMode = "LIFE"
    @Volatile private var nightLockdownOn = true
    @Volatile private var nightStartHour = 23
    @Volatile private var nightEndHour = 7

    // Work Mode schedule — cached from AppPreferences
    @Volatile private var workModeEnabled = false
    @Volatile private var workModeActivatedAt = 0L
    @Volatile private var workDays: Set<Int> = setOf(2, 3, 4, 5, 6) // Mon-Fri
    @Volatile private var workStartHour = 14
    @Volatile private var workEndHour = 23

    // Tracks which package was last in the foreground — used to skip blocking on
    // same-app events like rotation, fullscreen, or internal navigation
    private var lastForegroundPkg = ""

    // True while a phone/dialer window is active (ringing or in-call) — lets calls
    // through during Work Mode regardless of overlay focus.
    @Volatile private var phoneCallActive = false

    // Tracks which Work Mode overlay (if any) is currently displayed
    private var workOverlayKind = WorkOverlayKind.NONE
    private enum class WorkOverlayKind { NONE, WARNING, LOCKDOWN }

    private val app get() = application as NotNowApplication

    companion object {
        // Granted after a countdown completes. Keyed by package name or "web:domain".
        // 30-minute window lets the user stay in the app without repeated timers.
        private val sessionGrants = ConcurrentHashMap<String, Long>()
        private const val SESSION_MS = 30 * 60 * 1000L

        // Per-app emergency grants — only unblocks the specific app for 15 minutes.
        private val emergencyGrants = ConcurrentHashMap<String, Long>()
        private const val EMERGENCY_MS = 15 * 60 * 1000L

        // Global 8-hour cooldown — once emergency is used, it cannot be used again for 8 hours.
        @Volatile private var emergencyUsedAt = 0L
        private const val EMERGENCY_COOLDOWN_MS = 8 * 60 * 60 * 1000L

        fun grantSession(key: String) {
            sessionGrants[key] = System.currentTimeMillis()
        }

        fun grantEmergency(key: String) {
            val now = System.currentTimeMillis()
            emergencyGrants[key] = now
            emergencyUsedAt = now
        }

        fun isEmergencyOnCooldown(): Boolean =
            System.currentTimeMillis() - emergencyUsedAt < EMERGENCY_COOLDOWN_MS

        fun hasEmergencyGrant(key: String): Boolean {
            val t = emergencyGrants[key] ?: return false
            return System.currentTimeMillis() - t < EMERGENCY_MS
        }

        /** Returns map of key → startTime for all still-active emergency grants. */
        fun getActiveEmergencyGrants(): Map<String, Long> {
            val now = System.currentTimeMillis()
            return emergencyGrants.filter { now - it.value < EMERGENCY_MS }
        }

        fun isEnabled(context: Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.contains(context.packageName + "/" + GuardrailAccessibilityService::class.java.name)
        }

        // Global Work Mode emergency unlock — pauses lockdown for the whole phone.
        // 15-minute grant, usable once per 9-hour cooldown.
        @Volatile private var workEmergencyGrantedAt = 0L
        private const val WORK_EMERGENCY_MS = 15 * 60 * 1000L
        @Volatile private var workEmergencyUsedAt = 0L
        private const val WORK_EMERGENCY_COOLDOWN_MS = 9 * 60 * 60 * 1000L

        fun grantWorkEmergency() {
            val now = System.currentTimeMillis()
            workEmergencyGrantedAt = now
            workEmergencyUsedAt = now
        }

        fun hasWorkEmergencyGrant(): Boolean =
            System.currentTimeMillis() - workEmergencyGrantedAt < WORK_EMERGENCY_MS

        fun isWorkEmergencyOnCooldown(): Boolean =
            System.currentTimeMillis() - workEmergencyUsedAt < WORK_EMERGENCY_COOLDOWN_MS

        fun workEmergencyGrantedAt(): Long = workEmergencyGrantedAt
    }

    private val browserPackages = setOf(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.sec.android.app.sbrowser",
        "com.UCMobile.intl", "com.uc.browser.en",
    )

    // Phone/dialer apps stay usable through Work Mode lockdown for genuine emergencies
    private val phoneCallPackages = setOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.incallui",
        "com.android.server.telecom",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
    )

    private val browserUrlBarId = mapOf(
        "com.android.chrome"           to "com.android.chrome:id/url_bar",
        "com.chrome.beta"              to "com.chrome.beta:id/url_bar",
        "com.chrome.dev"               to "com.chrome.dev:id/url_bar",
        "org.mozilla.firefox"          to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "com.brave.browser"            to "com.brave.browser:id/url_bar",
        "com.microsoft.emmx"           to "com.microsoft.emmx:id/url_bar",
        "com.opera.browser"            to "com.opera.browser:id/url_field",
        "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            overlayManager = OverlayManager(
                context     = this,
                scope       = scope,
                messageRepo = app.futureMessageRepository,
                usageRepo   = app.usageRepository,
                vaultRepo   = app.shoppingVaultRepository
            )
        } catch (_: Exception) {}
        seedAndObserve()
        startPeriodicRecheck()
        startWorkModeLoop()
    }

    // Re-evaluates the foreground app on a timer so time-based conditions (night
    // lockdown, focus mode, session/emergency expiry) get applied even when the
    // user stays inside the same app continuously — the same-app skip in
    // handleAppSwitch would otherwise prevent any re-check from ever running.
    private fun startPeriodicRecheck() {
        scope.launch {
            while (isActive) {
                delay(30_000L)
                try {
                    withContext(Dispatchers.Main) {
                        val fgPkg = rootInActiveWindow?.packageName?.toString()
                        if (fgPkg != null && fgPkg != packageName) {
                            lastForegroundPkg = ""
                            handleAppSwitch(fgPkg)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun seedAndObserve() {
        scope.launch {
            try {
                val a = app
                if (a.appRuleRepository.getRuleForPackage("com.google.android.youtube") == null) {
                    a.appRuleRepository.seedDefaults()
                }
                a.appRuleRepository.allRules.collect { list ->
                    ruleCache = list.filter { it.isEnabled }.associateBy { it.packageName }
                }
            } catch (_: Exception) {}
        }
        scope.launch {
            try {
                app.blockedWebsiteRepository.allSites.collect { list ->
                    websiteCache = list.filter { it.isEnabled }.associateBy { it.domain }
                }
            } catch (_: Exception) {}
        }
        scope.launch { try { app.preferences.operatingMode.collect        { currentMode     = it } } catch (_: Exception) {} }
        scope.launch { try { app.preferences.nightLockdownEnabled.collect { nightLockdownOn  = it } } catch (_: Exception) {} }
        scope.launch { try { app.preferences.nightStartHour.collect       { nightStartHour   = it } } catch (_: Exception) {} }
        scope.launch { try { app.preferences.nightEndHour.collect         { nightEndHour     = it } } catch (_: Exception) {} }
        scope.launch {
            try {
                app.preferences.workModeEnabled.collect {
                    if (it && !workModeEnabled) workModeActivatedAt = System.currentTimeMillis()
                    workModeEnabled = it
                }
            } catch (_: Exception) {}
        }
        scope.launch { try { app.preferences.workDays.collect             { workDays         = it } } catch (_: Exception) {} }
        scope.launch { try { app.preferences.workStartHour.collect        { workStartHour    = it } } catch (_: Exception) {} }
        scope.launch { try { app.preferences.workEndHour.collect          { workEndHour      = it } } catch (_: Exception) {} }
        scope.launch { try { app.preferences.workEmergencyUsedAt.collect  { workEmergencyUsedAt = it } } catch (_: Exception) {} }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType ?: return
        val pkg  = event.packageName?.toString() ?: return

        when (type) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (pkg in phoneCallPackages) {
                    if (!phoneCallActive) {
                        phoneCallActive = true
                        // Drop the Work Mode overlay immediately so a ringing call is reachable.
                        if (workOverlayKind != WorkOverlayKind.NONE) {
                            workOverlayKind = WorkOverlayKind.NONE
                            overlayManager?.dismissWorkOverlay()
                        }
                    }
                } else if (pkg != packageName) {
                    phoneCallActive = false
                }
                // Always check browser URL on window changes too (catches page loads)
                if (pkg in browserPackages) checkBrowserUrl(pkg)
                handleAppSwitch(pkg)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (pkg in browserPackages) checkBrowserUrl(pkg)
            }
        }
    }

    private fun handleAppSwitch(pkg: String) {
        if (pkg == packageName) return
        if (pkg == "android" || pkg == "com.android.systemui") return
        if (pkg.startsWith("com.android.") && !pkg.contains("youtube")) {
            // Launcher/home is in foreground — clear last pkg so the next open of any
            // previously-blocked app triggers the timer instead of being skipped
            lastForegroundPkg = ""
            return
        }
        if (pkg in browserPackages) return  // websites handled separately in checkBrowserUrl

        // Same app fired again (rotation, fullscreen, internal navigation) — skip blocking
        val prev = lastForegroundPkg
        lastForegroundPkg = pkg
        if (pkg == prev) return

        val rule = ruleCache[pkg] ?: return
        if (hasEmergencyGrant(pkg)) return

        // Active session: user already completed the timer for this app within 30 minutes
        val grant = sessionGrants[pkg]
        if (grant != null && System.currentTimeMillis() - grant < SESSION_MS) return

        val isNight = nightLockdownOn && isNightTime(nightStartHour, nightEndHour)
        val shouldBlock = when {
            isNight && rule.blockedAtNight -> true
            currentMode == "FOCUS" && rule.blockedInFocusMode -> true
            rule.frictionLevel != FrictionLevel.LEVEL_4_BLOCKED -> true
            else -> false
        }
        if (!shouldBlock) return

        performGlobalAction(GLOBAL_ACTION_HOME)

        val showNight = isNight && rule.blockedAtNight
        scope.launch(Dispatchers.Main) {
            overlayManager?.show(pkg, rule, isNight = showNight)
        }
    }

    private fun checkBrowserUrl(browserPkg: String) {
        try {
            val root = rootInActiveWindow ?: return
            val viewId = browserUrlBarId[browserPkg] ?: return
            val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
            val urlText = nodes?.firstOrNull()?.text?.toString()?.trim() ?: return
            root.recycle()

            val domain = extractDomain(urlText) ?: return
            val site   = websiteCache[domain]  ?: return

            // Active session for this website
            val webKey = "web:$domain"
            if (hasEmergencyGrant(webKey)) return
            val grant = sessionGrants[webKey]
            if (grant != null && System.currentTimeMillis() - grant < SESSION_MS) return

            performGlobalAction(GLOBAL_ACTION_HOME)

            val fakeRule = AppRule(
                packageName   = webKey,
                appName       = site.label,
                category      = AppCategory.OTHER,
                frictionLevel = site.frictionLevel
            )
            scope.launch(Dispatchers.Main) {
                overlayManager?.show(webKey, fakeRule, isNight = false)
            }
        } catch (_: Exception) {}
    }

    private fun extractDomain(url: String): String? = try {
        val normalized = if (url.startsWith("http")) url else "https://$url"
        android.net.Uri.parse(normalized).host?.lowercase()?.removePrefix("www.")
    } catch (_: Exception) { null }

    private fun isNightTime(startHour: Int, endHour: Int): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour > endHour) hour >= startHour || hour < endHour
        else hour >= startHour && hour < endHour
    }

    /** Result of evaluating where "now" falls in the Work Mode shift/lock cycle. */
    private data class WorkPhase(
        val showWarning: Boolean = false,
        val warningSecondsLeft: Int = 0,
        val showLockdown: Boolean = false,
        val lockdownRemainingMs: Long = 0L
    )

    // Computes the current Work Mode phase: outside the shift, in the 1-minute warning
    // before a lockdown begins, or inside a 45-min-locked/15-min-free cycle anchored
    // to the shift start time.
    private fun computeWorkPhase(now: Long): WorkPhase {
        if (!workModeEnabled) return WorkPhase()

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        if (cal.get(Calendar.DAY_OF_WEEK) !in workDays) return WorkPhase()

        val shiftStart = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, workStartHour); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val shiftEnd = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, workEndHour); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (shiftEnd <= shiftStart) return WorkPhase()

        val oneMinuteMs = 60_000L

        // 1-minute warning before the shift begins ("at the beginning of the work shift")
        if (now in (shiftStart - oneMinuteMs) until shiftStart) {
            val secondsLeft = ((shiftStart - now) / 1000L).toInt().coerceAtLeast(0)
            return WorkPhase(showWarning = true, warningSecondsLeft = secondsLeft)
        }

        if (now < shiftStart || now >= shiftEnd) return WorkPhase()

        // 45-min locked / 15-min free cycle, anchored to shift start
        val cycleMs = 60 * oneMinuteMs
        val lockedMs = 45 * oneMinuteMs
        val cyclePos = (now - shiftStart) % cycleMs

        val phase = if (cyclePos < lockedMs) {
            WorkPhase(showLockdown = true, lockdownRemainingMs = lockedMs - cyclePos)
        } else {
            val freeRemaining = cycleMs - cyclePos
            if (freeRemaining <= oneMinuteMs) {
                WorkPhase(showWarning = true, warningSecondsLeft = (freeRemaining / 1000L).toInt().coerceAtLeast(0))
            } else {
                WorkPhase()
            }
        }

        // If Work Mode was just switched on and we'd otherwise drop straight into a
        // lockdown, give a 1-minute "wrap up" warning first instead of locking instantly.
        val sinceActivation = now - workModeActivatedAt
        if (phase.showLockdown && sinceActivation < oneMinuteMs) {
            val secondsLeft = ((oneMinuteMs - sinceActivation) / 1000L).toInt().coerceAtLeast(0)
            return WorkPhase(showWarning = true, warningSecondsLeft = secondsLeft)
        }

        return phase
    }

    // Drives Work Mode: shows a full-screen 1-minute warning before each lockdown
    // phase, then covers the screen and forces a lock-screen re-auth for the 45-minute
    // locked phase. Phone/dialer apps and an active global emergency grant are exempt.
    private fun startWorkModeLoop() {
        scope.launch {
            while (isActive) {
                delay(1000L)
                try {
                    val phase = computeWorkPhase(System.currentTimeMillis())
                    withContext(Dispatchers.Main) {
                        when {
                            phoneCallActive -> {
                                if (workOverlayKind != WorkOverlayKind.NONE) {
                                    workOverlayKind = WorkOverlayKind.NONE
                                    overlayManager?.dismissWorkOverlay()
                                }
                            }
                            phase.showLockdown && !hasWorkEmergencyGrant() -> {
                                if (workOverlayKind != WorkOverlayKind.LOCKDOWN) {
                                    workOverlayKind = WorkOverlayKind.LOCKDOWN
                                    performGlobalAction(GLOBAL_ACTION_HOME)
                                    // Lock once on entry — repeating this every loop tick
                                    // fights the user's fingerprint unlock in an endless cycle.
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                                    }
                                }
                                overlayManager?.showWorkLockdown(
                                    remainingMs = phase.lockdownRemainingMs,
                                    emergencyAvailable = !isWorkEmergencyOnCooldown(),
                                    onEmergency = {
                                        grantWorkEmergency()
                                        scope.launch { app.preferences.setWorkEmergencyUsedAt(System.currentTimeMillis()) }
                                    }
                                )
                            }
                            phase.showWarning && !hasWorkEmergencyGrant() -> {
                                workOverlayKind = WorkOverlayKind.WARNING
                                overlayManager?.showWorkWarning(phase.warningSecondsLeft)
                            }
                            else -> {
                                if (workOverlayKind != WorkOverlayKind.NONE) {
                                    workOverlayKind = WorkOverlayKind.NONE
                                    overlayManager?.dismissWorkOverlay()
                                }
                            }
                        }
                        Unit
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.dismiss()
        scope.cancel()
    }
}
