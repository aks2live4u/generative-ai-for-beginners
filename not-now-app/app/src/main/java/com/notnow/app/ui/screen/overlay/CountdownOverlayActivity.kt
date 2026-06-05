package com.notnow.app.ui.screen.overlay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.AccessOutcome
import com.notnow.app.ui.theme.*
import kotlinx.coroutines.*

class CountdownOverlayActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra("package_name") ?: run { finish(); return }
        val appName     = intent.getStringExtra("app_name") ?: packageName
        val delaySec    = intent.getLongExtra("delay_seconds", 30L)
        val isNight     = intent.getBooleanExtra("is_night_block", false)
        val app         = application as NotNowApplication

        setContent {
            NotNowTheme {
                if (isNight) {
                    NightBlockScreen(appName = appName, onBack = {
                        scope.launch {
                            app.usageRepository.record(packageName, appName, AccessOutcome.NIGHT_BLOCKED)
                        }
                        finish()
                    })
                } else {
                    CountdownScreen(
                        appName    = appName,
                        totalSec   = delaySec,
                        onComplete = {
                            scope.launch {
                                app.usageRepository.record(packageName, appName, AccessOutcome.WAITED, delaySec)
                                app.preferences.addProtectedTimeSeconds(delaySec)
                            }
                            finish()
                        },
                        onGoBack = {
                            scope.launch {
                                app.usageRepository.record(packageName, appName, AccessOutcome.WENT_BACK)
                            }
                            finish()
                        },
                        onEmergency = {
                            scope.launch {
                                // 15-minute emergency unlock
                                app.preferences.setEmergencyUnlockUntil(System.currentTimeMillis() + 15 * 60 * 1000L)
                                app.usageRepository.record(packageName, appName, AccessOutcome.EMERGENCY_UNLOCKED)
                            }
                            finish()
                        },
                        messageRepo = app.futureMessageRepository
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

@Composable
private fun CountdownScreen(
    appName: String,
    totalSec: Long,
    onComplete: () -> Unit,
    onGoBack: () -> Unit,
    onEmergency: () -> Unit,
    messageRepo: com.notnow.app.data.repository.FutureMessageRepository
) {
    var remaining by remember { mutableLongStateOf(totalSec) }
    var futureMsg by remember { mutableStateOf("") }
    var showEmergencyConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        futureMsg = messageRepo.getRandomMessage()?.message ?: ""
    }

    LaunchedEffect(remaining) {
        if (remaining <= 0) {
            onComplete()
            return@LaunchedEffect
        }
        delay(1000L)
        remaining--
    }

    val progress = if (totalSec > 0) remaining.toFloat() / totalSec.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy.copy(alpha = 0.97f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Not Now", style = MaterialTheme.typography.labelLarge, color = AccentAmber)

            Spacer(Modifier.height(8.dp))

            Text(
                "Take a breath.",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                "You were about to open $appName",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Circular progress countdown
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(140.dp),
                    color = AccentAmber,
                    trackColor = BorderDark,
                    strokeWidth = 6.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val mins = remaining / 60
                    val secs = remaining % 60
                    Text(
                        if (mins > 0) "%d:%02d".format(mins, secs) else "$secs",
                        fontSize = 42.sp,
                        color = TextPrimary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text("seconds", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            if (futureMsg.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "\"$futureMsg\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Text(
                "If you still want it after the timer, it's yours.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onGoBack,
                colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Go Back", color = TextPrimary)
            }

            if (!showEmergencyConfirm) {
                TextButton(onClick = { showEmergencyConfirm = true }) {
                    Text("Emergency Unlock (15 min)", color = TextSecondary, fontSize = 12.sp)
                }
            } else {
                Surface(shape = RoundedCornerShape(12.dp), color = CardDark) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Are you sure? This unlocks everything for 15 minutes.", color = TextSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { showEmergencyConfirm = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Button(
                                onClick = onEmergency,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                            ) {
                                Text("Unlock", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NightBlockScreen(appName: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy.copy(alpha = 0.97f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🌙", fontSize = 64.sp)
            Text("Night Lockdown", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
            Text("$appName is locked until 7:00 AM.\nRest well.", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go Back", color = TextPrimary)
            }
        }
    }
}
