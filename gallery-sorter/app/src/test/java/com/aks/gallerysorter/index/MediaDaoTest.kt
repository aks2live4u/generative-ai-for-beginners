package com.aks.gallerysorter.index

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.OverrideRow
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun media(id: Long) = MediaRow(
        id = id, path = "/sdcard/DCIM/$id.jpg", kind = MediaKind.IMAGE, sizeBytes = 1000,
        mime = "image/jpeg", width = 100, height = 100, dateTakenMs = id, durationMs = null,
        generation = 1, status = ScanStatus.DONE,
    )

    private fun ids(unlocked: Boolean, category: Category): List<Long> = runBlocking {
        val src = db.labelDao().itemsIn(category, includeLocked = unlocked)
        val res = src.load(PagingSource.LoadParams.Refresh(null, 50, false))
        (res as PagingSource.LoadResult.Page).data.map { it.id }
    }

    @Test
    fun upsert_thenItemsIn_returnsItem() { runBlocking {
        db.mediaDao().upsert(media(1))
        db.labelDao().upsertAll(listOf(LabelRow(1, Category.CAMERA, 0.95f)))
        assertThat(ids(false, Category.CAMERA)).containsExactly(1L)
        assertThat(db.labelDao().counts().first()).containsExactly(Category.CAMERA, 1)
    } }

    @Test
    fun itemsIn_lockedExcludesSensitiveDocsReview() {
        runBlocking {
            db.mediaDao().upsert(media(1)) // adult + camera
            db.mediaDao().upsert(media(2)) // camera only
            db.mediaDao().upsert(media(3)) // camera + doc
            db.mediaDao().upsert(media(4)) // camera + review-band confidence
            db.mediaDao().upsert(media(5)) // camera + REVIEW label
            db.labelDao().upsertAll(
                listOf(
                    LabelRow(1, Category.OTHER_ADULT, 0.9f), LabelRow(1, Category.CAMERA, 0.95f),
                    LabelRow(2, Category.CAMERA, 0.95f),
                    LabelRow(3, Category.CAMERA, 0.95f), LabelRow(3, Category.BANKING, 0.9f),
                    LabelRow(4, Category.CAMERA, 0.95f), LabelRow(4, Category.NATURE, 0.6f),
                    LabelRow(5, Category.CAMERA, 0.95f), LabelRow(5, Category.REVIEW, 0.7f),
                )
            )
        }
        assertThat(ids(false, Category.CAMERA)).containsExactly(2L)
        assertThat(ids(true, Category.CAMERA)).containsExactly(1L, 2L, 3L, 4L, 5L)
    }

    @Test
    fun override_excludedCategory_neverReturned() { runBlocking {
        db.mediaDao().upsert(media(1))
        db.labelDao().upsertAll(listOf(LabelRow(1, Category.CAMERA, 0.95f)))
        db.labelDao().upsertOverride(OverrideRow(1, Category.CAMERA, isExcluded = true))
        assertThat(ids(true, Category.CAMERA)).isEmpty()
        assertThat(ids(false, Category.CAMERA)).isEmpty()
    } }
}
