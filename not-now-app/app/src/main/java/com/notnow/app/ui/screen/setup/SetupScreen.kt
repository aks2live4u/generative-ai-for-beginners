package com.notnow.app.ui.screen.setup

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notnow.app.data.preferences.AppPreferences
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var accessibilityGranted by remember {
        mutableStateOf(isAccessibilityEnabled(context))
    }
    var usageStatsGranted by remember { mutableStateOf(isUsageStatsGranted(context)) }

    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        overlayGranted = Settings.canDrawOverlays(context)
    }
    val accessibilityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        accessibilityGranted = isAccessibilityEnabled(context)
    }
    val usageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        usageStatsGranted = isUsageStatsGranted(context)
    }

    val allGranted = overlayGranted && accessibilityGranted && usageStatsGranted
    val prefs = remember { AppPreferences.getInstance(context) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("NOT NOW", color = Color(0xFFE85D04), fontSize = 13.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            Text("Setup Required", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Three permissions are needed. None collect your data — everything stays on your phone.",
                color = Color(0xFF888888),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                title = "Draw Over Apps",
                description = "Shows the countdown overlay when you open a restricted app",
                granted = overlayGranted,
                onClick = {
                    overlayLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    )
                }
            )

            PermissionRow(
                title = "Accessibility Service",
                description = "Detects which app is in the foreground — the core guardrail mechanism",
                granted = accessibilityGranted,
                onClick = {
                    accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            PermissionRow(
                title = "Usage Access",
                description = "Tracks usage patterns for the weekly reflection dashboard",
                granted = usageStatsGranted,
                onClick = {
                    usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch { prefs.setSetupComplete(true) }
                    onSetupComplete()
                },
                enabled = allGranted,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE85D04),
                    disabledContainerColor = Color(0xFF2A2A2A)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (allGranted) "Start Not Now" else "Grant all permissions above",
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = if (allGranted) Color.White else Color(0xFF555555)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (granted) Color(0xFF1A3A1A) else Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            if (granted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(description, color = Color(0xFF666666), fontSize = 12.sp)
        }

        if (!granted) {
            TextButton(onClick = onClick) {
                Text("Grant", color = Color(0xFFE85D04), fontSize = 13.sp)
            }
        }
    }
}

private fun isAccessibilityEnabled(context: android.content.Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabled?.contains(context.packageName) == true
}

private fun isUsageStatsGranted(context: android.content.Context): Boolean {
    return try {
        val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        mode == android.app.AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }
}
