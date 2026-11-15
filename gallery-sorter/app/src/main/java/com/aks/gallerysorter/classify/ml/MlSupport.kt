package com.aks.gallerysorter.classify.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

internal const val ML_TAG = "GallerySorterMl"

/** Awaits an ML Kit / Play services [Task] without pulling in kotlinx-coroutines-play-services. */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

/** A LiteRT interpreter plus the GPU delegate it owns, if any. */
internal class LiteRtModel(val interpreter: Interpreter, private val gpu: GpuDelegate?) : AutoCloseable {
    val usesGpu: Boolean get() = gpu != null

    override fun close() {
        interpreter.close()
        gpu?.close()
    }

    companion object {
        /**
         * Memory-maps `assets/[assetPath]` (stored uncompressed, see `noCompress` in the build) and builds
         * an interpreter, on the GPU delegate when this device supports it, otherwise on the CPU.
         * Returns null, after logging, if the asset is missing or cannot be loaded.
         */
        fun open(context: Context, assetPath: String): LiteRtModel? {
            val model = try {
                context.assets.openFd(assetPath).use { fd ->
                    FileInputStream(fd.fileDescriptor).channel.use { ch ->
                        ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                    }
                }
            } catch (e: Exception) {
                Log.w(ML_TAG, "model asset $assetPath unavailable", e)
                return null
            }
            var gpu: GpuDelegate? = null
            try {
                val compat = CompatibilityList()
                try {
                    if (compat.isDelegateSupportedOnThisDevice) {
                        gpu = GpuDelegate(compat.bestOptionsForThisDevice)
                        return LiteRtModel(Interpreter(model, Interpreter.Options().addDelegate(gpu)), gpu)
                    }
                } finally {
                    compat.close()
                }
            } catch (t: Throwable) { // GPU failures surface as native/link/argument errors alike.
                Log.i(ML_TAG, "GPU delegate unavailable for $assetPath, using CPU", t)
                gpu?.close()
            }
            return try {
                LiteRtModel(Interpreter(model, Interpreter.Options().setNumThreads(CPU_THREADS)), null)
            } catch (e: Exception) {
                Log.w(ML_TAG, "cannot load model $assetPath", e)
                null
            }
        }

        private const val CPU_THREADS = 4
    }
}

/** Allocates a native-order direct buffer for [floats] float32 values. */
internal fun floatBuffer(floats: Int): ByteBuffer =
    ByteBuffer.allocateDirect(floats * 4).order(ByteOrder.nativeOrder())

/** Scales [bmp] (a software bitmap, not `Bitmap.Config.HARDWARE`) to [size]×[size]; returns ARGB pixels, row-major. */
internal fun scaledPixels(bmp: Bitmap, size: Int): IntArray {
    val scaled = if (bmp.width == size && bmp.height == size) bmp else Bitmap.createScaledBitmap(bmp, size, size, true)
    val px = IntArray(size * size)
    scaled.getPixels(px, 0, size, 0, 0, size, size)
    if (scaled !== bmp) scaled.recycle()
    return px
}
