package com.notnow.app.ui.screen.home

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.AppCategory
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.ui.theme.*

@Composable
fun HomeScreen(
    onOpenVault: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenDashboard: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as NotNowApplication
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.appRuleRepository, app.preferences))

    val mode by vm.operatingMode.collectAsStateWithLifecycle()
    val nightOn by vm.nightLockdownEnabled.collectAsStateWithLifecycle()
    val rules by vm.rules.collectAsStateWithLifecycle()
    val emergencyUntil by vm.emergencyUnlockUntil.collectAsStateWithLifecycle()
    val isEmergencyActive = emergencyUntil > System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Not Now", style = MaterialTheme.typography.headlineLarge, color = AccentAmber)
            Text("Get Off the Impulse Train", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        // Emergency unlock banner
        if (isEmergencyActive) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = AccentRed.copy(alpha = 0.15f)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
        item {
            ModeSelector(current = mode, onSelect = vm::setMode)
        }

        // Night lockdown toggle
        item {
            Surface(shape = RoundedCornerShape(12.dp), color = CardDark) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Nightlight, null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Night Lockdown", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("11 PM – 7 AM: blocks social, shopping & entertainment", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Switch(checked = nightOn, onCheckedChange = vm::toggleNightLockdown,
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentAmber, checkedTrackColor = AccentAmber.copy(alpha = 0.3f)))
                }
            }
        }

        // Quick nav cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NavCard("🛍️", "Shopping\nVault", Modifier.weight(1f), onOpenVault)
                NavCard("💬", "Future\nMessages", Modifier.weight(1f), onOpenMessages)
                NavCard("📊", "Weekly\nReview", Modifier.weight(1f), onOpenDashboard)
            }
        }

        // App rules
        item {
            Text("Friction Rules", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        items(rules, key = { it.packageName }) { rule ->
            AppRuleRow(rule = rule, onToggle = { enabled -> vm.toggleRule(rule.packageName, enabled) })
        }

        if (rules.isEmpty()) {
            item {
                Text("No rules yet — restart the app to load defaults.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
            }
        }
    }
}

@Composable
private fun ModeSelector(current: String, onSelect: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = CardDark) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeButton("FOCUS", "Focus", current == "FOCUS", Icons.Default.CenterFocusStrong, Modifier.weight(1f)) { onSelect("FOCUS") }
            ModeButton("LIFE",  "Life",  current == "LIFE",  Icons.Default.Balance,            Modifier.weight(1f)) { onSelect("LIFE") }
        }
    }
}

@Composable
private fun ModeButton(mode: String, label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
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
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AppRuleRow(rule: AppRule, onToggle: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(10.dp), color = SurfaceDark) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.appName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(rule.frictionLevel.label(), style = MaterialTheme.typography.bodyMedium, color = frictionColor(rule.frictionLevel))
            }
            Switch(
                checked = rule.isEnabled, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = AccentAmber, checkedTrackColor = AccentAmber.copy(0.3f))
            )
        }
    }
}

private fun FrictionLevel.label() = when(this) {
    FrictionLevel.LEVEL_1_MINOR     -> "30 sec delay"
    FrictionLevel.LEVEL_2_ATTENTION -> "10 min delay"
    FrictionLevel.LEVEL_3_SPENDING  -> "60 min delay"
    FrictionLevel.LEVEL_4_BLOCKED   -> "Always blocked"
}

@Composable
private fun frictionColor(level: FrictionLevel) = when(level) {
    FrictionLevel.LEVEL_1_MINOR     -> AccentGreen
    FrictionLevel.LEVEL_2_ATTENTION -> AccentAmber
    FrictionLevel.LEVEL_3_SPENDING  -> AccentRed
    FrictionLevel.LEVEL_4_BLOCKED   -> AccentRed
}
