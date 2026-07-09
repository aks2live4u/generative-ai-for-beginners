package com.aivideotranscriber.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivideotranscriber.model.AccuracyTier
import com.aivideotranscriber.ui.MainViewModel
import com.aivideotranscriber.util.LanguageOptions

@Composable
fun HomeScreen(viewModel: MainViewModel, errorMessage: String?) {
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.pickedFileUri = uri
            viewModel.pickedFileName = queryDisplayName(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Text(
                "AI Video Transcriber",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Text(
            "Private, on-device transcription. No login, nothing uploaded to a server.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                filePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Filled.UploadFile, contentDescription = null)
            Text(
                viewModel.pickedFileName ?: "Choose Video",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            "Pick a video from your gallery or files. Stays on your phone — nothing is copied or uploaded anywhere.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(24.dp))
        Text("Language", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LanguageDropdown(
            selectedCode = viewModel.language,
            onSelected = { viewModel.language = it },
        )

        Spacer(Modifier.height(24.dp))
        Text("Accuracy", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
        AccuracyTier.entries.forEach { tier ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(selected = viewModel.accuracyTier == tier, onClick = { viewModel.accuracyTier = tier })
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("${tier.label} (~${tier.approxSizeMb} MB model)")
                    Text(
                        tier.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        ToggleRow(
            title = "Timestamps",
            subtitle = "Show a time for each line, tap to jump the preview",
            checked = viewModel.timestampsEnabled,
            onCheckedChange = { viewModel.timestampsEnabled = it },
        )
        ToggleRow(
            title = "Remove filler words",
            subtitle = "Heuristic cleanup of “um”, “uh”, “like”, repeated words (may occasionally remove intentional words too)",
            checked = viewModel.fillerCleanupEnabled,
            onCheckedChange = { viewModel.fillerCleanupEnabled = it },
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { viewModel.startTranscription(context) },
            enabled = viewModel.canStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Transcribe", fontSize = 18.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageDropdown(selectedCode: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = LanguageOptions.ALL.firstOrNull { it.code == selectedCode }?.displayName ?: "Auto Detect"

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Read-only text fields can still swallow taps for cursor placement, so a transparent
        // clickable overlay is used to reliably open the menu instead of relying on the field.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LanguageOptions.ALL.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onSelected(option.code)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
        }
    } catch (e: Exception) {
        null
    }
}
