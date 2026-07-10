package com.aichiefofstaff.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aichiefofstaff.voice.VoiceState

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VoiceCaptureSheet(
    voiceState: VoiceState,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LaunchedEffect(Unit) { onStart() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(180.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (voiceState) {
                is VoiceState.Listening -> {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Listening",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Listening…", modifier = Modifier.padding(top = 12.dp))
                }
                is VoiceState.PartialResult -> {
                    Text(voiceState.text, style = MaterialTheme.typography.titleMedium)
                }
                is VoiceState.FinalResult -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Working on it…", modifier = Modifier.padding(start = 12.dp))
                    }
                }
                is VoiceState.Error -> {
                    Text(voiceState.message, color = MaterialTheme.colorScheme.error)
                }
                is VoiceState.Idle -> {
                    Text("Say something…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
