package com.aks.gallerysorter.classify.ml

import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService

/**
 * Owns one model instance and confines it to [executor], which must be single-threaded: the instance is
 * created, smoke-tested, invoked and closed only on that thread (the LiteRT GPU delegate's OpenGL backend
 * requires this). It prefers the instance from [preferred] (GPU) and falls back to [fallback] (CPU) when
 * [preferred] returns null or throws, when its [smoke] run throws, or when its first failing [run] throws:
 * then the preferred instance is closed, a fallback one is built and the call is retried once. Failures on
 * the fallback instance propagate. Construction blocks until the instance is ready and throws if even the
 * fallback cannot be built or smoke-tested.
 */
internal class ResilientRunner<M : AutoCloseable>(
    private val executor: ExecutorService,
    private val preferred: () -> M?,
    private val fallback: () -> M,
    private val smoke: (M) -> Unit,
) : AutoCloseable {
    /** The current instance; null only if rebuilding on [fallback] failed (retried on the next [run]). */
    private var model: M?
    /** True while the [preferred] instance is in use. Written only on [executor]. */
    @Volatile var onPreferred: Boolean = false
        private set

    init {
        model = onExecutor {
            val p = try {
                preferred()?.also { candidate ->
                    try {
                        smoke(candidate)
                    } catch (t: Throwable) {
                        runCatching { candidate.close() }
                        throw t
                    }
                }
            } catch (t: Throwable) { // GPU failures surface as native/link/argument errors alike.
                null
            }
            if (p != null) {
                onPreferred = true
                p
            } else {
                fallback().also { smoke(it) }
            }
        }
    }

    /** Runs [block] with the model on [executor], blocking the caller until it finishes. */
    fun <R> run(block: (M) -> R): R = onExecutor {
        val current = model ?: fallback().also { model = it }
        try {
            block(current)
        } catch (t: Throwable) {
            if (!onPreferred) throw t
            runCatching { current.close() }
            onPreferred = false
            model = null
            block(fallback().also { model = it })
        }
    }

    override fun close() {
        onExecutor {
            model?.close()
            model = null
        }
    }

    private fun <R> onExecutor(task: () -> R): R = try {
        executor.submit<R> { task() }.get()
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }
}
