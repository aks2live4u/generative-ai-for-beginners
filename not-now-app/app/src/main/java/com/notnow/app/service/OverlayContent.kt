package com.notnow.app.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notnow.app.data.repository.FutureMessageRepository
import com.notnow.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CountdownContent(
    appName: String,
    totalSec: Long,
    messageRepo: FutureMessageRepository,
    onOpen: () -> Unit,
    onGoBack: () -> Unit,
    onEmergency: () -> Unit
) {
    var remaining by remember { mutableLongStateOf(totalSec) }
    var futureMsg by remember { mutableStateOf("") }
    var showEmergencyConfirm by remember { mutableStateOf(false) }
    var emergencyPassword by remember { mutableStateOf("") }
    var emergencyPasswordError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        futureMsg = messageRepo.getRandomMessage()?.message ?: ""
    }

    LaunchedEffect(remaining) {
        if (remaining <= 0L) return@LaunchedEffect  // timer done — show button, don't auto-open
        delay(1000L)
        remaining -= 1L
    }

    val timerDone = remaining <= 0L
    val progress = if (totalSec > 0) remaining.toFloat() / totalSec.toFloat() else 0f

    Box(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Not Now", style = MaterialTheme.typography.labelLarge, color = AccentAmber)

            if (timerDone) {
                Text("Time's up.", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, textAlign = TextAlign.Center)
                Text("You waited. Do you still want to open $appName?", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
            } else {
                Text("Take a breath.", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, textAlign = TextAlign.Center)
                Text("You were about to open $appName", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(4.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (timerDone) 0f else progress },
                    modifier = Modifier.size(140.dp),
                    color = if (timerDone) AccentGreen else AccentAmber,
                    trackColor = BorderDark,
                    strokeWidth = 6.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (timerDone) {
                        Text("✓", fontSize = 42.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                    } else {
                        val mins = remaining / 60
                        val secs = remaining % 60
                        Text(
                            if (mins > 0L) "%d:%02d".format(mins, secs) else "$secs",
                            fontSize = 42.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (totalSec >= 60) Text("min", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        else Text("sec", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }

            if (!timerDone && futureMsg.isNotBlank()) {
                Surface(shape = RoundedCornerShape(12.dp), color = CardDark, modifier = Modifier.fillMaxWidth()) {
                    Text("\"$futureMsg\"", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                }
            }

            if (!timerDone) {
                Text("If you still want it after the timer, it's yours.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
            }

            if (timerDone) {
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open $appName", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onGoBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back Anyway", color = TextSecondary)
                }
            } else {
                Button(
                    onClick = onGoBack,
                    colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Go Back", color = TextPrimary)
                }

                if (!showEmergencyConfirm) {
                    TextButton(onClick = { showEmergencyConfirm = true }) {
                        Text("Emergency Unlock (15 min)", color = TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    Surface(shape = RoundedCornerShape(12.dp), color = CardDark) {
                        Column(
                            Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Emergency Unlock", color = AccentRed, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Type the password to unlock everything for 15 minutes.",
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = emergencyPassword,
                                onValueChange = { emergencyPassword = it; emergencyPasswordError = false },
                                placeholder = { Text("Password", color = TextSecondary) },
                                singleLine = true,
                                isError = emergencyPasswordError,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentRed,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            if (emergencyPasswordError) {
                                Text("Wrong password", color = AccentRed, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        showEmergencyConfirm = false
                                        emergencyPassword = ""
                                        emergencyPasswordError = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Cancel", color = TextSecondary) }
                                Button(
                                    onClick = {
                                        if (emergencyPassword == "Areyousure?") onEmergency()
                                        else emergencyPasswordError = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Unlock", color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NightBlockContent(appName: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🌙", fontSize = 64.sp)
            Text("Night Lockdown", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
            Text("$appName is locked until morning.\nRest well.", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = CardDark), modifier = Modifier.fillMaxWidth()) {
                Text("Go Back", color = TextPrimary)
            }
        }
    }
}

@Composable
fun ShoppingPauseContent(
    appName: String,
    delayMinutes: Long,
    onBuyNow: () -> Unit,
    onSaveForLater: (title: String, url: String, price: String) -> Unit,
    onGoBack: () -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var url   by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(DeepNavy), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🛍️", fontSize = 56.sp)
            Text("Spending Pause", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
            Text("You're about to open $appName.\nDo you need this right now?", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)

            if (!showForm) {
                Button(onClick = onBuyNow, colors = ButtonDefaults.buttonColors(containerColor = AccentAmber), modifier = Modifier.fillMaxWidth()) {
                    Text("Buy Now  (wait $delayMinutes min)", color = DeepNavy)
                }
                Button(onClick = { showForm = true }, colors = ButtonDefaults.buttonColors(containerColor = AccentGreen), modifier = Modifier.fillMaxWidth()) {
                    Text("Save for Later", color = Color.White)
                }
                OutlinedButton(onClick = onGoBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Go Back", color = TextSecondary)
                }
            } else {
                Surface(shape = RoundedCornerShape(16.dp), color = CardDark, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Save to Vault", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("What is it?") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentAmber, unfocusedBorderColor = BorderDark, focusedLabelColor = AccentAmber, unfocusedLabelColor = TextSecondary))
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentAmber, unfocusedBorderColor = BorderDark, focusedLabelColor = AccentAmber, unfocusedLabelColor = TextSecondary))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { showForm = false }, modifier = Modifier.weight(1f)) { Text("Cancel", color = TextSecondary) }
                            Button(onClick = { if (title.isNotBlank()) onSaveForLater(title, url, price) }, enabled = title.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber), modifier = Modifier.weight(1f)) { Text("Save", color = DeepNavy) }
                        }
                    }
                }
            }
        }
    }
}
