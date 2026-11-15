package com.aks.gallerysorter.scan

import android.content.Context
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
import com.aks.gallerysorter.classify.ml.LiteRtNsfwClassifier
import com.aks.gallerysorter.classify.ml.MlKitFaceEngine
import com.aks.gallerysorter.classify.ml.MlKitQrDetector
import com.aks.gallerysorter.classify.ml.MlKitSceneLabeler
import com.aks.gallerysorter.classify.ml.MlKitTextReader
import com.aks.gallerysorter.classify.ml.NsfwClassifier
import com.aks.gallerysorter.classify.ml.NsfwScore
import com.aks.gallerysorter.classify.ml.QrDetector
import com.aks.gallerysorter.classify.ml.SceneLabeler
import com.aks.gallerysorter.classify.ml.TextReader
import com.aks.gallerysorter.classify.sourceCategories
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryAssignment
import com.aks.gallerysorter.core.CategoryGroup
import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.MediaStoreSource
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.dupes.DupeCandidate
import com.aks.gallerysorter.dupes.groupDuplicates
import com.aks.gallerysorter.dupes.pHash
import com.aks.gallerysorter.dupes.sha256
import com.aks.gallerysorter.dupes.suggestKeep
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.DupeRow
import com.aks.gallerysorter.index.entities.FaceRow
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.PersonRow
import com.aks.gallerysorter.places.CityIndex
import com.aks.gallerysorter.places.TripPoint
import com.aks.gallerysorter.places.detectTrips
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Indexes the gallery (spec 6.2–6.3): lists MediaStore, analyses new/changed items one at a time, writes
 * labels, then recomputes duplicate groups and trips. Resumable: items are first stored PENDING and a later
 * run picks up whatever is still PENDING. ML calls block their thread, so analysis runs on Dispatchers.Default.
 * [autoHide] is called for confidently Sensitive items when the auto-hide setting is on.
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
    private val autoHide: (MediaRow, List<CategoryAssignment>) -> Unit,
) : AutoCloseable {

    private class RunContext(val ownModels: Set<String>, val mePersonId: Long?, val autoHide: Boolean)

    /** Per person: the sum of its L2-normalised face embeddings (cosine-equivalent to the mean). */
    private var centroids: MutableMap<Long, FloatArray>? = null

    suspend fun runIncremental(progress: (done: Int, total: Int) -> Unit) {
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
        pending.forEachIndexed { i, row ->
            processWith(row.toItem(), ctx)
            progress(i + 1, pending.size)
        }

        val done = db.mediaDao().byStatus(ScanStatus.DONE, Int.MAX_VALUE)
        recomputeDuplicates(done)
        recomputeTrips(done)
    }

    /** Analyses one item and writes its row, faces and labels; any failure marks it UNREADABLE. */
    suspend fun process(item: MediaItem) = processWith(item, runContext())

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

    private suspend fun processWith(item: MediaItem, ctx: RunContext) {
        val (row, assignments) = try {
            analyseAndWrite(item, ctx)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "item ${item.id} unreadable", e)
            // Earlier labels are kept on purpose: a read failure must never un-hide a Sensitive item.
            db.mediaDao().upsert(item.toRow(ScanStatus.UNREADABLE))
            return
        }
        val sensitive = assignments.any { it.category.group == CategoryGroup.SENSITIVE && it.confidence >= AUTO_HIDE_MIN }
        if (ctx.autoHide && sensitive) {
            try {
                autoHide(row, assignments)
            } catch (e: Exception) {
                Log.w(TAG, "auto-hide failed for ${item.id}", e)
            }
        }
    }

    private suspend fun analyseAndWrite(item: MediaItem, ctx: RunContext): Pair<MediaRow, List<CategoryAssignment>> =
        withContext(Dispatchers.Default) {
            // 1. Thumbnail; videos also sample frames (max score per label across them).
            val thumb = source.thumbnail(item, THUMB_PX)?.software() ?: throw IOException("no thumbnail for ${item.id}")
            val frameBitmaps = if (item.kind == MediaKind.VIDEO) {
                frames.frames(item, videoSampleTimesMs(item.durationMs ?: 0), THUMB_PX).map { it?.software() }
            } else {
                emptyList()
            }
            val views = frameBitmaps.filterNotNull().ifEmpty { listOf(thumb) }
            // EXIF is read up front because the source rules (screenshot check, step 5) take it.
            val exif = source.exif(item)

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
                Log.w(TAG, "cannot hash ${r.id}", e)
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

    companion object {
        const val THUMB_PX = 320
        /** Auto-hide only confident Sensitive results (spec: explicit ≥ 0.85). */
        private const val AUTO_HIDE_MIN = 0.85f
        /** A single face covering more than this fraction of the thumbnail counts as a selfie. */
        private const val LARGE_FACE = 0.20f
        private val DOC_LABELS = setOf("document", "text", "paper", "receipt")

        @Volatile private var database: AppDatabase? = null

        /** The process-wide encrypted index (Task 14's AppGraph takes this over). */
        fun database(context: Context): AppDatabase = database ?: synchronized(this) {
            database ?: AppDatabase.open(context.applicationContext).also { database = it }
        }

        /** Builds the pipeline on the real MediaStore and on-device models; throws if a model can't load. */
        fun create(
            context: Context,
            autoHide: (MediaRow, List<CategoryAssignment>) -> Unit = { _, _ -> },
        ): ScanPipeline {
            val app = context.applicationContext
            val nsfw = LiteRtNsfwClassifier.create(app) ?: error("NSFW model unavailable")
            val faces = MlKitFaceEngine.create(app) ?: run { nsfw.close(); error("face model unavailable") }
            return ScanPipeline(
                source = MediaStoreSource(app.contentResolver), db = database(app), nsfw = nsfw, faces = faces,
                text = MlKitTextReader(), scenes = MlKitSceneLabeler(), qr = MlKitQrDetector(),
                cityIndex = app.assets.open("cities.bin").use { CityIndex(it) },
                frames = RetrieverFrameSource(app), autoHide = autoHide,
            )
        }
    }
}

private fun MediaItem.toRow(status: ScanStatus) = MediaRow(
    id = id, path = path, kind = kind, sizeBytes = sizeBytes, mime = mime, width = width, height = height,
    dateTakenMs = dateTakenMs, durationMs = durationMs, generation = generation, status = status,
)

private fun MediaRow.toItem() = MediaItem(
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
