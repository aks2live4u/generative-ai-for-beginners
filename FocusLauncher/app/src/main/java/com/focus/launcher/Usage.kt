package com.focus.launcher

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

/** Screen-time helpers. Everything here is optional: it only works once the user grants Usage Access. */
object Usage {

    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openAccessSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Foreground milliseconds per package since midnight, built from raw resume/pause events. */
    fun todayByPackage(context: Context): Map<String, Long> {
        if (!hasAccess(context)) return emptyMap()
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(start, now) ?: return emptyMap()
        val event = UsageEvents.Event()
        val openSince = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                RESUMED -> openSince.putIfAbsent(pkg, event.timeStamp)
                PAUSED -> openSince.remove(pkg)?.let { begin ->
                    total[pkg] = (total[pkg] ?: 0L) + (event.timeStamp - begin)
                }
            }
        }
        openSince.forEach { (pkg, begin) -> total[pkg] = (total[pkg] ?: 0L) + (now - begin) }
        return total
    }

    /** Total time today in apps other than this launcher and the system UI. */
    fun screenTimeToday(context: Context): Long =
        todayByPackage(context)
            .filterKeys { it != context.packageName && it != "com.android.systemui" }
            .values.sum()

    fun format(ms: Long): String {
        val minutes = ms / 60_000
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    // ACTIVITY_RESUMED / ACTIVITY_PAUSED (same values as the older MOVE_TO_FOREGROUND / BACKGROUND).
    private const val RESUMED = 1
    private const val PAUSED = 2
}
