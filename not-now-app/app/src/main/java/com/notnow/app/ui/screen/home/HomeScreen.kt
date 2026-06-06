package com.notnow.app.ui.screen.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.ui.theme.*

@Composable
fun HomeScreen(
    onOpenVault: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenCustomRules: () -> Unit,
    onOpenWebsites: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as NotNowApplication
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.appRuleRepository, app.preferences, context)
    )

    val mode            by vm.operatingMode.collectAsStateWithLifecycle()
    val nightOn         by vm.nightLockdownEnabled.collectAsStateWithLifecycle()
    val nightStart      by vm.nightStartHour.collectAsStateWithLifecycle()
    val nightEnd        by vm.nightEndHour.collectAsStateWithLifecycle()
    val rules           by vm.rules.collectAsStateWithLifecycle()
    val emergencyUntil  by vm.emergencyUnlockUntil.collectAsStateWithLifecycle()
    val isEmergencyActive = emergencyUntil > System.currentTimeMillis()

    // Re-check on every recomposition (user might enable service while app is open)
    val accessibilityOk = vm.isAccessibilityEnabled()

    var showNightPicker by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // Header
        item {
            Text("Not Now", style = MaterialTheme.typography.headlineLarge, color = AccentAmber)
            Text("Get Off the Impulse Train", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        // ── CRITICAL: Accessibility Service Banner ─────────────────────────
        if (!accessibilityOk) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentRed.copy(alpha = 0.15f),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Guardrail is OFF",
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentRed
                            )
                            Text(
                                "Tap here → find \"Not Now Guardrail\" → Enable",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = AccentRed)
                    }
                }
            }
        } else {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = AccentGreen.copy(alpha = 0.12f)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Shield, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        Text(
                            "Guardrail is active",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentGreen
                        )
                    }
                }
            }
        }

        // Emergency unlock banner
        if (isEmergencyActive) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = AccentRed.copy(alpha = 0.15f)) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Emergency Unlock Active", style = MaterialTheme.typography.titleMedium, color = AccentRed)
                            Text("All restrictions lifted for 15 min", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        TextButton(onClick = { vm.clearEmergencyUnlock() }) {
                            Text("End Early", color = AccentRed)
                        }
                    }
                }
            }
        }

        // Mode selector
        item { ModeSelector(current = mode, onSelect = vm::setMode) }

        // Night lockdown card (now tappable to change hours)
        item {
            Surface(shape = RoundedCornerShape(12.dp), color = CardDark) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Nightlight, null, tint = AccentAmber, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Night Lockdown", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                "${hourLabel(nightStart)} – ${hourLabel(nightEnd)}: blocks social, shopping & entertainment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = nightOn,
                            onCheckedChange = vm::toggleNightLockdown,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentAmber,
                                checkedTrackColor = AccentAmber.copy(alpha = 0.3f)
                            )
                        )
                    }
                    if (nightOn) {
                        HorizontalDivider(color = BorderDark, modifier = Modifier.padding(horizontal = 16.dp))
                        TextButton(
                            onClick = { showNightPicker = true },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Schedule, null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Change hours (${hourLabel(nightStart)} – ${hourLabel(nightEnd)})", color = AccentAmber, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Quick nav cards — row 1
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NavCard("🛍️", "Shopping\nVault", Modifier.weight(1f), onOpenVault)
                NavCard("💬", "Future\nMessages", Modifier.weight(1f), onOpenMessages)
                NavCard("📊", "Weekly\nReview", Modifier.weight(1f), onOpenDashboard)
            }
        }

        // Quick nav cards — row 2 (new features)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NavCard("📱", "Block\nApps", Modifier.weight(1f), onOpenCustomRules)
                NavCard("🌐", "Block\nSites", Modifier.weight(1f), onOpenWebsites)
            }
        }

        // Friction rules header + reset button
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Friction Rules", style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = { showResetConfirm = true }) {
                    Icon(Icons.Default.Refresh, null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset", color = AccentAmber, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (rules.isEmpty()) {
            item {
                Text(
                    "No rules — tap Reset to restore defaults.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            }
        }

        items(rules, key = { it.packageName }) { rule ->
            AppRuleRow(
                rule = rule,
                onToggle = { enabled -> vm.toggleRule(rule.packageName, enabled) }
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    // Night hours picker dialog
    if (showNightPicker) {
        NightHoursDialog(
            startHour = nightStart,
            endHour = nightEnd,
            onConfirm = { s, e ->
                vm.setNightHours(s, e)
                showNightPicker = false
            },
            onDismiss = { showNightPicker = false }
        )
    }

    // Reset confirm dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = CardDark,
            title = { Text("Reset to Defaults?", color = TextPrimary) },
            text = { Text("This will re-enable all default apps (YouTube, Instagram, Amazon, etc.) at their original delay times.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { vm.resetToDefaults(); showResetConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                ) { Text("Reset", color = DeepNavy) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun NightHoursDialog(
    startHour: Int,
    endHour: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var start by remember { mutableIntStateOf(startHour) }
    var end   by remember { mutableIntStateOf(endHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Night Lockdown Hours", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Locks apps during these hours every day.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Starts at: ${hourLabel(start)}", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = start.toFloat(),
                        onValueChange = { start = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = AccentAmber, activeTrackColor = AccentAmber)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ends at: ${hourLabel(end)}", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = end.toFloat(),
                        onValueChange = { end = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = AccentAmber, activeTrackColor = AccentAmber)
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = AccentAmber.copy(alpha = 0.1f)) {
                    Text(
                        "Locked: ${hourLabel(start)} → ${hourLabel(end)}",
                        color = AccentAmber,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(start, end) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
            ) { Text("Save", color = DeepNavy) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun ModeSelector(current: String, onSelect: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = CardDark) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeButton("FOCUS", "Focus", current == "FOCUS", Icons.Default.CenterFocusStrong, Modifier.weight(1f)) { onSelect("FOCUS") }
            ModeButton("LIFE", "Life", current == "LIFE", Icons.Default.Balance, Modifier.weight(1f)) { onSelect("LIFE") }
        }
    }
}

@Composable
private fun ModeButton(
    mode: String, label: String, selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier, onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AccentAmber else DeepNavy,
            contentColor   = if (selected) DeepNavy    else TextSecondary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun NavCard(emoji: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = CardDark, onClick = onClick, modifier = modifier) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 26.sp)
            Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AppRuleRow(rule: AppRule, onToggle: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (rule.isEnabled) SurfaceDark else SurfaceDark.copy(alpha = 0.5f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    rule.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (rule.isEnabled) TextPrimary else TextSecondary
                )
                Text(
                    rule.frictionLevel.label(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (rule.isEnabled) frictionColor(rule.frictionLevel) else TextSecondary
                )
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor  = AccentAmber,
                    checkedTrackColor  = AccentAmber.copy(0.3f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = BorderDark
                )
            )
        }
    }
}

private fun FrictionLevel.label() = when (this) {
    FrictionLevel.LEVEL_1_MINOR     -> "30 sec delay"
    FrictionLevel.LEVEL_2_ATTENTION -> "10 min delay"
    FrictionLevel.LEVEL_3_SPENDING  -> "60 min delay"
    FrictionLevel.LEVEL_4_BLOCKED   -> "Always blocked"
}

@Composable
private fun frictionColor(level: FrictionLevel) = when (level) {
    FrictionLevel.LEVEL_1_MINOR     -> AccentGreen
    FrictionLevel.LEVEL_2_ATTENTION -> AccentAmber
    FrictionLevel.LEVEL_3_SPENDING  -> AccentRed
    FrictionLevel.LEVEL_4_BLOCKED   -> AccentRed
}

private fun hourLabel(hour: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val h = when (hour) { 0 -> 12; in 13..23 -> hour - 12; else -> hour }
    return "$h:00 $suffix"
}
