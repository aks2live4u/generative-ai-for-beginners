package com.finsight.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.finsight.ui.components.PrimaryButton
import com.finsight.ui.theme.financeColors
import kotlinx.coroutines.launch

data class SettingsUiState(
    val aiFeaturesEnabled: Boolean,
    val hasApiKey: Boolean,
    val lastCrashLog: String? = null
)

/**
 * Lets the user opt into Gemini-powered chat/insights/smart-scan by pasting their own API key.
 * Everything here defaults to off - the rule-based engine keeps working with zero network calls
 * until the user explicitly enables this and reads the disclosure below.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onToggleAiFeatures: (Boolean) -> Unit,
    onTestConnection: suspend () -> Result<String>,
    onCopyCrashLog: (String) -> Unit = {},
    onClearCrashLog: () -> Unit = {}
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var testState by remember { mutableStateOf<TestState>(TestState.Idle) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "AI Assistant (Gemini)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                    Text(
                        text = "Optional. When enabled, your chat questions, financial-health " +
                            "factors, and a summarized version of your transactions (merchant, " +
                            "amount, date, category - never your raw SMS/email text, account " +
                            "numbers, or phone numbers) are sent to Google's Gemini API using the " +
                            "key below to generate richer answers. The rule-based assistant keeps " +
                            "working fully offline if you leave this off, or if a Gemini call ever " +
                            "fails.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable AI features",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Switch(
                            checked = state.aiFeaturesEnabled,
                            onCheckedChange = onToggleAiFeatures,
                            enabled = state.hasApiKey
                        )
                    }
                    if (!state.hasApiKey) {
                        Text(
                            text = "Add an API key below to enable this.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.hasApiKey) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.financeColors.income)
                            Text(
                                text = "API key saved",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(text = "Remove API Key", onClick = { onClearApiKey(); testState = TestState.Idle })
                    } else {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Gemini API key") },
                            singleLine = true,
                            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { keyVisible = !keyVisible }) {
                                    Icon(
                                        imageVector = if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (keyVisible) "Hide key" else "Show key"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(
                            text = "Save API Key",
                            enabled = apiKeyInput.isNotBlank(),
                            onClick = { onSaveApiKey(apiKeyInput.trim()); apiKeyInput = "" }
                        )
                    }

                    if (state.hasApiKey) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.TextButton(onClick = {
                                testState = TestState.Loading
                                scope.launch {
                                    val result = onTestConnection()
                                    testState = result.fold(
                                        onSuccess = { TestState.Success(it) },
                                        onFailure = { TestState.Failed(it.message ?: "Connection failed") }
                                    )
                                }
                            }) {
                                Text("Test Connection")
                            }
                            if (testState is TestState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            }
                        }
                        when (val s = testState) {
                            is TestState.Success -> StatusRow(icon = Icons.Filled.CheckCircle, tint = MaterialTheme.financeColors.income, text = s.reply)
                            is TestState.Failed -> StatusRow(icon = Icons.Filled.ErrorOutline, tint = MaterialTheme.financeColors.expense, text = s.reason)
                            else -> {}
                        }
                    }
                }
            }

            if (state.lastCrashLog != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.financeColors.expense)
                            Text(
                                text = "App crashed last time it closed unexpectedly",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                        Text(
                            text = state.lastCrashLog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            androidx.compose.material3.TextButton(onClick = { onCopyCrashLog(state.lastCrashLog) }) {
                                Text("Copy to clipboard")
                            }
                            androidx.compose.material3.TextButton(onClick = onClearCrashLog) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class TestState {
    object Idle : TestState()
    object Loading : TestState()
    data class Success(val reply: String) : TestState()
    data class Failed(val reason: String) : TestState()
}

@Composable
private fun StatusRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, text: String) {
    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
