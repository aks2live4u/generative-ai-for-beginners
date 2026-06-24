package com.dosemate.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosemate.data.ReminderSoundPrefs
import com.dosemate.data.ThemeMode
import com.dosemate.data.ThemePrefs
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val themeMode by ThemePrefs.current
    var soundUri by remember { mutableStateOf(ReminderSoundPrefs.getSoundUri(context)) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        soundUri = uri?.toString()
        ReminderSoundPrefs.setSoundUri(context, soundUri)
    }

    GlassBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = LocalDoseMateColors.current.headerText)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalDoseMateColors.current.headerText
                    )
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Appearance", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Choose light or dark mode.", fontSize = 12.sp, color = LocalDoseMateColors.current.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { ThemePrefs.setThemeMode(context, ThemeMode.LIGHT) },
                            label = { Text("Light") }
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { ThemePrefs.setThemeMode(context, ThemeMode.DARK) },
                            label = { Text("Dark") }
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Reminder Sound", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (soundUri != null) "Custom ringtone selected" else "Using the app default sound",
                        fontSize = 12.sp,
                        color = LocalDoseMateColors.current.textSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Applies to all medicine reminders.",
                        fontSize = 12.sp,
                        color = LocalDoseMateColors.current.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                val current = soundUri?.let { Uri.parse(it) }
                                    ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
                            }
                            ringtonePickerLauncher.launch(intent)
                        }) {
                            Text("Choose Sound", fontSize = 13.sp)
                        }
                        if (soundUri != null) {
                            OutlinedButton(onClick = {
                                soundUri = null
                                ReminderSoundPrefs.setSoundUri(context, null)
                            }) {
                                Text("Reset to Default", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
