package com.aivideotranscriber.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivideotranscriber.model.AccuracyTier
import com.aivideotranscriber.ui.InputMode
import com.aivideotranscriber.ui.MainViewModel
import com.aivideotranscriber.util.LanguageOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, errorMessage: String?) {
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = viewModel.inputMode == InputMode.FILE,
                onClick = { viewModel.inputMode = InputMode.FILE },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Filled.UploadFile, contentDescription = null) },
            ) { Text("Upload Video") }
            SegmentedButton(
                selected = viewModel.inputMode == InputMode.URL,
                onClick = { viewModel.inputMode = InputMode.URL },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Filled.Link, contentDescription = null) },
            ) { Text("Video Link") }
        }

        Spacer(Modifier.height(16.dp))

        if (viewModel.inputMode == InputMode.FILE) {
            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("video/*")) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Text(
                    viewModel.pickedFileName ?: "Choose Video",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                "Supported: MP4, MOV, MKV, AVI, WEBM, M4V, FLV, 3GP, MPEG. Stays on your phone — nothing is copied anywhere.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            OutlinedTextField(
                value = viewModel.urlText,
                onValueChange = { viewModel.urlText = it },
                label = { Text("Paste a direct video link (https://...)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Direct downloadable links only (.mp4, direct Drive/Dropbox links, etc). YouTube, Instagram, TikTok and similar platforms aren't supported — downloading from them generally breaks their Terms of Service.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(selectedCode: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = LanguageOptions.ALL.firstOrNull { it.code == selectedCode }?.displayName ?: "Auto Detect"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
