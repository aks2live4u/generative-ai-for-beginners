package com.dosemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.ui.theme.TealDeep

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val colors = LocalDoseMateColors.current
    GlassBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .background(colors.iconSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💊", fontSize = 40.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("DoseMate", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Never miss your medicines again.",
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                    ) {
                        Text("Get Started", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
