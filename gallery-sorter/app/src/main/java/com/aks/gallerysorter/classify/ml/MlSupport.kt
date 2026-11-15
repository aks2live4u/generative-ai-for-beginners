package com.aks.gallerysorter.classify.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
internal class LiteRtInstance(val interpreter: Interpreter, private val gpu: GpuDelegate?) : AutoCloseable {
    override fun close() {
        interpreter.close()
        gpu?.close()
    }
}

/**
 * A LiteRT model confined to its own single thread: the interpreter (and GPU delegate) is created, invoked
 * and closed only there, as the GPU delegate's OpenGL backend requires. It prefers the GPU delegate and
 * falls back to the CPU if the delegate cannot be built, fails a smoke run at load time, or fails on a
 * later inference (that call is retried once on the CPU). [run] blocks the caller until inference is done.
 */
internal class LiteRtModel private constructor(
    private val executor: ExecutorService,
    private val runner: ResilientRunner<LiteRtInstance>,
) : AutoCloseable {
    val usesGpu: Boolean get() = runner.onPreferred

    fun <R> run(block: (Interpreter) -> R): R = runner.run { block(it.interpreter) }

    override fun close() {
        try {
            runner.close()
        } finally {
            executor.shutdown()
        }
    }

    companion object {
        /**
         * Memory-maps `assets/[assetPath]` (stored uncompressed, see `noCompress` in the build) and loads it
         * as described on [LiteRtModel]. Returns null, after logging, if the asset is missing or the model
         * cannot be loaded and smoke-tested even on the CPU.
         */
        fun open(context: Context, assetPath: String): LiteRtModel? {
            val buffer = try {
                context.assets.openFd(assetPath).use { fd ->
                    FileInputStream(fd.fileDescriptor).channel.use { ch ->
                        ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                    }
                }
            } catch (e: Exception) {
                Log.w(ML_TAG, "model asset $assetPath unavailable", e)
                return null
            }
            val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "litert-$assetPath").apply { isDaemon = true } }
            return try {
                val runner = ResilientRunner(
                    executor,
                    preferred = { gpuInstance(buffer, assetPath) },
                    fallback = {
                        Log.i(ML_TAG, "running $assetPath on the CPU")
                        LiteRtInstance(Interpreter(buffer, Interpreter.Options().setNumThreads(CPU_THREADS)), null)
                    },
                    smoke = { smokeRun(it.interpreter) },
                )
                LiteRtModel(executor, runner)
            } catch (t: Throwable) {
                Log.w(ML_TAG, "cannot load model $assetPath", t)
                executor.shutdown()
                null
            }
        }

        private fun gpuInstance(buffer: ByteBuffer, assetPath: String): LiteRtInstance? {
            val compat = CompatibilityList()
            try {
                if (!compat.isDelegateSupportedOnThisDevice) return null
                val gpu = GpuDelegate(compat.bestOptionsForThisDevice)
                return try {
                    LiteRtInstance(Interpreter(buffer, Interpreter.Options().addDelegate(gpu)), gpu)
                } catch (t: Throwable) {
                    Log.i(ML_TAG, "GPU delegate failed for $assetPath", t)
                    gpu.close()
                    throw t
                }
            } finally {
                compat.close()
            }
        }

        /** One inference on zeroed inputs, so a delegate that builds but cannot execute is caught at load. */
        private fun smokeRun(interpreter: Interpreter) {
            val inputs = Array<Any>(interpreter.inputTensorCount) { floatBuffer(interpreter.getInputTensor(it).numBytes() / 4) }
            val outputs = HashMap<Int, Any>()
            for (i in 0 until interpreter.outputTensorCount) outputs[i] = floatBuffer(interpreter.getOutputTensor(i).numBytes() / 4)
            interpreter.runForMultipleInputsOutputs(inputs, outputs)
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
