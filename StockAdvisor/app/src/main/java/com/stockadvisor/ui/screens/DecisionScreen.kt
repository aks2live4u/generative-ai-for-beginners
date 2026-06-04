package com.stockadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockadvisor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionScreen(
    symbol: String,
    onDecision: (String) -> Unit,
    onBack: () -> Unit
) {
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
                    .padding(horizontal = 24.dp),
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
                    description = "Analyze if buying is a wise decision",
                    color = GreenAccent,
                    darkColor = GreenDark,
                    icon = Icons.Default.AddCircle,
                    onClick = { onDecision("BUY") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DecisionCard(
                    label = "SELL",
                    description = "Analyze if selling now makes sense",
                    color = RedAccent,
                    darkColor = RedDark,
                    icon = Icons.Default.RemoveCircle,
                    onClick = { onDecision("SELL") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DecisionCard(
                    label = "HOLD",
                    description = "Analyze if holding is the right move",
                    color = AmberAccent,
                    darkColor = AmberDark,
                    icon = Icons.Default.PauseCircle,
                    onClick = { onDecision("HOLD") }
                )
            }
        }
    }
}

@Composable
private fun DecisionCard(
    label: String,
    description: String,
    color: Color,
    darkColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
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
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Column {
                Text(
                    text = label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
