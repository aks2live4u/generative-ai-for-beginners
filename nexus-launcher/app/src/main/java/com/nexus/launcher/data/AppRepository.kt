package com.nexus.launcher.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppCategory(val label: String) {
    COMMS("Comms"), MEDIA("Media"), WORK("Work"), CREATE("Create"), NAV("Navigate"), OTHER("Other")
}

data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val icon: ImageBitmap,
    val category: AppCategory,
)

/** Every launchable app on the device, plus how often NEXUS has opened each one. */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {
    private val pm = context.packageManager
    private val usagePrefs = context.getSharedPreferences("nexus_usage", Context.MODE_PRIVATE)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _usage = MutableStateFlow(readUsage())
    val usage: StateFlow<Map<String, Int>> = _usage.asStateFlow()

    private var lastLoad = 0L

    init {
        reload()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) = reload()
            },
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    fun refreshIfStale() {
        if (SystemClock.elapsedRealtime() - lastLoad > 5 * 60_000) reload()
    }

    fun reload() {
        scope.launch {
            _apps.value = withContext(Dispatchers.IO) { query() }
            lastLoad = SystemClock.elapsedRealtime()
        }
    }

    private fun query(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val iconPx = (56 * context.resources.displayMetrics.density).toInt()
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { ri ->
                runCatching {
                    val ai = ri.activityInfo
                    AppEntry(
                        label = ri.loadLabel(pm).toString(),
                        packageName = ai.packageName,
                        component = ComponentName(ai.packageName, ai.name),
                        icon = ri.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap(),
                        category = categorize(ai.applicationInfo),
                    )
                }.getOrNull()
            }
            .distinctBy { it.component }
            .sortedBy { it.label.lowercase() }
    }

    private fun categorize(info: ApplicationInfo): AppCategory = when (info.category) {
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.COMMS
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_GAME, ApplicationInfo.CATEGORY_NEWS -> AppCategory.MEDIA
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.WORK
        ApplicationInfo.CATEGORY_IMAGE -> AppCategory.CREATE
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.NAV
        else -> AppCategory.OTHER
    }

    /** Apps ordered by how often they're opened, then alphabetically. */
    fun ranked(apps: List<AppEntry>, usage: Map<String, Int>): List<AppEntry> =
        apps.sortedWith(compareByDescending<AppEntry> { usage[it.packageName] ?: 0 }.thenBy { it.label.lowercase() })

    fun launch(app: AppEntry): Boolean {
        val intent = Intent.makeMainActivity(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching { context.startActivity(intent) }.isSuccess.also { ok ->
            if (ok) recordLaunch(app.packageName)
        }
    }

    fun openAppInfo(app: AppEntry) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Loose match used by voice commands: "open spotify" → Spotify. */
    fun find(name: String): AppEntry? {
        val q = name.trim().lowercase()
        if (q.isEmpty()) return null
        val list = _apps.value
        return list.firstOrNull { it.label.lowercase() == q }
            ?: list.firstOrNull { it.label.lowercase().startsWith(q) }
            ?: list.firstOrNull { it.label.lowercase().contains(q) }
            ?: list.firstOrNull { q.contains(it.label.lowercase()) && it.label.length > 2 }
    }

    private fun recordLaunch(pkg: String) {
        val next = _usage.value.toMutableMap().apply { this[pkg] = (this[pkg] ?: 0) + 1 }
        usagePrefs.edit().putInt(pkg, next[pkg] ?: 1).apply()
        _usage.value = next
    }

    private fun readUsage(): Map<String, Int> =
        usagePrefs.all.mapNotNull { (k, v) -> (v as? Int)?.let { k to it } }.toMap()
}
