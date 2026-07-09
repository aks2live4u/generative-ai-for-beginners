package com.aivideotranscriber.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class ModelDownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Downloads and caches ggml Whisper models in the app's private storage. Nothing is bundled
 *  into the APK so the app install stays small - models are fetched on first use and kept for
 *  reuse (they are not "uploaded media", so they are exempt from the auto-delete policy). */
class ModelManager(context: Context) {

    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun modelFile(tier: AccuracyTier): File = File(modelsDir, tier.modelFileName)

    fun isDownloaded(tier: AccuracyTier): Boolean = modelFile(tier).exists() && modelFile(tier).length() > 0

    suspend fun ensureDownloaded(tier: AccuracyTier, onProgress: (Int) -> Unit = {}): File =
        withContext(Dispatchers.IO) {
            val target = modelFile(tier)
            if (isDownloaded(tier)) return@withContext target

            val partial = File(modelsDir, "${tier.modelFileName}.part")
            val request = Request.Builder().url(tier.downloadUrl).build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw ModelDownloadException("Couldn't download the ${tier.label} model (server said ${response.code}).")
                    }
                    val body = response.body ?: throw ModelDownloadException("Empty response while downloading the ${tier.label} model.")
                    val contentLength = body.contentLength()

                    partial.parentFile?.mkdirs()
                    body.byteStream().use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var totalRead = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                totalRead += read
                                output.write(buffer, 0, read)
                                if (contentLength > 0) {
                                    onProgress(((totalRead * 100) / contentLength).toInt().coerceIn(0, 100))
                                }
                            }
                        }
                    }
                }
                if (!partial.renameTo(target)) {
                    throw ModelDownloadException("Couldn't save the downloaded model to storage.")
                }
                target
            } catch (e: ModelDownloadException) {
                partial.delete()
                throw e
            } catch (e: Exception) {
                partial.delete()
                throw ModelDownloadException("Model download failed: ${e.message}", e)
            }
        }
}
