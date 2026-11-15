package com.aks.gallerysorter.scan

import android.util.Log
import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.dupes.DupeCandidate
import com.aks.gallerysorter.dupes.groupDuplicates
import com.aks.gallerysorter.dupes.sha256
import com.aks.gallerysorter.dupes.suggestKeep
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.DupeRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.places.CityIndex
import com.aks.gallerysorter.places.TripPoint
import com.aks.gallerysorter.places.detectTrips
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A run stopped after repeated classifier/DB failures; the PENDING items are retried next run. */
class ScanAbortedException(message: String) : Exception(message)

/** The media itself could not be read or decoded (the item becomes UNREADABLE). */
internal class MediaReadException(cause: Throwable) : Exception(cause)

/** After each run: duplicate groups (sha256 only for size collisions) and trips, over the DONE rows. */
internal class PostScan(
    private val source: MediaSource,
    private val db: AppDatabase,
    private val cityIndex: CityIndex,
    private val settings: ScanSettings,
) {
    suspend fun run() {
        val done = db.mediaDao().byStatus(ScanStatus.DONE, Int.MAX_VALUE)
        recomputeDuplicates(done)
        recomputeTrips(done)
    }

    private suspend fun recomputeDuplicates(rows: List<MediaRow>) {
        val colliding = rows.groupBy { it.kind to it.sizeBytes }.values.filter { it.size > 1 }.flatten()
        val shaById = HashMap<Long, String>()
        for (r in colliding) {
            val sha = r.sha256 ?: try {
                withContext(Dispatchers.IO) { source.open(r.toItem()).use { sha256(it) } }
                    .also { db.mediaDao().upsert(r.copy(sha256 = it)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "cannot hash item ${r.id}: ${e.javaClass.name}")
                null
            }
            if (sha != null) shaById[r.id] = sha
        }
        val candidates = rows.map { r ->
            DupeCandidate(
                id = r.id, kind = r.kind, sizeBytes = r.sizeBytes, sha256 = shaById[r.id], phash = r.phash,
                durationMs = r.durationMs,
                framePhashes = r.framePhashes?.split(',')?.map { it.toLong() }.orEmpty(),
                width = r.width, height = r.height, isOwnCameraOriginal = r.ownCameraOriginal, exifDateMs = r.dateTakenMs,
            )
        }
        val dupeRows = groupDuplicates(candidates).flatMap { group ->
            val keep = suggestKeep(group).id
            val groupId = group.minOf { it.id }
            group.map { DupeRow(groupId, it.id, suggestedKeep = it.id == keep) }
        }
        db.dupeDao().replaceAll(dupeRows)
    }

    private suspend fun recomputeTrips(rows: List<MediaRow>) {
        val points = rows.mapNotNull { r ->
            val lat = r.lat ?: return@mapNotNull null
            val lon = r.lon ?: return@mapNotNull null
            val date = r.dateTakenMs ?: return@mapNotNull null
            cityIndex.nearest(lat, lon)?.let { TripPoint(r.id, date, it) }
        }
        settings.setTrips(detectTrips(points))
    }
}

internal fun MediaItem.toRow(status: ScanStatus) = MediaRow(
    id = id, path = path, kind = kind, sizeBytes = sizeBytes, mime = mime, width = width, height = height,
    dateTakenMs = dateTakenMs, durationMs = durationMs, generation = generation, status = status,
)

internal fun MediaRow.toItem() = MediaItem(
    id = id, path = path, kind = kind, sizeBytes = sizeBytes, mime = mime, width = width, height = height,
    dateTakenMs = dateTakenMs, durationMs = durationMs, generation = generation,
)

/** Face embeddings are stored as little-endian float32. */
internal fun encodeEmbedding(e: FloatArray): ByteArray =
    ByteBuffer.allocate(e.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply { asFloatBuffer().put(e) }.array()

internal fun decodeEmbedding(b: ByteArray): FloatArray {
    val fb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    return FloatArray(fb.remaining()).also { fb.get(it) }
}
