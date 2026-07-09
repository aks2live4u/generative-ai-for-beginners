package com.aivideotranscriber.ui

import android.net.Uri
import com.aivideotranscriber.whisper.TranscriptSegment

enum class InputMode { FILE, URL }

sealed class PipelineState {
    object Idle : PipelineState()
    data class Downloading(val percent: Int) : PipelineState()
    data class ExtractingAudio(val percent: Int) : PipelineState()
    data class LoadingModel(val percent: Int) : PipelineState()
    data class Transcribing(val percent: Int) : PipelineState()
    data class Done(val segments: List<TranscriptSegment>, val previewUri: Uri?) : PipelineState()
    data class Failed(val message: String, val canRetry: Boolean = true) : PipelineState()
}
