package com.notnow.app.ui.screen.overlay

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.data.entity.InteractionType
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.ui.theme.NotNowTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CountdownOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_DELAY_SECONDS = "delay_seconds"
        const val EXTRA_FRICTION_LEVEL = "friction_level"
        const val EXTRA_IS_NIGHT_LOCKDOWN = "is_night_lockdown"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        val delaySeconds = intent.getIntExtra(EXTRA_DELAY_SECONDS, 30)
        val frictionLevel = FrictionLevel.valueOf(
            intent.getStringExtra(EXTRA_FRICTION_LEVEL) ?: FrictionLevel.LEVEL_1_DISTRACTION.name
        )
        val isNightLockdown = intent.getBooleanExtra(EXTRA_IS_NIGHT_LOCKDOWN, false)

        val app = application as NotNowApplication
        val usageRepo = app.usageRecordRepository
        val messageRepo = app.futureMessageRepository
        val prefs = AppPreferences.getInstance(applicationContext)

        setContent {
            NotNowTheme {
                var secondsLeft by remember { mutableIntStateOf(delaySeconds) }
                var isComplete by remember { mutableStateOf(delaySeconds == 0) }
                var futureMessage by remember { mutableStateOf<String?>(null) }
                var showEmergencyDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    futureMessage = messageRepo.getRandomMessage()?.message
                }

                LaunchedEffect(Unit) {
                    if (delaySeconds > 0) {
                        object : CountDownTimer(delaySeconds * 1000L, 1000L) {
                            override fun onTick(millisUntilFinished: Long) {
                                secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                            }
                            override fun onFinish() {
                                isComplete = true
                                lifecycleScope.launch {
                                    usageRepo.record(packageName, appName, InteractionType.BLOCKED, delaySeconds)
                                }
                            }
                        }.start()
                    }
                }

                BackHandler {
                    lifecycleScope.launch {
                        usageRepo.record(packageName, appName, InteractionType.ABANDONED, delaySeconds)
                    }
                    finish()
                }

                if (showEmergencyDialog) {
                    EmergencyUnlockDialog(
                        onUnlock15 = {
                            lifecycleScope.launch {
                                val until = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)
                                prefs.setEmergencyUnlockUntil(until)
                                usageRepo.record(packageName, appName, InteractionType.BYPASSED, 0)
                                finish()
                            }
                        },
                        onUnlock30 = {
                            lifecycleScope.launch {
                                val until = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30)
                                prefs.setEmergencyUnlockUntil(until)
                                usageRepo.record(packageName, appName, InteractionType.BYPASSED, 0)
                                finish()
                            }
                        },
                        onDismiss = { showEmergencyDialog = false }
                    )
                }

                CountdownScreen(
                    appName = appName,
                    secondsLeft = secondsLeft,
                    totalSeconds = delaySeconds,
                    isComplete = isComplete,
                    isNightLockdown = isNightLockdown,
                    futureMessage = futureMessage,
                    onDone = { finish() },
                    onGoBack = {
                        lifecycleScope.launch {
                            usageRepo.record(packageName, appName, InteractionType.ABANDONED, delaySeconds)
                        }
                        finish()
                    },
                    onEmergencyUnlock = { showEmergencyDialog = true }
                )
            }
        }
    }
}

@Composable
private fun CountdownScreen(
    appName: String,
    secondsLeft: Int,
    totalSeconds: Int,
    isComplete: Boolean,
    isNightLockdown: Boolean,
    futureMessage: String?,
    onDone: () -> Unit,
    onGoBack: () -> Unit,
    onEmergencyUnlock: () -> Unit
) {
    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, easing = LinearEasing),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (isNightLockdown) "Night Lockdown" else "Not Now",
                color = Color(0xFF888888),
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )

            Text(
                text = appName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (!isComplete && totalSeconds > 0) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(140.dp),
                        color = Color(0xFFE85D04),
                        trackColor = Color(0xFF2A2A2A),
                        strokeWidth = 8.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(secondsLeft),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "remaining", color = Color(0xFF888888), fontSize = 12.sp)
                    }
                }

                Text(
                    text = "Pause. Is this intentional?",
                    color = Color(0xFF888888),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (isNightLockdown && totalSeconds == 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "11 PM – 7 AM", color = Color(0xFFE85D04), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Low-energy hours. Poor decisions happen here.\nCome back in the morning.",
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            futureMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "\"$msg\"",
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isComplete) {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE85D04))
                ) {
                    Text("Open $appName", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (!isNightLockdown) {
                OutlinedButton(
                    onClick = onGoBack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888))
                ) {
                    Text("Go Back")
                }
            }

            if (!isComplete) {
                TextButton(onClick = onEmergencyUnlock) {
                    Text("Emergency Unlock", color = Color(0xFF444444), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EmergencyUnlockDialog(
    onUnlock15: () -> Unit,
    onUnlock30: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Emergency Unlock", color = Color.White) },
        text = {
            Text(
                "This is for genuine emergencies only.\nRestrictions return automatically.",
                color = Color(0xFF888888)
            )
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onUnlock15,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE85D04))
                ) { Text("Unlock for 15 minutes") }
                OutlinedButton(
                    onClick = onUnlock30,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Unlock for 30 minutes") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = Color(0xFF888888))
                }
            }
        },
        dismissButton = {}
    )
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
