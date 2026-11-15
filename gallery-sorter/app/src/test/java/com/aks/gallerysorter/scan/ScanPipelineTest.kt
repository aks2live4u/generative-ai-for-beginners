package com.aks.gallerysorter.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aks.gallerysorter.classify.ml.FaceEngine
import com.aks.gallerysorter.classify.ml.FaceHit
import com.aks.gallerysorter.classify.ml.NsfwClassifier
import com.aks.gallerysorter.classify.ml.NsfwScore
import com.aks.gallerysorter.classify.ml.QrDetector
import com.aks.gallerysorter.classify.ml.SceneLabeler
import com.aks.gallerysorter.classify.ml.TextReader
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryAssignment
import com.aks.gallerysorter.core.ExifData
import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.OverrideRow
import com.aks.gallerysorter.places.CityIndex
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScanPipelineTest {
    private lateinit var db: AppDatabase
    private lateinit var settings: ScanSettings

    private val source = FakeSource()
    private val nsfw = FakeNsfw()
    private val text = FakeText()
    private val scenes = FakeScenes()
    private var faceEngine: FaceEngine = NoFaces
    private val hidden = mutableListOf<Pair<MediaRow, List<CategoryAssignment>>>()

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        settings = ScanSettings(db.settingsDao(), deviceModel = "test-device")
    }

    @After
    fun tearDown() = db.close()

    private fun pipeline() = ScanPipeline(
        source = source, db = db, nsfw = nsfw, faces = faceEngine, text = text, scenes = scenes, qr = NoQr,
        cityIndex = tinyCityIndex(), frames = NoFrames,
    ) { row, labels -> hidden += row to labels }

    private fun item(
        id: Long,
        path: String = "/storage/emulated/0/DCIM/Camera/IMG_$id.jpg",
        w: Int = 1920,
        h: Int = 1080,
        size: Long = 1000L + id,
        generation: Long = 1,
    ) = MediaItem(id, path, MediaKind.IMAGE, size, "image/jpeg", w, h, dateTakenMs = id * 1000, durationMs = null, generation = generation)

    private fun labels(id: Long) = runBlocking { db.labelDao().labelsFor(id).map { it.category }.toSet() }
    private fun status(id: Long) = runBlocking { db.mediaDao().get(id)?.status }

    @Test
    fun pipeline_classifiesAndWritesLabels() = runBlocking<Unit> {
        settings.setOwnCameraModels(setOf("vivo X200"))
        val it = item(1)
        source.add(it, exif = ExifData("vivo", "vivo X200", 19.07, 72.88, null))
        nsfw.scores[source.thumbs.getValue(1)] = NsfwScore(explicit = 0.95f, suggestive = 0.02f)

        pipeline().runIncremental { _, _ -> }

        assertThat(status(1)).isEqualTo(ScanStatus.DONE)
        assertThat(labels(1)).containsAtLeast(Category.MY_NUDES, Category.MY_NUDES_CAMERA, Category.CAMERA)
        assertThat(labels(1)).doesNotContain(Category.OTHER_ADULT)
        val row = db.mediaDao().get(1)!!
        assertThat(row.city).isEqualTo("Mumbai")
        assertThat(row.phash).isNotNull()
    }

    @Test
    fun pipeline_corruptFile_marksUnreadableAndContinues() = runBlocking<Unit> {
        listOf(item(1), item(2), item(3)).forEach { source.add(it) }
        source.failingThumbs += 2L
        val seen = mutableListOf<Pair<Int, Int>>()

        pipeline().runIncremental { done, total -> seen += done to total }

        assertThat(status(1)).isEqualTo(ScanStatus.DONE)
        assertThat(status(2)).isEqualTo(ScanStatus.UNREADABLE)
        assertThat(status(3)).isEqualTo(ScanStatus.DONE)
        assertThat(seen.last()).isEqualTo(3 to 3)

        source.thumbnailCalls.clear()
        pipeline().runIncremental { _, _ -> }
        assertThat(source.thumbnailCalls).doesNotContain(2L)
        assertThat(status(2)).isEqualTo(ScanStatus.UNREADABLE)

        // A new generation of the unreadable file is retried.
        source.failingThumbs.clear()
        source.add(item(2, generation = 2))
        pipeline().runIncremental { _, _ -> }
        assertThat(source.thumbnailCalls).containsExactly(2L)
        assertThat(status(2)).isEqualTo(ScanStatus.DONE)
    }

    @Test
    fun pipeline_skipsOcrForCameraPhotoWithoutDocLabels() = runBlocking<Unit> {
        val photo = item(1, w = 1920, h = 1080) // 16:9 camera photo, aspect 1.78
        source.add(photo)
        scenes.labels[source.thumbs.getValue(1)] = mapOf("Dog" to 0.9f, "Grass" to 0.8f)

        pipeline().runIncremental { _, _ -> }

        assertThat(text.calls).isEqualTo(0)
        assertThat(labels(1)).contains(Category.PETS)

        // Positive controls: a screenshot, a "Paper" label, and an A4-like non-camera image each trigger OCR.
        source.add(item(2, path = "/storage/emulated/0/Pictures/Screenshots/s.png", w = 1080, h = 1920))
        source.add(item(3, w = 1000, h = 1000))
        scenes.labels[source.thumbs.getValue(3)] = mapOf("Paper" to 0.6f)
        source.add(item(4, path = "/storage/emulated/0/Download/form.jpg", w = 1000, h = 1414))
        pipeline().runIncremental { _, _ -> }
        assertThat(text.calls).isEqualTo(3)
    }

    @Test
    fun pipeline_aspectGateAppliesOnlyToNonCameraItems() = runBlocking<Unit> {
        source.add(item(1, w = 4000, h = 3000)) // 4:3 camera photo, no doc labels
        pipeline().runIncremental { _, _ -> }
        assertThat(text.calls).isEqualTo(0)

        source.add(item(2, path = "/storage/emulated/0/Download/scan.jpg", w = 4000, h = 3000)) // 4:3 download
        pipeline().runIncremental { _, _ -> }
        assertThat(text.calls).isEqualTo(1)
    }

    @Test
    fun pipeline_autoHideCalledOnlyWhenEnabledAndConfident() = runBlocking<Unit> {
        source.add(item(1))
        nsfw.scores[source.thumbs.getValue(1)] = NsfwScore(0.95f, 0f)
        pipeline().runIncremental { _, _ -> }
        assertThat(hidden).isEmpty() // setting defaults to off

        settings.setAutoHideSensitive(true)
        source.add(item(2))
        nsfw.scores[source.thumbs.getValue(2)] = NsfwScore(0.95f, 0f)
        source.add(item(3))
        nsfw.scores[source.thumbs.getValue(3)] = NsfwScore(0.70f, 0f) // Review band
        source.add(item(4)) // safe
        pipeline().runIncremental { _, _ -> }

        assertThat(hidden.map { it.first.id }).containsExactly(2L)
        assertThat(hidden.single().second.map { it.category }).contains(Category.OTHER_ADULT)
        assertThat(labels(3)).containsAtLeast(Category.OTHER_ADULT, Category.REVIEW)
    }

    @Test
    fun pipeline_autoHideSkipsItemsWithAnyUserOverride() = runBlocking<Unit> {
        settings.setAutoHideSensitive(true)
        source.add(item(1))
        nsfw.scores[source.thumbs.getValue(1)] = NsfwScore(0.95f, 0f)
        source.add(item(2))
        nsfw.scores[source.thumbs.getValue(2)] = NsfwScore(0.95f, 0f)
        source.add(item(3))
        nsfw.scores[source.thumbs.getValue(3)] = NsfwScore(0.95f, 0f)
        // Unrelated correction, and a user-confirmed Sensitive category: both block auto-hide.
        db.labelDao().upsertOverride(OverrideRow(1, Category.SCREENSHOTS, isExcluded = true))
        db.labelDao().upsertOverride(OverrideRow(2, Category.MY_NUDES, isExcluded = false))

        pipeline().runIncremental { _, _ -> }

        assertThat(hidden.map { it.first.id }).containsExactly(3L)
        assertThat(labels(2)).contains(Category.MY_NUDES)
    }

    @Test
    fun pipeline_emptySourceWithKnownItems_doesNotDeleteRows() = runBlocking<Unit> {
        source.add(item(1))
        source.add(item(2))
        pipeline().runIncremental { _, _ -> }
        assertThat(db.mediaDao().allIds()).containsExactly(1L, 2L)

        source.clear() // provider unavailable / permission lost
        pipeline().runIncremental { _, _ -> }
        assertThat(db.mediaDao().allIds()).containsExactly(1L, 2L)
        assertThat(labels(1)).contains(Category.CAMERA)

        // A real removal (non-empty listing) still deletes.
        source.add(item(2))
        pipeline().runIncremental { _, _ -> }
        assertThat(db.mediaDao().allIds()).containsExactly(2L)
    }

    @Test
    fun pipeline_afterRun_writesDuplicateGroupsWithLazySha() = runBlocking<Unit> {
        val bytes = Random(7).nextBytes(4096)
        source.add(item(1, size = 4096, w = 4000, h = 3000), content = bytes)
        source.add(item(2, size = 4096, w = 1600, h = 1200), content = bytes)
        source.add(item(3, size = 999), content = Random(8).nextBytes(999))

        pipeline().runIncremental { _, _ -> }

        val dupes = db.dupeDao().all()
        assertThat(dupes.map { it.mediaId }).containsExactly(1L, 2L)
        assertThat(dupes.single { it.suggestedKeep }.mediaId).isEqualTo(1L) // higher resolution
        assertThat(db.mediaDao().get(1)!!.sha256).isNotNull()
        assertThat(db.mediaDao().get(3)!!.sha256).isNull() // no size collision → never hashed
    }

    @Test
    fun pipeline_facesClusterIntoPeopleAndLargeSingleFaceIsSelfie() = runBlocking<Unit> {
        // Every thumbnail shows one face covering ~40% of it; items 1 and 2 are the same person, 3 is not.
        val embeddings = mapOf(1L to floatArrayOf(1f, 0f, 0f), 2L to floatArrayOf(0.99f, 0.1f, 0f), 3L to floatArrayOf(0f, 0f, 1f))
        listOf(1L, 2L, 3L).forEach { source.add(item(it)) }
        faceEngine = object : FaceEngine {
            override suspend fun faces(bmp: Bitmap): List<FaceHit> {
                val id = source.thumbs.entries.single { it.value === bmp }.key
                return listOf(FaceHit(RectF(0f, 0f, bmp.width * 0.6f, bmp.height * 0.7f), embeddings.getValue(id)))
            }
        }

        pipeline().runIncremental { _, _ -> }

        suspend fun person(id: Long) = db.peopleDao().facesFor(id).single().personId
        assertThat(person(1)).isNotNull()
        assertThat(person(2)).isEqualTo(person(1))
        assertThat(person(3)).isNotEqualTo(person(1))
        assertThat(db.peopleDao().people().first()).hasSize(2)
        assertThat(labels(1)).contains(Category.SELFIES)
    }

    @Test
    fun pipeline_leftoverInflightMarker_marksItemUnreadable() = runBlocking<Unit> {
        source.add(item(1))
        source.add(item(2))
        // Simulate a native crash while analysing item 2: its row is PENDING and the marker was left behind.
        db.mediaDao().upsert(MediaRow(2, item(2).path, MediaKind.IMAGE, 1002, "image/jpeg", 1920, 1080, 2000, null, 1, ScanStatus.PENDING, city = "Kept"))
        settings.setInflightItem(2)

        pipeline().runIncremental { _, _ -> }

        assertThat(status(1)).isEqualTo(ScanStatus.DONE)
        assertThat(status(2)).isEqualTo(ScanStatus.UNREADABLE)
        assertThat(db.mediaDao().get(2)!!.city).isEqualTo("Kept") // previous fields preserved
        assertThat(source.thumbnailCalls).doesNotContain(2L)
        assertThat(settings.inflightItem()).isNull()
    }

    @Test
    fun pipeline_classifierFailure_leavesPendingAndContinues() = runBlocking<Unit> {
        listOf(item(1), item(2), item(3)).forEach { source.add(it) }
        nsfw.failing += source.thumbs.getValue(2)

        pipeline().runIncremental { _, _ -> }

        assertThat(status(1)).isEqualTo(ScanStatus.DONE)
        assertThat(status(2)).isEqualTo(ScanStatus.PENDING)
        assertThat(status(3)).isEqualTo(ScanStatus.DONE)
        assertThat(settings.inflightItem()).isNull()

        nsfw.failing.clear() // transient: the next run retries it
        pipeline().runIncremental { _, _ -> }
        assertThat(status(2)).isEqualTo(ScanStatus.DONE)
    }

    @Test
    fun pipeline_threeConsecutiveClassifierFailures_abortRun() = runBlocking<Unit> {
        (1L..5L).forEach { source.add(item(it)) }
        (1L..3L).forEach { nsfw.failing += source.thumbs.getValue(it) }

        val error = runCatching { pipeline().runIncremental { _, _ -> } }.exceptionOrNull()

        assertThat(error).isInstanceOf(ScanAbortedException::class.java)
        assertThat(source.thumbnailCalls).containsExactly(1L, 2L, 3L)
        (1L..5L).forEach { assertThat(status(it)).isEqualTo(ScanStatus.PENDING) }
    }

    @Test
    fun pipeline_errorDuringAnalysis_keepsInflightMarkerForNextRun() = runBlocking<Unit> {
        source.add(item(1))
        source.add(item(2))
        nsfw.erroring += source.thumbs.getValue(1)

        val error = runCatching { pipeline().runIncremental { _, _ -> } }.exceptionOrNull()

        assertThat(error).isInstanceOf(OutOfMemoryError::class.java)
        assertThat(settings.inflightItem()).isEqualTo(1L)
        assertThat(status(1)).isEqualTo(ScanStatus.PENDING)

        pipeline().runIncremental { _, _ -> } // the "restarted process"
        assertThat(status(1)).isEqualTo(ScanStatus.UNREADABLE)
        assertThat(status(2)).isEqualTo(ScanStatus.DONE)
        assertThat(settings.inflightItem()).isNull()
    }

    @Test
    fun pipeline_alwaysFailingItems_becomeUnreadableAfterThreeRuns() = runBlocking<Unit> {
        (1L..4L).forEach { source.add(item(it)) }
        (1L..3L).forEach { nsfw.failing += source.thumbs.getValue(it) }

        repeat(3) { runCatching { pipeline().runIncremental { _, _ -> } } }

        (1L..3L).forEach { assertThat(status(it)).isEqualTo(ScanStatus.UNREADABLE) }
        assertThat(status(4)).isEqualTo(ScanStatus.DONE)
        assertThat(db.mediaDao().get(4)!!.analysisFailures).isEqualTo(0)

        // A new generation resets the count and is retried.
        nsfw.failing.clear()
        source.add(item(1, generation = 2))
        pipeline().runIncremental { _, _ -> }
        assertThat(status(1)).isEqualTo(ScanStatus.DONE)
        assertThat(db.mediaDao().get(1)!!.analysisFailures).isEqualTo(0)
    }

    @Test
    fun pipeline_cancellation_leavesItemPending() = runBlocking<Unit> {
        source.add(item(1))
        nsfw.cancelling += source.thumbs.getValue(1)

        val error = runCatching { pipeline().runIncremental { _, _ -> } }.exceptionOrNull()

        assertThat(error).isInstanceOf(CancellationException::class.java)
        assertThat(status(1)).isEqualTo(ScanStatus.PENDING)
        assertThat(settings.inflightItem()).isNull()
    }

    @Test
    fun pipeline_rescanChangedItem_keepsExcludedOverrideAndDoesNotDuplicateLabels() = runBlocking<Unit> {
        source.add(item(1))
        nsfw.scores[source.thumbs.getValue(1)] = NsfwScore(0.95f, 0f)
        scenes.labels[source.thumbs.getValue(1)] = mapOf("Dog" to 0.9f)
        pipeline().runIncremental { _, _ -> }
        assertThat(labels(1)).containsAtLeast(Category.OTHER_ADULT, Category.PETS, Category.CAMERA)

        db.labelDao().upsertOverride(OverrideRow(1, Category.OTHER_ADULT, isExcluded = true))
        scenes.labels.clear() // the edited file no longer shows a dog
        source.add(item(1, generation = 2))
        pipeline().runIncremental { _, _ -> }

        val rows = db.labelDao().labelsFor(1)
        assertThat(rows.map { it.category }).containsNoDuplicates()
        assertThat(rows.map { it.category }).containsExactly(Category.CAMERA)
        assertThat(db.labelDao().overridesFor(1)).containsExactly(OverrideRow(1, Category.OTHER_ADULT, true))
    }

    // ---- fakes ----

    private class FakeSource : MediaSource {
        private val items = LinkedHashMap<Long, MediaItem>()
        private val exifs = HashMap<Long, ExifData?>()
        private val contents = HashMap<Long, ByteArray>()
        val thumbs = HashMap<Long, Bitmap>()
        val failingThumbs = mutableSetOf<Long>()
        val thumbnailCalls = mutableListOf<Long>()

        fun add(item: MediaItem, exif: ExifData? = null, content: ByteArray = ByteArray(0)) {
            items[item.id] = item
            exifs[item.id] = exif
            contents[item.id] = content
            thumbs.getOrPut(item.id) { noise(item.id, item.width, item.height) }
        }

        fun clear() = items.clear()

        override suspend fun all() = items.values.toList()
        override suspend fun changedSince(generation: Long) = items.values.filter { it.generation > generation }
        override fun open(item: MediaItem): InputStream = ByteArrayInputStream(contents.getValue(item.id))
        override suspend fun thumbnail(item: MediaItem, px: Int): Bitmap? {
            thumbnailCalls += item.id
            if (item.id in failingThumbs) throw IOException("corrupt")
            return thumbs.getValue(item.id)
        }
        override suspend fun exif(item: MediaItem) = exifs[item.id]

        /** A distinct random-noise thumbnail per item, fitted inside 320 px with the item's aspect. */
        private fun noise(seed: Long, w: Int, h: Int): Bitmap {
            val scale = 320f / maxOf(w, h)
            val tw = maxOf(1, (w * scale).toInt())
            val th = maxOf(1, (h * scale).toInt())
            val r = Random(seed)
            val px = IntArray(tw * th) { 0xFF000000.toInt() or r.nextInt(0x1000000) }
            return Bitmap.createBitmap(px, tw, th, Bitmap.Config.ARGB_8888)
        }
    }

    private class FakeNsfw : NsfwClassifier {
        val scores = HashMap<Bitmap, NsfwScore>()
        val failing = mutableSetOf<Bitmap>()
        val cancelling = mutableSetOf<Bitmap>()
        val erroring = mutableSetOf<Bitmap>()
        override fun score(bmp: Bitmap): NsfwScore {
            if (bmp in erroring) throw OutOfMemoryError("simulated")
            if (bmp in failing) throw IllegalStateException("inference failed")
            if (bmp in cancelling) throw CancellationException("stopped")
            return scores[bmp] ?: NsfwScore(0.01f, 0.01f)
        }
    }

    private class FakeText : TextReader {
        var calls = 0
        override suspend fun read(bmp: Bitmap): String { calls++; return "" }
    }

    private class FakeScenes : SceneLabeler {
        val labels = HashMap<Bitmap, Map<String, Float>>()
        override suspend fun labels(bmp: Bitmap) = labels[bmp] ?: emptyMap()
    }

    private object NoFaces : FaceEngine {
        override suspend fun faces(bmp: Bitmap) = emptyList<FaceHit>()
    }

    private object NoQr : QrDetector {
        override suspend fun hasCode(bmp: Bitmap) = false
    }

    private object NoFrames : FrameSource {
        override suspend fun frames(item: MediaItem, timesMs: List<Long>, px: Int) = timesMs.map { null }
    }

    /** A one-city index (Mumbai) in the cities.bin format. */
    private fun tinyCityIndex(): CityIndex {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { d ->
            d.writeInt(1)
            d.writeFloat(19.076f); d.writeFloat(72.8777f); d.writeUTF("Mumbai"); d.writeUTF("IN")
        }
        return CityIndex(ByteArrayInputStream(out.toByteArray()))
    }
}
