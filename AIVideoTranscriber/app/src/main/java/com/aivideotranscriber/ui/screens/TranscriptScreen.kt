package com.aivideotranscriber.ui.screens

import android.content.Context
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aivideotranscriber.export.SaveToDownloads
import com.aivideotranscriber.export.ShareTranscript
import com.aivideotranscriber.export.TranscriptExporter
import com.aivideotranscriber.ui.MainViewModel
import com.aivideotranscriber.ui.PipelineState
import com.aivideotranscriber.util.FillerWordCleaner
import com.aivideotranscriber.whisper.TranscriptSegment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptScreen(viewModel: MainViewModel, result: PipelineState.Done) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearDownloadedVideo() }
    }

    val displaySegments = remember(result.segments, viewModel.fillerCleanupEnabled) {
        result.segments.map { seg ->
            if (viewModel.fillerCleanupEnabled) seg.copy(text = FillerWordCleaner.clean(seg.text)) else seg
        }
    }
    val filteredSegments = remember(displaySegments, searchQuery) {
        if (searchQuery.isBlank()) displaySegments
        else displaySegments.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }
    val fullText = remember(displaySegments, viewModel.timestampsEnabled) {
        TranscriptExporter.toPlainText(displaySegments, viewModel.timestampsEnabled)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Transcript") },
            navigationIcon = {
                IconButton(onClick = { viewModel.reset(context) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "New transcription")
                }
            },
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                if (result.previewUri != null && viewModel.inputMode == com.aivideotranscriber.ui.InputMode.URL) {
                    "This device downloaded a temporary copy of your video only to show the preview below. " +
                        "It will be permanently deleted the moment you leave this screen."
                } else {
                    "Transcription ran entirely on this device. Nothing was ever uploaded to a server."
                },
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        if (result.previewUri != null) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setMediaController(MediaController(ctx))
                        setVideoURI(result.previewUri)
                        videoView = this
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).padding(horizontal = 16.dp),
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search transcript") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionButton(Icons.Filled.ContentCopy, "Copy") {
                clipboard.setText(AnnotatedString(fullText))
            }
            ActionButton(Icons.Filled.Download, "TXT") {
                saveExport(context, "transcript.txt", "text/plain", fullText)
            }
            ActionButton(Icons.Filled.Download, "SRT") {
                saveExport(context, "transcript.srt", "application/x-subrip", TranscriptExporter.toSrt(displaySegments))
            }
            ActionButton(Icons.Filled.Share, "Share") {
                ShareTranscript.shareText(context, fullText)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
            items(filteredSegments) { segment ->
                TranscriptRow(
                    segment = segment,
                    showTimestamp = viewModel.timestampsEnabled,
                    onTimestampClick = {
                        videoView?.let { vv ->
                            vv.seekTo(segment.startMs.toInt())
                            vv.start()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.padding(start = 6.dp), fontSize = 13.sp)
    }
}

@Composable
private fun TranscriptRow(segment: TranscriptSegment, showTimestamp: Boolean, onTimestampClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (showTimestamp) {
            Text(
                TranscriptExporter.formatClock(segment.startMs),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickableTimestamp(onTimestampClick),
            )
        }
        Text(segment.text, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

private fun Modifier.clickableTimestamp(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

private fun saveExport(context: Context, fileName: String, mimeType: String, content: String) {
    try {
        val location = SaveToDownloads.save(context, fileName, mimeType, content)
        android.widget.Toast.makeText(context, "Saved to $location", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        android.widget.Toast.makeText(
            context,
            "Couldn't save to Downloads on this Android version - use Share instead.",
            android.widget.Toast.LENGTH_LONG,
        ).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Couldn't save the file: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
