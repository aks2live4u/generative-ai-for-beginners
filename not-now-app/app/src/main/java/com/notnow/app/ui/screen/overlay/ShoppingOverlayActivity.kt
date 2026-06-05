package com.notnow.app.ui.screen.overlay

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.ShoppingVaultItem
import com.notnow.app.ui.theme.NotNowTheme
import kotlinx.coroutines.launch

class ShoppingOverlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val packageName = intent.getStringExtra(CountdownOverlayActivity.EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(CountdownOverlayActivity.EXTRA_APP_NAME) ?: "Shopping"
        val delaySeconds = intent.getIntExtra(CountdownOverlayActivity.EXTRA_DELAY_SECONDS, 3600)

        val app = application as NotNowApplication
        val vaultRepo = app.shoppingVaultRepository

        setContent {
            NotNowTheme {
                var showSaveForm by remember { mutableStateOf(false) }
                var title by remember { mutableStateOf("") }
                var price by remember { mutableStateOf("") }
                var url by remember { mutableStateOf("") }

                BackHandler { finish() }

                if (showSaveForm) {
                    SaveItemForm(
                        appName = appName,
                        title = title,
                        price = price,
                        url = url,
                        onTitleChange = { title = it },
                        onPriceChange = { price = it },
                        onUrlChange = { url = it },
                        onSave = {
                            if (title.isNotBlank()) {
                                lifecycleScope.launch {
                                    vaultRepo.save(
                                        ShoppingVaultItem(
                                            title = title.trim(),
                                            price = price.trim(),
                                            url = url.trim(),
                                            sourceApp = appName
                                        )
                                    )
                                    finish()
                                }
                            }
                        },
                        onCancel = { finish() }
                    )
                } else {
                    ShoppingChoiceScreen(
                        appName = appName,
                        delaySeconds = delaySeconds,
                        onBuyNow = {
                            val intent = Intent(this, CountdownOverlayActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra(CountdownOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
                                putExtra(CountdownOverlayActivity.EXTRA_APP_NAME, appName)
                                putExtra(CountdownOverlayActivity.EXTRA_DELAY_SECONDS, delaySeconds)
                                putExtra(CountdownOverlayActivity.EXTRA_FRICTION_LEVEL, "LEVEL_3_SPENDING")
                            }
                            startActivity(intent)
                            finish()
                        },
                        onSaveForLater = { showSaveForm = true },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShoppingChoiceScreen(
    appName: String,
    delaySeconds: Int,
    onBuyNow: () -> Unit,
    onSaveForLater: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF111111))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Shopping Guardrail", color = Color(0xFF888888), fontSize = 12.sp, letterSpacing = 2.sp)
            Text(text = appName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Impulse purchases happen here.\nWhat do you want to do?",
                color = Color(0xFF888888),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onSaveForLater,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Save For Later", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Add to vault, decide tomorrow", color = Color(0xFF6699CC), fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = onBuyNow,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Buy Now", color = Color(0xFFCCCCCC), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Text(
                        text = "Wait ${formatDelay(delaySeconds)} first",
                        color = Color(0xFF666666),
                        fontSize = 12.sp
                    )
                }
            }

            TextButton(onClick = onCancel) {
                Text("Go Back", color = Color(0xFF444444), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SaveItemForm(
    appName: String,
    title: String,
    price: String,
    url: String,
    onTitleChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF111111))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Save to Vault", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("You can decide tomorrow whether you still want it.", color = Color(0xFF888888), fontSize = 13.sp)

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("What is it?") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE85D04),
                    focusedLabelColor = Color(0xFFE85D04),
                    unfocusedBorderColor = Color(0xFF333333),
                    unfocusedLabelColor = Color(0xFF888888),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFE85D04)
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true
            )

            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                label = { Text("Price (optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE85D04),
                    focusedLabelColor = Color(0xFFE85D04),
                    unfocusedBorderColor = Color(0xFF333333),
                    unfocusedLabelColor = Color(0xFF888888),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFE85D04)
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("Link (optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE85D04),
                    focusedLabelColor = Color(0xFFE85D04),
                    unfocusedBorderColor = Color(0xFF333333),
                    unfocusedLabelColor = Color(0xFF888888),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFE85D04)
                ),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE85D04))
                ) {
                    Text("Save It")
                }
            }
        }
    }
}

private fun formatDelay(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes >= 60) "${minutes / 60}h" else "${minutes}m"
}
