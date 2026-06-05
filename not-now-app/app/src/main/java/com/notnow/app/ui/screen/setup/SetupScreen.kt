package com.notnow.app.ui.screen.setup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notnow.app.service.GuardrailAccessibilityService
import com.notnow.app.ui.theme.*

@Composable
fun SetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    var accessibilityOk by remember { mutableStateOf(false) }
    var overlayOk       by remember { mutableStateOf(false) }
    var usageOk         by remember { mutableStateOf(false) }

    fun refresh() {
        accessibilityOk = GuardrailAccessibilityService.isEnabled(context)
        overlayOk       = Settings.canDrawOverlays(context)
        usageOk         = hasUsageStatsPermission(context)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresh()
    }

    LaunchedEffect(Unit) { refresh() }

    val allDone = accessibilityOk && overlayOk && usageOk

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👋", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("Set Up Not Now", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, textAlign = TextAlign.Center)
        Text(
            "Three quick permissions — all on-device, nothing leaves your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(32.dp))

        PermissionItem(
            title = "Accessibility Service",
            subtitle = "Detects when a restricted app opens",
            granted = accessibilityOk,
            onClick = {
                launcher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )
        Spacer(Modifier.height(12.dp))
        PermissionItem(
            title = "Draw Over Apps",
            subtitle = "Shows the countdown on top of blocked apps",
            granted = overlayOk,
            onClick = {
                launcher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
            }
        )
        Spacer(Modifier.height(12.dp))
        PermissionItem(
            title = "Usage Access",
            subtitle = "Tracks which apps you tried to open (locally only)",
            granted = usageOk,
            onClick = {
                launcher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { if (allDone) onSetupComplete() else refresh() },
            enabled = allDone,
            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                if (allDone) "All Set — Let's Go" else "Grant All Three Above",
                color = DeepNavy,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun PermissionItem(title: String, subtitle: String, granted: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardDark,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (granted) AccentGreen.copy(alpha = 0.2f) else BorderDark,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (granted) Icon(Icons.Default.Check, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            if (!granted) Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
        }
    }
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    return try {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 1000, System.currentTimeMillis())
        stats != null && stats.isNotEmpty()
    } catch (e: Exception) { false }
}
