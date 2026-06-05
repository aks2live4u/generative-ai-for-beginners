package com.notnow.app.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import kotlin.math.roundToInt

@Composable
fun WeeklyDashboardScreen(app: NotNowApplication, onBack: () -> Unit) {
    val vm: WeeklyDashboardViewModel = viewModel(
        factory = WeeklyDashboardViewModelFactory(app.usageRecordRepository, app.shoppingVaultRepository)
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D)).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text("Weekly Review", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Last 7 days · awareness only", color = Color(0xFF666666), fontSize = 12.sp)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Protected Hours",
                    value = "%.1f h".format(state.protectedHours),
                    color = Color(0xFF1A3A1A),
                    textColor = Color(0xFF66CC66),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Items Saved\nto Vault",
                    value = state.itemsSaved.toString(),
                    color = Color(0xFF1E3A5F),
                    textColor = Color(0xFF6699CC),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Peak Trigger\nTime",
                    value = state.peakTriggerTime,
                    color = Color(0xFF2A1A1A),
                    textColor = Color(0xFFE85D04),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Most\nAttempted",
                    value = state.mostAttemptedApp,
                    color = Color(0xFF2A1A2A),
                    textColor = Color(0xFFCC66CC),
                    modifier = Modifier.weight(1f),
                    valueFontSize = 14
                )
            }
        }

        item {
            StatCard(
                label = "Emergency Unlocks This Week",
                value = state.emergencyUnlocks.toString(),
                color = if (state.emergencyUnlocks > 3) Color(0xFF3A1A1A) else Color(0xFF1A1A1A),
                textColor = if (state.emergencyUnlocks > 3) Color(0xFFE85D04) else Color(0xFF888888),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.records.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data yet. Your stats will appear\nonce the guardrail runs.", color = Color(0xFF555555), fontSize = 14.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    textColor: Color,
    modifier: Modifier,
    valueFontSize: Int = 28
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = Color(0xFF888888), fontSize = 11.sp, lineHeight = 15.sp)
        Text(value, color = textColor, fontSize = valueFontSize.sp, fontWeight = FontWeight.Bold)
    }
}
