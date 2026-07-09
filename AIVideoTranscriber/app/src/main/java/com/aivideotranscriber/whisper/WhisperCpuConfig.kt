package com.aivideotranscriber.whisper

internal object WhisperCpuConfig {
    /** Leaves one core free for the UI/system so the app stays responsive during inference. */
    fun preferredThreadCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return (cores - 1).coerceAtLeast(2)
    }
}
