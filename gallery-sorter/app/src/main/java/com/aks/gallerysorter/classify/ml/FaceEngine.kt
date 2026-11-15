package com.aks.gallerysorter.classify.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.aks.gallerysorter.classify.l2Normalize
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.nio.ByteBuffer

/** One detected face: its box in the source bitmap's pixels and an L2-normalised embedding. */
class FaceHit(val box: RectF, val embedding: FloatArray /* L2-normalised */)

interface FaceEngine {
    suspend fun faces(bmp: Bitmap): List<FaceHit>
}

/**
 * ML Kit face detection (bundled) → crop each face box → 112×112 → `assets/models/face_embed.tflite`
 * (MobileFaceNet, 128-d). The model has two image inputs (img1, img2: 1×3×112×112 float32 RGB in 0..1,
 * channels first) and outputs 2×128, one row per input, computed independently; faces are embedded two
 * per call and an odd last face is paired with itself.
 */
class MlKitFaceEngine private constructor(
    private val detector: FaceDetector,
    private val model: LiteRtModel,
) : FaceEngine, AutoCloseable {
    private val in1 = floatBuffer(PLANE * 3)
    private val in2 = floatBuffer(PLANE * 3)
    private val out = Array(2) { FloatArray(EMBEDDING_DIM) }

    override suspend fun faces(bmp: Bitmap): List<FaceHit> {
        val boxes = detector.process(InputImage.fromBitmap(bmp, 0)).await()
            .mapNotNull { clamp(it.boundingBox, bmp.width, bmp.height) }
        val hits = ArrayList<FaceHit>(boxes.size)
        for (i in boxes.indices step 2) {
            val a = boxes[i]
            val b = boxes.getOrElse(i + 1) { a }
            val (ea, eb) = embedPair(crop(bmp, a), crop(bmp, b))
            hits += FaceHit(RectF(a), ea)
            if (i + 1 < boxes.size) hits += FaceHit(RectF(b), eb)
        }
        return hits
    }

    @Synchronized
    private fun embedPair(a: IntArray, b: IntArray): Pair<FloatArray, FloatArray> {
        fill(in1, a)
        fill(in2, b)
        model.interpreter.runForMultipleInputsOutputs(arrayOf<Any>(in1, in2), mapOf<Int, Any>(0 to out))
        return l2Normalize(out[0]) to l2Normalize(out[1])
    }

    @Synchronized
    override fun close() {
        detector.close()
        model.close()
    }

    companion object {
        const val ASSET = "models/face_embed.tflite"
        const val EMBEDDING_DIM = 128
        private const val SIZE = 112
        private const val PLANE = SIZE * SIZE

        /** Returns null (after logging) if the embedding model asset is missing or cannot be loaded. */
        fun create(context: Context): MlKitFaceEngine? {
            val model = LiteRtModel.open(context, ASSET) ?: return null
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
            return MlKitFaceEngine(FaceDetection.getClient(options), model)
        }

        private fun clamp(r: Rect, w: Int, h: Int): Rect? {
            val c = Rect(r.left.coerceAtLeast(0), r.top.coerceAtLeast(0), r.right.coerceAtMost(w), r.bottom.coerceAtMost(h))
            return if (c.width() > 0 && c.height() > 0) c else null
        }

        private fun crop(bmp: Bitmap, r: Rect): IntArray {
            val face = Bitmap.createBitmap(bmp, r.left, r.top, r.width(), r.height())
            val px = scaledPixels(face, SIZE)
            if (face !== bmp) face.recycle()
            return px
        }

        /** Writes ARGB pixels as channels-first (R plane, G plane, B plane) floats in 0..1. */
        private fun fill(buf: ByteBuffer, px: IntArray) {
            buf.rewind()
            for (shift in intArrayOf(16, 8, 0)) for (p in px) buf.putFloat(((p shr shift) and 0xFF) / 255f)
            buf.rewind()
        }
    }
}
