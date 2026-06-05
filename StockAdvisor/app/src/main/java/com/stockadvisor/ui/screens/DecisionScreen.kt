package com.stockadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockadvisor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionScreen(
    symbol: String,
    onDecision: (String, Int?, Double?) -> Unit,
    onBack: () -> Unit
) {
    var showPositionDialog by remember { mutableStateOf(false) }
    var pendingDecision by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var avgPrice by remember { mutableStateOf("") }

    if (showPositionDialog) {
        AlertDialog(
            onDismissRequest = { showPositionDialog = false },
            containerColor = Surface,
            title = {
                Text(
                    "Your $symbol Position",
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter your holding details for personalised P&L analysis. Both fields are optional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                        label = { Text("Quantity held (units/shares)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = SurfaceVariant,
                            focusedLabelColor = GoldAccent,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = avgPrice,
                        onValueChange = { v ->
                            if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) avgPrice = v
                        },
                        label = { Text("Avg buy price (₹ / $)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = SurfaceVariant,
                            focusedLabelColor = GoldAccent,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPositionDialog = false
                        onDecision(
                            pendingDecision,
                            quantity.trim().toIntOrNull(),
                            avgPrice.trim().toDoubleOrNull()
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = OnPrimary)
                ) { Text("Analyse", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPositionDialog = false
                        onDecision(pendingDecision, null, null)
                    }
                ) { Text("Skip", color = OnSurfaceVariant) }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = symbol,
                    style = MaterialTheme.typography.headlineLarge,
                    color = GreenAccent,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "What would you like to do?",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                DecisionCard(
                    label = "BUY",
                    description = "Analyse if buying is a wise decision",
                    color = GreenAccent,
                    darkColor = GreenDark,
                    icon = Icons.Default.AddCircle,
                    onClick = { onDecision("BUY", null, null) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DecisionCard(
                    label = "SELL",
                    description = "Analyse if selling now makes sense",
                    subtitle = "Enter your holding details for P&L analysis",
                    color = RedAccent,
                    darkColor = RedDark,
                    icon = Icons.Default.RemoveCircle,
                    onClick = {
                        pendingDecision = "SELL"
                        quantity = ""; avgPrice = ""
                        showPositionDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DecisionCard(
                    label = "HOLD",
                    description = "Analyse if holding is the right move",
                    subtitle = "Enter your holding details for P&L analysis",
                    color = AmberAccent,
                    darkColor = AmberDark,
                    icon = Icons.Default.PauseCircle,
                    onClick = {
                        pendingDecision = "HOLD"
                        quantity = ""; avgPrice = ""
                        showPositionDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun DecisionCard(
    label: String,
    description: String,
    subtitle: String = "",
    color: Color,
    darkColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle.isNotBlank()) 110.dp else 100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (pressed) darkColor.copy(alpha = 0.3f) else Surface)
            .clickable { pressed = true; onClick() }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(40.dp))
            Column {
                Text(text = label, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                if (subtitle.isNotBlank()) {
                    Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
                }
            }
        }
    }
}
