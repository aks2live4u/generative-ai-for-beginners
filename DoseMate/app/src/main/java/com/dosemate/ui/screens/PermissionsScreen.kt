package com.dosemate.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.ui.theme.TealDeep
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    GlassBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Almost ready", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LocalDoseMateColors.current.accent)
                Spacer(Modifier.height(6.dp))
                Text("DoseMate needs a few permissions to remind you on time.", fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))

                PermissionRow(
                    title = "Notifications",
                    subtitle = "So reminders can reach you",
                    onClick = { notificationPermission?.launchPermissionRequest() }
                )
                Spacer(Modifier.height(12.dp))
                PermissionRow(
                    title = "Alarms & reminders",
                    subtitle = "So reminders fire exactly on time",
                    onClick = { openExactAlarmSettings(context) }
                )
                Spacer(Modifier.height(12.dp))
                PermissionRow(
                    title = "Battery optimization (optional)",
                    subtitle = "Prevents the system from delaying reminders",
                    onClick = { openBatteryOptimizationSettings(context) }
                )

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                ) {
                    Text("Continue", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp)
            }
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = TealDeep)) {
                Text("Allow", fontSize = 12.sp)
            }
        }
    }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (!alarmManager.canScheduleExactAlarms()) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        }
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    val powerManager = context.getSystemService(PowerManager::class.java)
    if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
    }
}
