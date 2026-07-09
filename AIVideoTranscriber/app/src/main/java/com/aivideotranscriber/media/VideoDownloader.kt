package com.aivideotranscriber.media

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadException(message: String) : Exception(message)

/**
 * Fetches a direct, publicly downloadable video/media URL (e.g. a raw .mp4 link, or a direct
 * Google Drive/Dropbox link). Deliberately does NOT support YouTube/TikTok/Instagram/Facebook -
 * scraping those breaks constantly and generally violates their Terms of Service.
 */
object VideoDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Generous but bounded so a mobile CPU can realistically decode + transcribe it.
    private const val MAX_DOWNLOAD_BYTES = 2L * 1024 * 1024 * 1024 // 2 GB

    fun download(url: String, destination: File, onProgress: (Int) -> Unit = {}): File {
        val httpUrl = url.trim().toHttpUrlOrNull()
            ?: throw DownloadException("That doesn't look like a valid video link.")
        if (httpUrl.scheme != "https") {
            throw DownloadException("Only secure https:// links are supported.")
        }

        val request = Request.Builder().url(httpUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw DownloadException("Couldn't download this video (server said ${response.code}).")
            }
            val body = response.body ?: throw DownloadException("The server returned an empty response.")
            val contentLength = body.contentLength()
            if (contentLength > MAX_DOWNLOAD_BYTES) {
                throw DownloadException("This video is larger than the 2 GB on-device processing limit.")
            }

            destination.parentFile?.mkdirs()
            body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var totalRead = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        totalRead += read
                        if (totalRead > MAX_DOWNLOAD_BYTES) {
                            throw DownloadException("This video is larger than the 2 GB on-device processing limit.")
                        }
                        output.write(buffer, 0, read)
                        if (contentLength > 0) {
                            onProgress(((totalRead * 100) / contentLength).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
        }
        return destination
    }
}
