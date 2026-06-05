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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.AppCategory
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.data.preferences.OperatingMode
import com.notnow.app.service.GuardrailAccessibilityService

@Composable
fun HomeScreen(
    app: NotNowApplication,
    onNavigateToVault: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(app.appRuleRepository, app.appPreferences)
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val serviceRunning = GuardrailAccessibilityService.isRunning

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NOT NOW", color = Color(0xFFE85D04), fontSize = 11.sp, letterSpacing = 3.sp)
                    Text("Guardrail", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (serviceRunning) Color(0xFF1A3A1A) else Color(0xFF3A1A1A))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (serviceRunning) "Active" else "Inactive",
                        color = if (serviceRunning) Color(0xFF4CAF50) else Color(0xFFE85D04),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (state.emergencyUnlockActive) {
            item {
                EmergencyUnlockBanner(
                    minutesLeft = state.emergencyUnlockMinutesLeft,
                    onCancel = { vm.cancelEmergencyUnlock() }
                )
            }
        }

        if (state.nightLockdownActive) {
            item { NightLockdownBanner() }
        }

        item { ModeToggle(mode = state.operatingMode, onModeChange = { vm.setOperatingMode(it) }) }

        item {
            SectionCard(title = "Controls") {
                ToggleRow("Guardrail System", "Master on/off switch", state.guardrailEnabled) {
                    vm.toggleGuardrail(it)
                }
                HorizontalDivider(color = Color(0xFF222222))
                ToggleRow("Night Lockdown", "11 PM – 7 AM blocking", state.nightLockdownEnabled) {
                    vm.toggleNightLockdown(it)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickNavCard("Shopping\nVault", Icons.Default.ShoppingCart, Color(0xFF1E3A5F), Modifier.weight(1f)) {
                    onNavigateToVault()
                }
                QuickNavCard("Future\nMessages", Icons.Default.Message, Color(0xFF1A2A1A), Modifier.weight(1f)) {
                    onNavigateToMessages()
                }
                QuickNavCard("Weekly\nReview", Icons.Default.BarChart, Color(0xFF2A1A2A), Modifier.weight(1f)) {
                    onNavigateToDashboard()
                }
            }
        }

        item {
            Text("App Rules", color = Color(0xFF888888), fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp))
        }

        items(state.rules) { rule ->
            AppRuleRow(rule = rule, onToggle = { vm.toggleAppRule(rule.packageName, it) })
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun EmergencyUnlockBanner(minutesLeft: Int, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF3A2000))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFFE85D04), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Emergency unlock — $minutesLeft min left", color = Color(0xFFFFAA55), fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel) { Text("End", color = Color(0xFFE85D04), fontSize = 13.sp) }
    }
}

@Composable
private fun NightLockdownBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A2A))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Nightlight, contentDescription = null, tint = Color(0xFF6666CC), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Night Lockdown active — 11 PM to 7 AM", color = Color(0xFF8888CC), fontSize = 13.sp)
    }
}

@Composable
private fun ModeToggle(mode: OperatingMode, onModeChange: (OperatingMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .padding(4.dp)
    ) {
        listOf(OperatingMode.FOCUS to "Focus Mode", OperatingMode.LIFE to "Life Mode").forEach { (m, label) ->
            Button(
                onClick = { onModeChange(m) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mode == m) Color(0xFFE85D04) else Color.Transparent,
                    contentColor = if (mode == m) Color.White else Color(0xFF666666)
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(label, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
    ) {
        Text(title, color = Color(0xFF666666), fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
        content()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF666666), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFE85D04),
                uncheckedThumbColor = Color(0xFF666666),
                uncheckedTrackColor = Color(0xFF2A2A2A)
            )
        )
    }
}

@Composable
private fun QuickNavCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun AppRuleRow(rule: AppRule, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(rule.appName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "${rule.category.name.lowercase().replace('_', ' ')} · ${rule.frictionLevel.label}",
                color = frictionColor(rule.frictionLevel),
                fontSize = 11.sp
            )
        }
        Switch(
            checked = rule.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = frictionColor(rule.frictionLevel),
                uncheckedThumbColor = Color(0xFF444444),
                uncheckedTrackColor = Color(0xFF222222)
            )
        )
    }
}

private fun frictionColor(level: FrictionLevel) = when (level) {
    FrictionLevel.LEVEL_1_DISTRACTION -> Color(0xFF888888)
    FrictionLevel.LEVEL_2_ATTENTION_TRAP -> Color(0xFFE8A504)
    FrictionLevel.LEVEL_3_SPENDING -> Color(0xFFE85D04)
    FrictionLevel.LEVEL_4_BLOCKED -> Color(0xFFCF6679)
}

class HomeViewModelFactory(
    private val ruleRepo: com.notnow.app.data.repository.AppRuleRepository,
    private val prefs: com.notnow.app.data.preferences.AppPreferences
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(ruleRepo, prefs) as T
    }
}
