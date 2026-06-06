package com.notnow.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
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

    // Tracks which package was last in the foreground — used to skip blocking on
    // same-app events like rotation, fullscreen, or internal navigation
    private var lastForegroundPkg = ""

    // Per-app/site debounce — prevents duplicate events within 1 second
    private var lastBlockedKey = ""
    private var lastBlockedTime = 0L

    private val app get() = application as NotNowApplication

    companion object {
        // Granted after a countdown completes. Keyed by package name or "web:domain".
        // 30-minute window lets the user stay in the app without repeated timers.
        private val sessionGrants = ConcurrentHashMap<String, Long>()
        private const val SESSION_MS = 30 * 60 * 1000L

        // Per-app emergency grants — only unblocks the specific app for 15 minutes.
        private val emergencyGrants = ConcurrentHashMap<String, Long>()
        private const val EMERGENCY_MS = 15 * 60 * 1000L

        fun grantSession(key: String) {
            sessionGrants[key] = System.currentTimeMillis()
        }

        fun grantEmergency(key: String) {
            emergencyGrants[key] = System.currentTimeMillis()
        }

        fun hasEmergencyGrant(key: String): Boolean {
            val t = emergencyGrants[key] ?: return false
            return System.currentTimeMillis() - t < EMERGENCY_MS
        }

        fun isEnabled(context: Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.contains(context.packageName + "/" + GuardrailAccessibilityService::class.java.name)
        }
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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType ?: return
        val pkg  = event.packageName?.toString() ?: return

        when (type) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
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
        if (pkg.startsWith("com.android.") && !pkg.contains("youtube")) return
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

        val now = System.currentTimeMillis()
        if (pkg == lastBlockedKey && now - lastBlockedTime < 1000L) return
        lastBlockedKey = pkg
        lastBlockedTime = now

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

            val now = System.currentTimeMillis()
            if (webKey == lastBlockedKey && now - lastBlockedTime < 1000L) return
            lastBlockedKey = webKey
            lastBlockedTime = now

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

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.dismiss()
        scope.cancel()
    }
}
