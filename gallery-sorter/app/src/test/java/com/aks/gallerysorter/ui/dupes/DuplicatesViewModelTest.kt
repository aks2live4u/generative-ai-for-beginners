package com.aks.gallerysorter.ui.dupes

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.DupeRow
import com.aks.gallerysorter.index.entities.LabelRow
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
class DuplicatesViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase
    private val lockState = LockState()

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = TestIndex.db()
        runBlocking {
            db.mediaDao().upsertAll((1L..5L).map { TestIndex.media(it) })
            // Group 10: plain photos. Group 20: one member is Sensitive.
            db.dupeDao().replaceAll(
                listOf(
                    DupeRow(10, 1, suggestedKeep = true), DupeRow(10, 2, suggestedKeep = false), DupeRow(10, 3, suggestedKeep = false),
                    DupeRow(20, 4, suggestedKeep = true), DupeRow(20, 5, suggestedKeep = false),
                ),
            )
            db.labelDao().upsertAll(listOf(LabelRow(5, Category.OTHER_ADULT, 0.9f)))
        }
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun vm() = DuplicatesViewModel(db = { db }, lockState = lockState)

    @Test fun locked_hidesGroupsWithALockableItem() = runBlocking<Unit> {
        val s = withTimeout(10_000) { vm().state.first { it.loaded } }
        assertThat(s.groups.map { it.id }).containsExactly(10L)
        assertThat(s.groups.single().items.map { it.media.id }).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test fun unlocked_showsEveryGroup_andDeletesTheSuggested() = runBlocking<Unit> {
        TestIndex.unlock(lockState, tmp.newFolder("keys"))
        val vm = vm()
        val s = withTimeout(10_000) { vm.state.first { it.loaded && it.groups.size == 2 } }
        assertThat(s.groups.first { it.id == 10L }.keep).containsExactly(1L)
        assertThat(vm.toDelete()).containsExactly(2L, 3L, 5L)

        // The user keeps 3 as well and swaps group 20's keep.
        vm.toggleKeep(10, 3)
        vm.toggleKeep(20, 5)
        vm.toggleKeep(20, 4)
        assertThat(vm.toDelete()).containsExactly(2L, 4L)
    }

    @Test fun lock_dropsTheSensitiveGroupAgain() = runBlocking<Unit> {
        TestIndex.unlock(lockState, tmp.newFolder("keys"))
        val vm = vm()
        withTimeout(10_000) { vm.state.first { it.groups.size == 2 } }
        lockState.lockAll()
        val s = withTimeout(10_000) { vm.state.first { it.groups.size == 1 } }
        assertThat(s.groups.single().id).isEqualTo(10L)
        assertThat(vm.toDelete()).containsExactly(2L, 3L)
    }

    @Test fun aGroupWithNothingKept_blocksTheDelete() = runBlocking<Unit> {
        val vm = vm()
        withTimeout(10_000) { vm.state.first { it.loaded && it.groups.isNotEmpty() } }
        vm.toggleKeep(10, 1) // un-keep the only kept copy

        assertThat(vm.state.value.missingKeep).containsExactly(10L)
        assertThat(vm.toDelete()).isEmpty()

        vm.toggleKeep(10, 2)
        assertThat(vm.state.value.missingKeep).isEmpty()
        assertThat(vm.toDelete()).containsExactly(1L, 3L)
    }
}
