package com.nexus.launcher.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

/**
 * Android's "Usage access" special permission. With it, NEXUS ranks apps by how long you
 * actually use them, including apps you open from notifications or other launchers.
 */
object UsageAccess {
    fun isGranted(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Total foreground minutes per package over the last [days] days. */
    fun foregroundMinutes(context: Context, days: Int): Map<String, Int> {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - days * 24L * 60 * 60 * 1000
        return runCatching {
            usm.queryAndAggregateUsageStats(start, end)
                .mapValues { (_, stats) -> (stats.totalTimeInForeground / 60_000L).toInt() }
                .filterValues { it > 0 }
        }.getOrDefault(emptyMap())
    }

    /** Opens the system screen where the user switches usage access on for NEXUS. */
    fun openSettings(context: Context) {
        val direct = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(direct) }.onFailure {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
