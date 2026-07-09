package com.aivideotranscriber.whisper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/** A single transcribed utterance, timestamps in milliseconds from the start of the audio. */
data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/**
 * Coroutine-friendly wrapper around a loaded whisper.cpp model. All native calls for a given
 * instance are serialized onto one dedicated thread, matching whisper.cpp's requirement that a
 * context not be used concurrently from multiple threads.
 */
class WhisperContext private constructor(private var contextPtr: Long) {

    private val dispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "WhisperInference")
    }.asCoroutineDispatcher()

    suspend fun transcribe(
        audioData: FloatArray,
        language: String,
        onProgress: (Int) -> Unit = {},
    ): List<TranscriptSegment> = withContext(dispatcher) {
        check(contextPtr != 0L) { "This WhisperContext has already been released" }

        val numThreads = WhisperCpuConfig.preferredThreadCount()
        WhisperNative.fullTranscribe(contextPtr, numThreads, audioData, language) { percent ->
            onProgress(percent)
        }

        val segmentCount = WhisperNative.getTextSegmentCount(contextPtr)
        (0 until segmentCount).map { index ->
            TranscriptSegment(
                startMs = WhisperNative.getTextSegmentT0(contextPtr, index) * 10L,
                endMs = WhisperNative.getTextSegmentT1(contextPtr, index) * 10L,
                text = WhisperNative.getTextSegment(contextPtr, index).trim(),
            )
        }
    }

    suspend fun release() = withContext(dispatcher) {
        if (contextPtr != 0L) {
            WhisperNative.freeContext(contextPtr)
            contextPtr = 0L
        }
    }

    companion object {
        suspend fun load(modelPath: String): WhisperContext = withContext(Dispatchers.IO) {
            val ptr = WhisperNative.initContext(modelPath)
            check(ptr != 0L) { "Failed to load the Whisper model at $modelPath. It may be corrupt - try re-downloading it." }
            WhisperContext(ptr)
        }

        suspend fun systemInfo(): String = withContext(Dispatchers.Default) {
            WhisperNative.getSystemInfo()
        }
    }
}
