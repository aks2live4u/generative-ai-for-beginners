package com.aks.gallerysorter.ui.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aks.gallerysorter.ui.fragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** Images/videos (API 33+; below that All files access covers reads) and photo locations. */
fun mediaPermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.READ_MEDIA_IMAGES)
        add(Manifest.permission.READ_MEDIA_VIDEO)
    }
    add(Manifest.permission.ACCESS_MEDIA_LOCATION)
}

/** Scan progress notifications; a runtime permission from API 33. */
fun notificationPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()

fun Context.granted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** A counter that changes every time the screen resumes (re-check permissions after Settings). */
@Composable
fun resumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}

@Composable
fun MediaPermissionStep() {
    StepText(
        "Photos and videos",
        "Gallery Sorter reads your photos and videos to sort them, and their location tags to group them by place. " +
            "Everything stays on this phone.",
    )
    PermissionRow("Photos, videos and their locations", mediaPermissions())
    if (notificationPermissions().isNotEmpty()) PermissionRow("Scan progress notifications (optional)", notificationPermissions())
}

/**
 * One permission group: "Allowed ✓", an "Allow" button, or, once Android stops showing the
 * prompt (denied permanently), "Open app settings".
 */
@Composable
private fun PermissionRow(title: String, permissions: List<String>) {
    val context = LocalContext.current
    val tick = resumeTick()
    var asked by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        result++
    }
    val missing = remember(tick, result) { permissions.filterNot(context::granted) }
    val activity = context.fragmentActivity()
    val blocked = asked && missing.isNotEmpty() && activity != null &&
        missing.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    Text(title, style = MaterialTheme.typography.titleSmall)
    when {
        missing.isEmpty() -> Text("Allowed ✓")
        blocked -> Button(onClick = {
            context.tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }) { Text("Open app settings") }
        else -> Button(onClick = { launcher.launch(missing.toTypedArray()) }) { Text("Allow") }
    }
}

@Composable
fun AllFilesStep() {
    val context = LocalContext.current
    val tick = resumeTick()
    val granted = remember(tick) { Environment.isExternalStorageManager() }
    StepText(
        "All files access",
        "Hiding moves a file into the vault without leaving a copy behind. Android only allows that with \"All files access\".",
    )
    if (granted) {
        Text("Allowed ✓")
    } else {
        Button(onClick = {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
            if (!context.tryStart(intent)) context.tryStart(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }) { Text("Open settings") }
    }
}

const val VIVO_BACKGROUND_TEXT = "Settings → Battery → Background power consumption management → Gallery Sorter → Allow"

/** vivo / iQOO battery pages, tried in order; app details is the fallback. */
private val VIVO_BATTERY_ACTIVITIES = listOf(
    ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
    ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity"),
    ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
    ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
)

@Composable
fun BackgroundStep() {
    val context = LocalContext.current
    StepText(
        "Let the scan run in the background",
        "On vivo phones, allow background power use so the first scan isn't stopped:\n\n$VIVO_BACKGROUND_TEXT",
    )
    Button(onClick = { openBatterySettings(context) }) { Text("Open battery settings") }
}

fun openBatterySettings(context: Context) {
    for (c in VIVO_BATTERY_ACTIVITIES) {
        if (context.tryStart(Intent().setComponent(c))) return
    }
    context.tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
}

private fun Context.tryStart(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    Log.i("Onboarding", "settings page not allowed: ${e.javaClass.name}")
    false
}
