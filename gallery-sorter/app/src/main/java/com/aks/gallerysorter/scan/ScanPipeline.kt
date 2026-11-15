package com.aks.gallerysorter.scan

import android.graphics.Bitmap
import android.util.Log
import androidx.room.withTransaction
import com.aks.gallerysorter.classify.FaceClusterer
import com.aks.gallerysorter.classify.Signals
import com.aks.gallerysorter.classify.blurScore
import com.aks.gallerysorter.classify.categorize
import com.aks.gallerysorter.classify.documentCategory
import com.aks.gallerysorter.classify.isOwnCameraOriginal
import com.aks.gallerysorter.classify.ml.FaceEngine
import com.aks.gallerysorter.classify.ml.NsfwClassifier
import com.aks.gallerysorter.classify.ml.NsfwScore
import com.aks.gallerysorter.classify.ml.QrDetector
import com.aks.gallerysorter.classify.ml.SceneLabeler
import com.aks.gallerysorter.classify.ml.TextReader
import com.aks.gallerysorter.classify.sourceCategories
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryAssignment
import com.aks.gallerysorter.core.CategoryGroup
import com.aks.gallerysorter.core.ExifData
import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.dupes.pHash
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.FaceRow
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.PersonRow
import com.aks.gallerysorter.places.CityIndex
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Indexes the gallery (spec 6.2–6.3): lists MediaStore, analyses new/changed items one at a time, writes
 * labels, then recomputes duplicate groups and trips. Resumable: items are first stored PENDING and a later
 * run picks up whatever is still PENDING. ML calls block their thread, so analysis runs on Dispatchers.Default.
 * [autoHide] is called for confidently Sensitive items when the auto-hide setting is on, unless the
 * user has any override on the item.
 */
class ScanPipeline(
    private val source: MediaSource,
    private val db: AppDatabase,
    private val nsfw: NsfwClassifier,
    private val faces: FaceEngine,
    private val text: TextReader,
    private val scenes: SceneLabeler,
    private val qr: QrDetector,
    private val cityIndex: CityIndex,
    private val frames: FrameSource,
    private val settings: ScanSettings = ScanSettings(db.settingsDao()),
    private val clusterer: FaceClusterer = FaceClusterer(),
    private val autoHide: suspend (MediaRow, List<CategoryAssignment>) -> Unit,
) : AutoCloseable {

    private class RunContext(val ownModels: Set<String>, val mePersonId: Long?, val autoHide: Boolean)

    /** Per person: the sum of its L2-normalised face embeddings (cosine-equivalent to the mean). */
    private var centroids: MutableMap<Long, FloatArray>? = null

    private enum class Outcome { DONE, UNREADABLE, FAILED }

    private class MediaRead(val thumb: Bitmap, val frames: List<Bitmap?>, val exif: ExifData?)

    suspend fun runIncremental(progress: (done: Int, total: Int) -> Unit) {
        // Poison pill: a marker left behind means the process died (native crash / OOM) on that item.
        settings.inflightItem()?.let { id ->
            Log.w(TAG, "item $id crashed the previous scan; marking it unreadable")
            db.mediaDao().get(id)?.let { db.mediaDao().upsert(it.copy(status = ScanStatus.UNREADABLE)) }
            settings.clearInflightItem()
        }
        val current = source.all()
        val known = db.mediaDao().generations().associate { it.id to it.generation }
        val delta = computeDelta(known, current)
        if (current.isEmpty() && known.isNotEmpty()) {
            // An empty listing with a non-empty index means MediaStore is unavailable or permission was lost.
            Log.w(TAG, "media source returned nothing but ${known.size} items are indexed; skipping removals")
        } else if (delta.removedIds.isNotEmpty()) {
            db.withTransaction { delta.removedIds.forEach { db.mediaDao().delete(it) } }
        }
        val fresh = delta.added + delta.changed
        if (fresh.isNotEmpty()) db.mediaDao().upsertAll(fresh.map { it.toRow(ScanStatus.PENDING) })

        val pending = db.mediaDao().byStatus(ScanStatus.PENDING, Int.MAX_VALUE)
        val ctx = runContext()
        progress(0, pending.size)
        var failuresInARow = 0
        pending.forEachIndexed { i, row ->
            failuresInARow = if (processWith(row.toItem(), ctx) == Outcome.FAILED) failuresInARow + 1 else 0
            if (failuresInARow >= MAX_FAILURES_IN_A_ROW) {
                throw ScanAbortedException("$failuresInARow consecutive analysis failures; stopping this run")
            }
            progress(i + 1, pending.size)
        }

        PostScan(source, db, cityIndex, settings).run()
    }

    /**
     * Analyses one item and writes its row, faces and labels. A media-read failure marks it UNREADABLE; a
     * classifier or DB failure leaves it PENDING for the next run.
     */
    suspend fun process(item: MediaItem) {
        processWith(item, runContext())
    }

    override fun close() {
        listOf(nsfw, faces, text, scenes, qr).forEach { dep ->
            (dep as? AutoCloseable)?.let { runCatching { it.close() } }
        }
    }

    private suspend fun runContext() = RunContext(
        ownModels = settings.ownCameraModels(),
        mePersonId = db.peopleDao().people().first().firstOrNull { it.isMe }?.id,
        autoHide = settings.autoHideSensitive(),
    )

    /**
     * The in-flight marker is cleared on every handled outcome and on cancellation, but deliberately not
     * while an Error (e.g. OutOfMemoryError) unwinds: then the process is likely dying and the marker makes
     * the next run mark this item UNREADABLE instead of crashing on it again.
     */
    private suspend fun processWith(item: MediaItem, ctx: RunContext): Outcome {
        settings.setInflightItem(item.id)
        val outcome = try {
            analyseItem(item, ctx)
        } catch (e: CancellationException) {
            withContext(NonCancellable) { settings.clearInflightItem() }
            throw e
        }
        settings.clearInflightItem()
        return outcome
    }

    private suspend fun analyseItem(item: MediaItem, ctx: RunContext): Outcome {
        val media = try {
            readMedia(item)
        } catch (e: MediaReadException) {
            Log.w(TAG, "item ${item.id} unreadable: ${e.cause?.javaClass?.name}")
            // The previous row (and its labels) is kept: a read failure must never un-hide a Sensitive item.
            markUnreadable(item)
            return Outcome.UNREADABLE
        }
        val (row, assignments) = try {
            analyseAndWrite(item, media, ctx)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "analysis failed for item ${item.id}: ${e.javaClass.name}")
            return recordAnalysisFailure(item)
        }
        val sensitive = assignments.any { it.category.group == CategoryGroup.SENSITIVE && it.confidence >= AUTO_HIDE_MIN }
        if (ctx.autoHide && sensitive) {
            try {
                // Any user correction (override) on the item means it is never auto-hidden: the
                // user's decision must not be undone by re-classification.
                if (db.labelDao().overridesFor(item.id).isEmpty()) autoHide(row, assignments)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "auto-hide failed for item ${item.id}: ${e.javaClass.name}")
            }
        }
        return Outcome.DONE
    }

    private suspend fun markUnreadable(item: MediaItem, failures: Int? = null) {
        val previous = db.mediaDao().get(item.id)
        val row = previous?.copy(status = ScanStatus.UNREADABLE, generation = item.generation)
            ?: item.toRow(ScanStatus.UNREADABLE)
        db.mediaDao().upsert(if (failures != null) row.copy(analysisFailures = failures) else row)
    }

    /** Counts a classifier/DB failure; the item stays PENDING until its third, then becomes UNREADABLE. */
    private suspend fun recordAnalysisFailure(item: MediaItem): Outcome = try {
        val previous = db.mediaDao().get(item.id)?.takeIf { it.generation == item.generation }
        val failures = (previous?.analysisFailures ?: 0) + 1
        if (failures >= MAX_ANALYSIS_FAILURES) {
            Log.w(TAG, "item ${item.id} failed analysis $failures times; marking it unreadable")
            markUnreadable(item, failures)
            Outcome.UNREADABLE
        } else {
            db.mediaDao().upsert((previous ?: item.toRow(ScanStatus.PENDING)).copy(analysisFailures = failures))
            Outcome.FAILED
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "cannot record the failure of item ${item.id}: ${e.javaClass.name}")
        Outcome.FAILED
    }

    /** Step 1: thumbnail, video frames and EXIF. Any failure here (OOM while decoding included) is a read failure. */
    private suspend fun readMedia(item: MediaItem): MediaRead = withContext(Dispatchers.IO) {
        try {
            val thumb = source.thumbnail(item, THUMB_PX)?.software() ?: throw IOException("no thumbnail for ${item.id}")
            val frameBitmaps = if (item.kind == MediaKind.VIDEO) {
                frames.frames(item, videoSampleTimesMs(item.durationMs ?: 0), THUMB_PX).map { it?.software() }
            } else {
                emptyList()
            }
            // EXIF is read up front because the source rules (screenshot check, step 5) take it.
            MediaRead(thumb, frameBitmaps, source.exif(item))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MediaReadException(e)
        } catch (e: OutOfMemoryError) {
            throw MediaReadException(e)
        }
    }

    private suspend fun analyseAndWrite(
        item: MediaItem,
        media: MediaRead,
        ctx: RunContext,
    ): Pair<MediaRow, List<CategoryAssignment>> =
        withContext(Dispatchers.Default) {
            val thumb = media.thumb
            val frameBitmaps = media.frames
            val exif = media.exif
            // Videos: max score per label across the sampled frames (the thumbnail if none decoded).
            val views = frameBitmaps.filterNotNull().ifEmpty { listOf(thumb) }

            // 2. NSFW.
            val scores = views.map { nsfw.score(it) }
            val nsfwScore = NsfwScore(scores.maxOf { it.explicit }, scores.maxOf { it.suggestive })

            // 3. Faces → cluster against known people (new people are created at write time).
            val hits = faces.faces(thumb)
            val known = centroids()
            val matched = hits.map { clusterer.assign(it.embedding, known) }
            val thumbArea = thumb.width.toFloat() * thumb.height
            val largeSingleFace = hits.size == 1 && hits[0].box.width() * hits[0].box.height() > LARGE_FACE * thumbArea

            // 4. Scene labels and QR.
            val labels = HashMap<String, Float>()
            for (v in views) for ((name, c) in scenes.labels(v)) labels.merge(name, c, ::maxOf)
            val hasQr = qr.hasCode(thumb)

            // 5. OCR, only for likely documents.
            val sources = sourceCategories(item.path, item.kind, exif, ctx.ownModels)
            val doc = if (needsOcr(labels.keys, sources, item, thumb)) documentCategory(text.read(thumb)) else null

            // 6. Blur (images only: video frames are routinely motion-blurred).
            val blur = if (item.kind == MediaKind.IMAGE) blurScore(thumb.toGray(), thumb.width, thumb.height) else null

            // 7. sha256 is computed lazily in the duplicates pass, only for size collisions.
            // 8. pHash (videos: also the 25/50/75% frames).
            val phash = pHash(thumb.gray32())
            val dupeFrames = DUPE_FRAME_INDICES.map { frameBitmaps.getOrNull(it) }
            val framePhashes = if (item.kind == MediaKind.VIDEO && dupeFrames.all { it != null }) {
                dupeFrames.joinToString(",") { pHash(it!!.gray32()).toString() }
            } else {
                null
            }

            // 9. EXIF → city.
            val lat = exif?.lat
            val lon = exif?.lon
            val city = if (lat != null && lon != null) cityIndex.nearest(lat, lon)?.name else null
            val ownCamera = isOwnCameraOriginal(item.path, exif, ctx.ownModels)

            // 10. Categorize → write row, faces and labels together.
            val previous = db.mediaDao().get(item.id)
            val row = item.toRow(ScanStatus.DONE).copy(
                sha256 = previous?.takeIf { it.generation == item.generation }?.sha256,
                phash = phash, lat = lat, lon = lon, city = city,
                framePhashes = framePhashes, ownCameraOriginal = ownCamera,
            )
            val (assignments, personIds) = db.withTransaction {
                val personIds = matched.map { it ?: db.peopleDao().insertPerson(PersonRow()) }
                val signals = Signals(
                    nsfw = nsfwScore, sources = sources, ownCamera = ownCamera, doc = doc, labels = labels,
                    hasQr = hasQr, blur = blur, faces = personIds, frontCamera = exif?.lensFacingFront,
                    largeSingleFace = largeSingleFace,
                )
                val out = categorize(signals, ctx.mePersonId, db.labelDao().overridesFor(item.id))
                db.mediaDao().upsert(row)
                db.peopleDao().clearFaces(item.id)
                db.peopleDao().insertFaces(
                    hits.mapIndexed { i, hit -> FaceRow(mediaId = item.id, personId = personIds[i], embedding = encodeEmbedding(hit.embedding)) },
                )
                db.labelDao().clearLabels(item.id)
                db.labelDao().upsertAll(out.map { LabelRow(item.id, it.category, it.confidence) })
                out to personIds
            }
            // Committed: fold this item's faces into the in-memory centroids.
            hits.forEachIndexed { i, hit -> addTo(known, personIds[i], hit.embedding) }
            row to assignments
        }

    private fun needsOcr(labels: Set<String>, sources: Set<Category>, item: MediaItem, thumb: Bitmap): Boolean {
        if (labels.any { it.lowercase() in DOC_LABELS }) return true
        if (Category.SCREENSHOTS in sources) return true
        // Camera photos are mostly 4:3, so for them only a document-like scene label triggers OCR.
        if (Category.CAMERA in sources) return false
        val w = if (item.width > 0) item.width else thumb.width
        val h = if (item.height > 0) item.height else thumb.height
        if (w <= 0 || h <= 0) return false
        val ratio = maxOf(w, h).toDouble() / minOf(w, h)
        return ratio in 1.3..1.5 || ratio > 1.9
    }

    private suspend fun centroids(): MutableMap<Long, FloatArray> = centroids ?: HashMap<Long, FloatArray>().also { m ->
        for (f in db.peopleDao().allFaces()) addTo(m, f.personId ?: continue, decodeEmbedding(f.embedding))
        centroids = m
    }

    private fun addTo(m: MutableMap<Long, FloatArray>, person: Long, e: FloatArray) {
        val sum = m[person]
        if (sum == null || sum.size != e.size) m[person] = e.copyOf() else for (i in e.indices) sum[i] += e[i]
    }

    companion object {
        const val THUMB_PX = 320
        /** Auto-hide only confident Sensitive results (spec: explicit ≥ 0.85). */
        private const val AUTO_HIDE_MIN = 0.85f
        /** A single face covering more than this fraction of the thumbnail counts as a selfie. */
        private const val LARGE_FACE = 0.20f
        /** Consecutive classifier/DB failures after which a run gives up (something systemic is wrong). */
        private const val MAX_FAILURES_IN_A_ROW = 3
        /** Analysis failures of one item (same generation, across runs) after which it is UNREADABLE. */
        private const val MAX_ANALYSIS_FAILURES = 3
        private val DOC_LABELS = setOf("document", "text", "paper", "receipt")
    }
}
