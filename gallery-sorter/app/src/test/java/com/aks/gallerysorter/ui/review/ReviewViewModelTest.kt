package com.aks.gallerysorter.ui.review

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.OverrideRow
import com.aks.gallerysorter.lock.LockState
import com.aks.gallerysorter.ui.TestIndex
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReviewViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase
    private val lockState = LockState()

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = TestIndex.db()
        runBlocking {
            db.mediaDao().upsertAll(listOf(TestIndex.media(1), TestIndex.media(2), TestIndex.media(3)))
            db.labelDao().upsertAll(
                listOf(
                    // Item 2 is the newest, so it comes first.
                    LabelRow(2, Category.OTHER_ADULT, 0.6f), LabelRow(2, Category.REVIEW, 0.6f), LabelRow(2, Category.CAMERA, 1f),
                    LabelRow(1, Category.BANKING, 0.7f), LabelRow(1, Category.REVIEW, 0.7f),
                    LabelRow(3, Category.CAMERA, 1f),
                ),
            )
        }
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun vm() = ReviewViewModel(db = { db }, lockState = lockState)

    @Test fun locked_showsNothing() = runBlocking<Unit> {
        val state = withTimeout(10_000) { vm().state.first { it.loaded } }
        assertThat(state.locked).isTrue()
        assertThat(state.item).isNull()
    }

    @Test fun confirm_writesIncludedOverride_andLeavesTheQueue() = runBlocking<Unit> {
        TestIndex.unlock(lockState, tmp.newFolder("keys"))
        val vm = vm()
        val first = withTimeout(10_000) { vm.state.first { it.item != null } }
        assertThat(first.item!!.row.id).isEqualTo(2L)
        assertThat(first.item!!.suggested).isEqualTo(Category.OTHER_ADULT)
        assertThat(first.remaining).isEqualTo(2)

        vm.confirm().join()

        assertThat(db.labelDao().overridesFor(2)).containsExactly(OverrideRow(2, Category.OTHER_ADULT, isExcluded = false))
        // Re-categorized at once: the REVIEW label is gone, the confirmed category stays.
        assertThat(db.labelDao().labelsFor(2).map { it.category }).containsExactly(Category.OTHER_ADULT, Category.CAMERA)
        val next = withTimeout(10_000) { vm.state.first { it.item?.row?.id == 1L } }
        assertThat(next.item!!.suggested).isEqualTo(Category.BANKING)
        assertThat(next.remaining).isEqualTo(1)
    }

    @Test fun reject_writesExcludedOverride_andRemovesTheLabels() = runBlocking<Unit> {
        TestIndex.unlock(lockState, tmp.newFolder("keys"))
        val vm = vm()
        withTimeout(10_000) { vm.state.first { it.item != null } }

        vm.reject().join()

        assertThat(db.labelDao().overridesFor(2)).containsExactly(OverrideRow(2, Category.OTHER_ADULT, isExcluded = true))
        assertThat(db.labelDao().labelsFor(2).map { it.category }).containsExactly(Category.CAMERA)
        val done = withTimeout(10_000) { vm.state.first { it.item?.row?.id == 1L } }
        assertThat(done.remaining).isEqualTo(1)
    }
}
