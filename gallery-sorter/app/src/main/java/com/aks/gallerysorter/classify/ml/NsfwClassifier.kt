package com.aks.gallerysorter.classify.ml

import android.content.Context
import android.graphics.Bitmap

/**
 * Probabilities in 0..1 from the on-device NSFW model (spec 6.3, "Nudity/explicit").
 * [explicit] = porn + hentai, [suggestive] = sexy; the rest (neutral, drawings) is "safe".
 */
data class NsfwScore(val explicit: Float, val suggestive: Float)

interface NsfwClassifier {
    fun score(bmp: Bitmap): NsfwScore
}

/**
 * [NsfwClassifier] on `assets/models/nsfw.tflite`, a MobileNetV4-small classifier converted to TFLite
 * (see assets/models/LICENSES.md). Input: 1×224×224×3 float32 RGB in 0..1 (ImageNet normalisation is
 * inside the model). Output: 1×5 softmax over [LABELS]. Thread-safe; [score] blocks the calling thread
 * while inference runs on the model's own thread (see [LiteRtModel]), so call it off the main thread.
 * [close] releases the interpreter.
 */
class LiteRtNsfwClassifier private constructor(private val model: LiteRtModel) : NsfwClassifier, AutoCloseable {
    private val input = floatBuffer(SIZE * SIZE * 3)
    private val output = Array(1) { FloatArray(LABELS.size) }

    @Synchronized
    override fun score(bmp: Bitmap): NsfwScore {
        input.rewind()
        for (p in scaledPixels(bmp, SIZE)) {
            input.putFloat(((p shr 16) and 0xFF) / 255f)
            input.putFloat(((p shr 8) and 0xFF) / 255f)
            input.putFloat((p and 0xFF) / 255f)
        }
        model.run { interpreter ->
            input.rewind() // also on the CPU retry
            interpreter.run(input, output)
        }
        val probs = output[0]
        return NsfwScore(explicit = probs[PORN] + probs[HENTAI], suggestive = probs[SEXY])
    }

    @Synchronized
    override fun close() = model.close()

    companion object {
        const val ASSET = "models/nsfw.tflite"
        private const val SIZE = 224
        /** Output order of the model. */
        val LABELS = listOf("drawings", "hentai", "neutral", "porn", "sexy")
        private const val HENTAI = 1
        private const val PORN = 3
        private const val SEXY = 4

        /** Returns null (after logging) if the model asset is missing or cannot be loaded. */
        fun create(context: Context): LiteRtNsfwClassifier? =
            LiteRtModel.open(context, ASSET)?.let(::LiteRtNsfwClassifier)
    }
}
