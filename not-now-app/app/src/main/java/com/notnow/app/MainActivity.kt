package com.notnow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.notnow.app.receiver.BootReceiver
import com.notnow.app.ui.navigation.AppNavigation
import com.notnow.app.ui.navigation.Route
import com.notnow.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Flipped to true in onStop (real background, not rotation).
    // Compose reads this directly — recompose happens automatically.
    private var isLocked by mutableStateOf(false)

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) isLocked = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NotNowApplication

        lifecycleScope.launch {
            val isFirst = app.preferences.isFirstLaunch.first()

            if (isFirst) {
                app.appRuleRepository.seedDefaults()
                app.preferences.setFirstLaunchDone()
                BootReceiver.scheduleWorkers(applicationContext)
            }

            val startDest = if (isFirst) Route.Setup.path else Route.Home.path

            setContent {
                NotNowTheme {
                    if (isLocked) {
                        AppLockScreen(onUnlock = { isLocked = false })
                    } else {
                        val navController = rememberNavController()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DeepNavy)
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            AppNavigation(
                                navController = navController,
                                startDestination = startDest
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLockScreen(onUnlock: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Not Now", style = MaterialTheme.typography.headlineLarge, color = AccentAmber)
            Text(
                "Enter password to open",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = false },
                placeholder = { Text("Password", color = TextSecondary) },
                singleLine = true,
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentAmber,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            if (error) {
                Text("Wrong password", color = AccentRed, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (password == "Areyousure?") onUnlock() else error = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Unlock", color = DeepNavy, fontWeight = FontWeight.Bold)
            }
        }
    }
}
