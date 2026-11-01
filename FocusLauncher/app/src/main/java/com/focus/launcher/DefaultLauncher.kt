package com.focus.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Checking and requesting "default home app" status. */
object DefaultLauncher {

    fun isDefault(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) return rm.isRoleHeld(RoleManager.ROLE_HOME)
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /** Fallback: the system "Default apps → Home app" screen. */
    fun openHomeSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in intents) {
            val ok = runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)
            if (ok) return
        }
    }
}

/**
 * Returns a function that asks Android to make this the default home app.
 * On Android 10+ this shows the system "Set Focus Launcher as your default home app?" dialog;
 * if that is unavailable or was dismissed, it opens the Home-app settings screen instead.
 */
@Composable
fun rememberMakeDefaultRequest(onResult: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!DefaultLauncher.isDefault(context)) DefaultLauncher.openHomeSettings(context)
        onResult()
    }
    return {
        val rm = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java) else null
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            runCatching { launcher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)) }
                .onFailure { DefaultLauncher.openHomeSettings(context) }
        } else {
            DefaultLauncher.openHomeSettings(context)
        }
    }
}
