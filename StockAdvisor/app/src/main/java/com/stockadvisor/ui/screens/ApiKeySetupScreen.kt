package com.stockadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stockadvisor.R
import com.stockadvisor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySetupScreen(
    isUpdate: Boolean = false,
    currentKey: String = "",
    onKeySaved: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var apiKey by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    fun save() {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank() || !trimmed.startsWith("sk-ant-")) {
            showError = true
        } else {
            onKeySaved(trimmed)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isUpdate) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { onBack?.invoke() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = OnSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
                )
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isUpdate) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_indistock_logo),
                        contentDescription = "IndiStock Advisor",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text(
                    text = if (isUpdate) "Update API Key" else "Welcome to IndiStock",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isUpdate)
                        "Enter your new Anthropic API key below."
                    else
                        "To get started, enter your Anthropic API key.\nGet one free at console.anthropic.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Anthropic API Key") },
                    placeholder = { Text("sk-ant-...", color = OnSurfaceVariant) },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key",
                                tint = OnSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Key must start with sk-ant-", color = RedAccent) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = GoldAccent,
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = OnSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your key is stored only on this device and never shared.",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = OnPrimary
                    )
                ) {
                    Text(
                        text = if (isUpdate) "Update Key" else "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
