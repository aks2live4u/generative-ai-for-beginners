package com.aichiefofstaff.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aichiefofstaff.data.prefs.ThemeMode
import com.aichiefofstaff.ui.util.LocalAppContainer

@Composable
fun SettingsScreen() {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory {
        initializer { SettingsViewModel(container) }
    })
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    var apiKeyDraft by remember { mutableStateOf("") }
    var showWipeConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = { Text(mode.name) }
                )
            }
        }

        Text("OpenAI API key", style = MaterialTheme.typography.titleMedium)
        Text(
            if (hasApiKey) "A key is saved, encrypted on this device." else "No key saved yet. AI features are disabled until you add one.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = apiKeyDraft,
            onValueChange = { apiKeyDraft = it },
            placeholder = { Text("sk-…") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                viewModel.saveApiKey(apiKeyDraft)
                apiKeyDraft = ""
            }) { Text("Save key") }
            OutlinedButton(onClick = { viewModel.clearApiKey() }) { Text("Remove key") }
        }

        Text("Data", style = MaterialTheme.typography.titleMedium)
        Text(
            "Everything lives only on this device. No account, no cloud sync.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = { showWipeConfirm = true }) { Text("Delete all data") }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Delete all data?") },
            text = { Text("This permanently removes every task, project, conversation, and your saved API key. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllData { showWipeConfirm = false }
                }) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
