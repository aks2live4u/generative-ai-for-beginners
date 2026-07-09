package com.aivideotranscriber.whisper

import android.os.Build
import android.util.Log
import java.io.File

/** Called from native code on the transcription thread with a 0-100 progress value. */
fun interface WhisperProgressListener {
    fun onProgress(percent: Int)
}

/**
 * Raw JNI surface over whisper.cpp (https://github.com/ggerganov/whisper.cpp, MIT licensed).
 * Prefer [WhisperContext] for everyday use - it wraps this with coroutines and lifecycle safety.
 */
internal object WhisperNative {
    private const val TAG = "WhisperNative"

    init {
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        val cpuInfo = if (primaryAbi == "armeabi-v7a" || primaryAbi == "arm64-v8a") readCpuInfo() else null
        val libraryName = when {
            primaryAbi == "arm64-v8a" && cpuInfo?.contains("fphp") == true -> "whisper_v8fp16_va"
            primaryAbi == "armeabi-v7a" && cpuInfo?.contains("vfpv4") == true -> "whisper_vfpv4"
            else -> "whisper"
        }
        try {
            System.loadLibrary(libraryName)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Falling back to generic libwhisper.so (couldn't load $libraryName)", e)
            System.loadLibrary("whisper")
        }
    }

    private fun readCpuInfo(): String? = try {
        File("/proc/cpuinfo").readText()
    } catch (e: Exception) {
        null
    }

    external fun initContext(modelPath: String): Long

    external fun freeContext(contextPtr: Long)

    external fun fullTranscribe(
        contextPtr: Long,
        numThreads: Int,
        audioData: FloatArray,
        language: String,
        listener: WhisperProgressListener?,
    )

    external fun getTextSegmentCount(contextPtr: Long): Int

    external fun getTextSegment(contextPtr: Long, index: Int): String

    external fun getTextSegmentT0(contextPtr: Long, index: Int): Long

    external fun getTextSegmentT1(contextPtr: Long, index: Int): Long

    external fun getSystemInfo(): String
}
