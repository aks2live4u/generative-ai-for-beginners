package com.braindump.app.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * All photos and voice notes are stored as plain files under the app's
 * private storage (/data/data/com.braindump.app/files/media). Thoughts in
 * the database reference these files by name only.
 */
class MediaStorage(private val context: Context) {

    val mediaDir: File by lazy {
        File(context.filesDir, "media").apply { mkdirs() }
    }

    /** Saves an image picked from the camera/gallery, downscaled per the quality setting. Returns the file name. */
    fun saveImage(uri: Uri, quality: String): String {
        val bitmap = loadBitmap(uri) ?: throw IllegalArgumentException("Could not decode image")
        val maxDimension = when (quality) {
            "low" -> 800
            "high" -> 2048
            else -> 1280 // medium
        }
        val jpegQuality = when (quality) {
            "low" -> 60
            "high" -> 90
            else -> 78
        }
        val scaled = scaleBitmap(bitmap, maxDimension)
        val fileName = "img_${UUID.randomUUID()}.jpg"
        val file = File(mediaDir, fileName)
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
        }
        if (scaled !== bitmap) bitmap.recycle()
        scaled.recycle()
        return fileName
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        context.contentResolver.openInputStream(uri)?.use { input ->
            return BitmapFactory.decodeStream(input)
        }
        return null
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / maxOf(width, height)
        val newWidth = (width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /** Allocates a new file (not yet created) for an audio recording. */
    fun newAudioFile(): File = File(mediaDir, "audio_${UUID.randomUUID()}.m4a")

    fun file(name: String): File = File(mediaDir, name)

    fun delete(name: String) {
        if (name.isBlank()) return
        val file = File(mediaDir, name)
        if (file.exists()) file.delete()
    }

    fun deleteAll() {
        mediaDir.listFiles()?.forEach { it.delete() }
    }

    /** Reads a media file and returns it base64-encoded, for sending to Claude/Gemini. */
    fun readAsBase64(name: String): String {
        val bytes = File(mediaDir, name).readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
