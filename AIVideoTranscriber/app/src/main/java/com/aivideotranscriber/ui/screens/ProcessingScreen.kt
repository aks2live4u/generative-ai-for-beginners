package com.aivideotranscriber.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivideotranscriber.ui.PipelineState

private data class Stage(val title: String, val subtitle: String, val percent: Int?)

@Composable
fun ProcessingScreen(state: PipelineState) {
    val stage = when (state) {
        is PipelineState.ExtractingAudio -> Stage("Extracting audio…", "Converting to mono 16 kHz for the best accuracy", state.percent)
        is PipelineState.LoadingModel -> Stage("Preparing the AI model…", "Downloaded once, then reused on-device", state.percent)
        is PipelineState.Transcribing -> Stage("Running AI transcription…", "Entirely on this device — nothing leaves your phone", state.percent)
        else -> Stage("Working…", "", null)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stage.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            stage.subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
        )

        if (stage.percent != null) {
            LinearProgressIndicator(
                progress = stage.percent / 100f,
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
            Text(
                "${stage.percent}%",
                modifier = Modifier.padding(top = 12.dp),
                fontWeight = FontWeight.Medium,
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(10.dp))
        }
    }
}
