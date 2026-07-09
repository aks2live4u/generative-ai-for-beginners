package com.aivideotranscriber.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aivideotranscriber.ui.screens.HomeScreen
import com.aivideotranscriber.ui.screens.ProcessingScreen
import com.aivideotranscriber.ui.screens.TranscriptScreen
import com.aivideotranscriber.ui.theme.AIVideoTranscriberTheme
import com.aivideotranscriber.cleanup.CleanupManager

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Wipe any temp files left behind by a previous session that crashed or was killed
        // before it could clean up after itself.
        CleanupManager.clearTempFiles(applicationContext)

        setContent {
            AIVideoTranscriberTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(viewModel)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: MainViewModel) {
    val state = viewModel.pipelineState

    when (state) {
        is PipelineState.Idle,
        is PipelineState.Failed,
        -> HomeScreen(viewModel = viewModel, errorMessage = (state as? PipelineState.Failed)?.message)

        is PipelineState.Downloading,
        is PipelineState.ExtractingAudio,
        is PipelineState.LoadingModel,
        is PipelineState.Transcribing,
        -> ProcessingScreen(state = state)

        is PipelineState.Done -> TranscriptScreen(viewModel = viewModel, result = state)
    }
}
