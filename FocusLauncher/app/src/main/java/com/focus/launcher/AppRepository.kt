package com.focus.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
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

    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val browsers = pm.queryIntentActivities(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_ALL
        ).map { it.activityInfo.packageName }.toSet()

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcherIntent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                val info = ri.activityInfo
                AppEntry(
                    key = "${info.packageName}/${info.name}",
                    pkg = info.packageName,
                    cls = info.name,
                    label = ri.loadLabel(pm).toString(),
                    kind = classify(info.packageName, info.applicationInfo, browsers),
                )
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
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
        return wanted.distinct().mapNotNull { pkg -> apps.firstOrNull { it.pkg == pkg }?.key }
    }

    fun launch(context: Context, app: AppEntry): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.pkg, app.cls))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    fun openAppInfo(context: Context, pkg: String) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
