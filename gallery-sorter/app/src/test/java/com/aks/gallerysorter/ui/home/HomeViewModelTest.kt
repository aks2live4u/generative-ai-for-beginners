package com.aks.gallerysorter.ui.home

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.lock.LockState
import com.aks.gallerysorter.scan.ScanSettings
import com.aks.gallerysorter.vault.Argon2Params
import com.aks.gallerysorter.vault.FakeKeyWrapper
import com.aks.gallerysorter.vault.VaultKeys
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
class HomeViewModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private val lockState = LockState()

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        runBlocking {
            db.mediaDao().upsertAll(listOf(row(1), row(2), row(3)))
            db.labelDao().upsertAll(
                listOf(
                    LabelRow(1, Category.MY_NUDES, 0.95f), LabelRow(1, Category.CAMERA, 1f),
                    LabelRow(2, Category.MY_NUDES, 0.97f), LabelRow(2, Category.CAMERA, 1f),
                    LabelRow(3, Category.CAMERA, 1f),
                ),
            )
        }
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun row(id: Long) = MediaRow(
        id = id, path = "/storage/emulated/0/DCIM/Camera/IMG_$id.jpg", kind = MediaKind.IMAGE, sizeBytes = 10,
        mime = "image/jpeg", width = 10, height = 10, dateTakenMs = id, durationMs = null, generation = 1,
        status = ScanStatus.DONE,
    )

    private fun viewModel() = HomeViewModel(
        db = { db }, scanSettings = { ScanSettings(db.settingsDao(), deviceModel = "test") }, lockState = lockState,
    )

    private fun tile(state: HomeState, c: Category) = state.groups.flatMap { it.tiles }.single { it.category == c }

    @Test fun locked_sensitiveTileHasCountButNoPreview() = runBlocking<Unit> {
        val state = withTimeout(10_000) { viewModel().state.first { it.loaded } }

        val nudes = tile(state, Category.MY_NUDES)
        assertThat(nudes.count).isEqualTo(2)
        assertThat(nudes.locked).isTrue()
        assertThat(nudes.previewIds).isEmpty()
        // Unlocked categories never preview a locked item either.
        assertThat(tile(state, Category.CAMERA).previewIds).containsExactly(3L)
    }

    @Test fun unlocked_sensitiveTileShowsPreview() = runBlocking<Unit> {
        val keys = VaultKeys(tmp.newFolder("keys"), FakeKeyWrapper(), Argon2Params(1, 1024, 1))
        keys.create("123456".toCharArray())
        val vm = viewModel()
        withTimeout(10_000) { vm.state.first { it.loaded } }

        lockState.unlock(VaultTier.STANDARD, keys.unlockWithSecret("123456".toCharArray()))
        val state = withTimeout(10_000) { vm.state.first { it.loaded && !tile(it, Category.MY_NUDES).locked } }

        assertThat(tile(state, Category.MY_NUDES).previewIds).containsExactly(2L)
        assertThat(tile(state, Category.CAMERA).previewIds).containsExactly(3L)
    }

    /**
     * stateIn(WhileSubscribed) replays its last value to a new collector. After a lock while Home
     * was off-screen, that replay must not carry the unlocked previews, not even for one frame.
     */
    @Test fun replayAfterLock_neverShowsPreviews() = runBlocking<Unit> {
        val keys = VaultKeys(tmp.newFolder("keys"), FakeKeyWrapper(), Argon2Params(1, 1024, 1))
        keys.create("123456".toCharArray())
        val vm = viewModel()
        lockState.unlock(VaultTier.STANDARD, keys.unlockWithSecret("123456".toCharArray()))
        withTimeout(10_000) { vm.state.first { it.loaded && tile(it, Category.MY_NUDES).previewIds.isNotEmpty() } }

        lockState.lockAll()
        // The very first value a new collector sees, before any re-query.
        val replayed = vm.state.first()

        assertThat(replayed.unlocked).isFalse()
        val nudes = tile(replayed, Category.MY_NUDES)
        assertThat(nudes.locked).isTrue()
        assertThat(nudes.previewIds).isEmpty()
        // Previews of open tiles were computed with locked items included, so they go too.
        assertThat(replayed.groups.flatMap { it.tiles }.flatMap { it.previewIds }).isEmpty()
        assertThat(nudes.count).isEqualTo(2)
    }
}
