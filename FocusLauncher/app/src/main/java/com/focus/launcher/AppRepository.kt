package com.focus.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony

enum class AppKind(val tag: String) {
    SHOPPING("shopping"), SOCIAL("social"), VIDEO("video"), BROWSER("browser"),
    GAME("game"), NEWS("news"), OTHER("");

    val isDistracting: Boolean get() = this != OTHER
}

data class AppEntry(
    val key: String,
    val pkg: String,
    val cls: String,
    val label: String,
    val kind: AppKind,
    /** Which profile the app lives in: main, clone/dual-app, or work profile. */
    val user: UserHandle,
    val isClone: Boolean = false,
)

object AppRepository {

    // Known shopping apps (prefix match on package name). Android has no "shopping" category.
    private val shoppingPrefixes = listOf(
        "com.amazon.mShop", "in.amazon.mShop", "com.amazon.windowshop",
        "com.flipkart", "com.myntra", "com.meesho", "com.ril.ajio", "com.fsn.nykaa", "com.snapdeal",
        "club.shopsy", "com.tatadigital", "com.ebay", "com.alibaba.aliexpress", "com.einnovation.temu",
        "com.zzkko", "com.etsy", "com.walmart", "com.target", "com.contextlogic", "com.shopify.arrive",
        "com.bestbuy", "com.lenskart", "com.firstcry", "com.purplle", "com.nykaa", "com.shein",
    )

    // Feeds that are built to keep you scrolling.
    private val socialPrefixes = listOf(
        "com.instagram", "com.facebook.katana", "com.facebook.lite", "com.zhiliaoapp.musically",
        "com.ss.android.ugc", "com.snapchat", "com.twitter.android", "com.reddit", "com.pinterest",
        "in.mohalla", "com.threads", "com.instagram.barcelona", "com.tumblr", "com.josh",
        "com.google.android.youtube", "com.netflix", "in.startv.hotstar", "com.amazon.avod",
    )

    /**
     * Lists launchable apps from every profile the phone exposes to launchers, so clone apps
     * (Dual apps / App twin / Parallel apps, e.g. a second WhatsApp) and work apps show up too.
     */
    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val userManager = context.getSystemService(UserManager::class.java)
        val me = Process.myUserHandle()
        val browsers = pm.queryIntentActivities(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_ALL
        ).map { it.activityInfo.packageName }.toSet()

        val profiles = runCatching { launcherApps.profiles }.getOrNull()?.takeIf { it.isNotEmpty() } ?: listOf(me)
        val result = mutableListOf<AppEntry>()
        for (user in profiles) {
            val isMain = user == me
            val serial = runCatching { userManager.getSerialNumberForUser(user) }.getOrDefault(0L)
            val activities = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            for (info in activities) {
                val cn = info.componentName
                if (cn.packageName == context.packageName) continue
                val baseLabel = info.label?.toString() ?: cn.packageName
                val label = if (isMain) baseLabel else {
                    // Uses the phone's own wording ("Dual WhatsApp", "Work Gmail") when it has one.
                    val badged = runCatching { pm.getUserBadgedLabel(baseLabel, user).toString() }.getOrDefault(baseLabel)
                    if (badged == baseLabel) "$baseLabel (2)" else badged
                }
                result += AppEntry(
                    // Main-profile keys keep the old format so saved essentials survive updates.
                    key = if (isMain) "${cn.packageName}/${cn.className}" else "${cn.packageName}/${cn.className}@$serial",
                    pkg = cn.packageName,
                    cls = cn.className,
                    label = label,
                    kind = classify(cn.packageName, info.applicationInfo, browsers),
                    user = user,
                    isClone = !isMain,
                )
            }
        }
        return result.distinctBy { it.key }.sortedBy { it.label.lowercase() }
    }

    private fun classify(pkg: String, ai: ApplicationInfo, browsers: Set<String>): AppKind = when {
        shoppingPrefixes.any { pkg.startsWith(it) } -> AppKind.SHOPPING
        socialPrefixes.any { pkg.startsWith(it) } -> AppKind.SOCIAL
        pkg in browsers -> AppKind.BROWSER
        ai.category == ApplicationInfo.CATEGORY_SOCIAL -> AppKind.SOCIAL
        ai.category == ApplicationInfo.CATEGORY_VIDEO -> AppKind.VIDEO
        ai.category == ApplicationInfo.CATEGORY_GAME -> AppKind.GAME
        ai.category == ApplicationInfo.CATEGORY_NEWS -> AppKind.NEWS
        else -> AppKind.OTHER
    }

    /** Phone, messages, camera, clock — a sensible starting set of essentials. */
    fun defaultEssentials(context: Context, apps: List<AppEntry>): List<String> {
        val pm = context.packageManager
        fun pkgFor(intent: Intent): String? =
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName

        val wanted = listOfNotNull(
            pkgFor(Intent(Intent.ACTION_DIAL)),
            runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull(),
            pkgFor(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
            pkgFor(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)),
        )
        return wanted.distinct().mapNotNull { pkg -> apps.firstOrNull { it.pkg == pkg && !it.isClone }?.key }
    }

    fun launch(context: Context, app: AppEntry): Boolean = runCatching {
        context.getSystemService(LauncherApps::class.java)
            .startMainActivity(ComponentName(app.pkg, app.cls), app.user, null, null)
        true
    }.recoverCatching {
        // Fallback for the main profile if LauncherApps refuses for some reason.
        if (app.user != Process.myUserHandle()) throw it
        context.startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(app.pkg, app.cls))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        )
        true
    }.getOrDefault(false)

    fun openAppInfo(context: Context, app: AppEntry) {
        runCatching {
            context.getSystemService(LauncherApps::class.java)
                .startAppDetailsActivity(ComponentName(app.pkg, app.cls), app.user, null, null)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.pkg, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
