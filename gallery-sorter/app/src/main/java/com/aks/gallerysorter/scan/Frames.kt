package com.aks.gallerysorter.scan

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.util.Log
import com.aks.gallerysorter.core.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Video frame extraction, behind an interface so the pipeline test can fake it. */
interface FrameSource {
    /** One entry per [timesMs], in order; null where no frame could be decoded. Frames fit within [px]. */
    suspend fun frames(item: MediaItem, timesMs: List<Long>, px: Int): List<Bitmap?>
}

/** [FrameSource] on [MediaMetadataRetriever], taking the closest sync frame to each time. */
class RetrieverFrameSource(private val context: Context) : FrameSource {
    override suspend fun frames(item: MediaItem, timesMs: List<Long>, px: Int): List<Bitmap?> =
        withContext(Dispatchers.IO) {
            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                timesMs.map { t ->
                    try {
                        retriever.getScaledFrameAtTime(t * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, px, px)
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "cannot open video ${item.id} for frames", e)
                timesMs.map { null }
            } finally {
                retriever.release()
            }
        }
}

/** Sample times for a video of [durationMs]: 1 s, 25%, 50%, 75% and the end (spec 6.2). */
fun videoSampleTimesMs(durationMs: Long): List<Long> {
    val d = durationMs.coerceAtLeast(0)
    return listOf(minOf(1000L, d), d / 4, d / 2, d * 3 / 4, d)
}

/** Indices into [videoSampleTimesMs] whose frames feed duplicate detection (25/50/75%). */
val DUPE_FRAME_INDICES = listOf(1, 2, 3)

/** A copy readable with getPixels (hardware bitmaps are not); the bitmap itself otherwise. */
fun Bitmap.software(): Bitmap =
    if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this

/** Luminance 0..255 (BT.601 weights) of every pixel, row-major, width × height values. */
fun Bitmap.toGray(): IntArray {
    val px = IntArray(width * height)
    getPixels(px, 0, width, 0, 0, width, height)
    for (i in px.indices) {
        val p = px[i]
        px[i] = (299 * ((p shr 16) and 0xFF) + 587 * ((p shr 8) and 0xFF) + 114 * (p and 0xFF)) / 1000
    }
    return px
}

/** The bitmap scaled to 32×32 and converted to luminance, as [com.aks.gallerysorter.dupes.pHash] expects. */
fun Bitmap.gray32(): IntArray {
    val scaled = Bitmap.createScaledBitmap(this, 32, 32, true)
    val gray = scaled.toGray()
    if (scaled !== this) scaled.recycle()
    return gray
}

internal const val TAG = "GallerySorterScan"
