package com.aks.gallerysorter.core

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

data class ExifData(
    val make: String?,
    val model: String?,
    val lat: Double?,
    val lon: Double?,
    val lensFacingFront: Boolean?,
)

interface MediaSource {
    suspend fun all(): List<MediaItem>
    suspend fun changedSince(generation: Long): List<MediaItem>
    fun open(item: MediaItem): InputStream
    suspend fun thumbnail(item: MediaItem, px: Int = 320): Bitmap?
    suspend fun exif(item: MediaItem): ExifData?
}

class MediaStoreSource(private val resolver: ContentResolver) : MediaSource {

    override suspend fun all(): List<MediaItem> = withContext(Dispatchers.IO) {
        query(IMAGES, null, null) + query(VIDEOS, null, null)
    }

    override suspend fun changedSince(generation: Long): List<MediaItem> = withContext(Dispatchers.IO) {
        val sel = "${MediaStore.MediaColumns.GENERATION_MODIFIED} > ?"
        val args = arrayOf(generation.toString())
        query(IMAGES, sel, args) + query(VIDEOS, sel, args)
    }

    override fun open(item: MediaItem): InputStream =
        resolver.openInputStream(uriOf(item)) ?: error("Cannot open ${item.id}")

    override suspend fun thumbnail(item: MediaItem, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            resolver.loadThumbnail(uriOf(item), Size(px, px), null)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun exif(item: MediaItem): ExifData? = withContext(Dispatchers.IO) {
        if (item.kind != MediaKind.IMAGE) return@withContext null
        val uri = uriOf(item)
        try {
            val original = try {
                // Needs ACCESS_MEDIA_LOCATION at runtime to keep GPS tags; without it the open throws
                // (SecurityException or UnsupportedOperationException) and the redacted file is read.
                resolver.openInputStream(MediaStore.setRequireOriginal(uri))?.let { it to true }
            } catch (e: SecurityException) {
                null
            } catch (e: UnsupportedOperationException) {
                null
            } ?:(resolver.openInputStream(uri)?.let { it to false } ?: return@withContext null)
            original.first.use { stream ->
                val exif = ExifInterface(stream)
                val ll = if (original.second) exif.latLong else null
                val lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
                ExifData(
                    make = exif.getAttribute(ExifInterface.TAG_MAKE),
                    model = exif.getAttribute(ExifInterface.TAG_MODEL),
                    lat = ll?.get(0),
                    lon = ll?.get(1),
                    lensFacingFront = if (lens?.contains("front", ignoreCase = true) == true) true else null,
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun uriOf(item: MediaItem): Uri =
        ContentUris.withAppendedId(if (item.kind == MediaKind.IMAGE) IMAGES else VIDEOS, item.id)

    private fun query(base: Uri, selection: String?, args: Array<String>?): List<MediaItem> {
        val isVideo = base == VIDEOS
        val cols = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DATA)
            add(MediaStore.MediaColumns.RELATIVE_PATH)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.GENERATION_MODIFIED)
            if (isVideo) add(MediaStore.MediaColumns.DURATION)
        }.toTypedArray()
        val out = ArrayList<MediaItem>()
        resolver.query(base, cols, selection, args, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val iData = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val iRel = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val iName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val iMime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val iW = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val iH = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val iTaken = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val iGen = c.getColumnIndexOrThrow(MediaStore.MediaColumns.GENERATION_MODIFIED)
            val iDur = if (isVideo) c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION) else -1
            while (c.moveToNext()) {
                val path = c.getString(iData)
                    ?: "/storage/emulated/0/" + (c.getString(iRel) ?: "") + (c.getString(iName) ?: "")
                out += MediaItem(
                    id = c.getLong(iId),
                    path = path,
                    kind = if (isVideo) MediaKind.VIDEO else MediaKind.IMAGE,
                    sizeBytes = c.getLong(iSize),
                    mime = c.getString(iMime) ?: "",
                    width = c.getInt(iW),
                    height = c.getInt(iH),
                    dateTakenMs = if (c.isNull(iTaken)) null else c.getLong(iTaken),
                    durationMs = if (iDur >= 0 && !c.isNull(iDur)) c.getLong(iDur) else null,
                    generation = c.getLong(iGen),
                )
            }
        }
        return out
    }

    private companion object {
        val IMAGES: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val VIDEOS: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
}
