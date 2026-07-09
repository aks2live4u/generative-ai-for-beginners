package com.aivideotranscriber.model

/**
 * Maps the app's "Fast / Best / Ultra" accuracy picker onto real on-device Whisper model sizes.
 * These are multilingual ggml models published by the whisper.cpp project on Hugging Face.
 * Note: this is on-device inference, not the cloud "Whisper Large" - "Ultra" tops out at the
 * `small` model (~466 MB) because anything bigger is impractical to run on a phone CPU.
 */
enum class AccuracyTier(
    val label: String,
    val description: String,
    val modelFileName: String,
    val approxSizeMb: Int,
    val downloadUrl: String,
) {
    FAST(
        label = "Fast",
        description = "Quickest, good for clear speech",
        modelFileName = "ggml-tiny.bin",
        approxSizeMb = 75,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
    ),
    BEST(
        label = "Best",
        description = "Recommended balance of speed and accuracy",
        modelFileName = "ggml-base.bin",
        approxSizeMb = 142,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
    ),
    ULTRA(
        label = "Ultra",
        description = "Highest on-device accuracy, slowest, largest download",
        modelFileName = "ggml-small.bin",
        approxSizeMb = 466,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
    ),
}
