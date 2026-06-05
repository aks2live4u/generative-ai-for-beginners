package com.notnow.app.ui.screen.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.ShoppingVaultItem
import com.notnow.app.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ShoppingOverlayActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra("package_name") ?: run { finish(); return }
        val appName     = intent.getStringExtra("app_name") ?: packageName
        val delaySec    = intent.getLongExtra("delay_seconds", 3600L)
        val app         = application as NotNowApplication

        setContent {
            NotNowTheme {
                ShoppingPauseScreen(
                    appName = appName,
                    delaySec = delaySec,
                    onBuyNow = {
                        // Route through standard countdown
                        val intent = android.content.Intent(this, CountdownOverlayActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            putExtra("package_name", packageName)
                            putExtra("app_name", appName)
                            putExtra("delay_seconds", delaySec)
                        }
                        startActivity(intent)
                        finish()
                    },
                    onSaveForLater = { title, url, price ->
                        scope.launch(Dispatchers.IO) {
                            app.shoppingVaultRepository.save(
                                ShoppingVaultItem(title = title, url = url, price = price)
                            )
                        }
                        finish()
                    },
                    onGoBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

@Composable
private fun ShoppingPauseScreen(
    appName: String,
    delaySec: Long,
    onBuyNow: () -> Unit,
    onSaveForLater: (title: String, url: String, price: String) -> Unit,
    onGoBack: () -> Unit
) {
    var showSaveForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var url   by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    val delayMins = delaySec / 60

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
            Text("🛍️", fontSize = 56.sp)
            Text("Spending Pause", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
            Text(
                "You're about to open $appName.\nDo you need this right now?",
                style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center
            )

            if (!showSaveForm) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onBuyNow,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buy Now  (wait ${delayMins} min)", color = DeepNavy)
                }
                Button(
                    onClick = { showSaveForm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save for Later", color = Color.White)
                }
                OutlinedButton(
                    onClick = onGoBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back", color = TextSecondary)
                }
            } else {
                Surface(shape = RoundedCornerShape(16.dp), color = CardDark, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Save to Vault", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("What is it?") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentAmber,
                                unfocusedBorderColor = BorderDark,
                                focusedLabelColor = AccentAmber,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentAmber,
                                unfocusedBorderColor = BorderDark,
                                focusedLabelColor = AccentAmber,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("Link (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentAmber,
                                unfocusedBorderColor = BorderDark,
                                focusedLabelColor = AccentAmber,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { showSaveForm = false }, modifier = Modifier.weight(1f)) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) onSaveForLater(title, url, price)
                                },
                                enabled = title.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save", color = DeepNavy)
                            }
                        }
                    }
                }
            }
        }
    }
}
