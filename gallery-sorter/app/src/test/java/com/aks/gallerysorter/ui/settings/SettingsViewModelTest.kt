package com.aks.gallerysorter.ui.settings

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.scan.ScanSettings
import com.aks.gallerysorter.ui.TestIndex
import com.aks.gallerysorter.vault.TierSettings
import com.aks.gallerysorter.vault.defaultTierFor
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = TestIndex.db()
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun vm() = SettingsViewModel(db = { db })

    @Test fun defaults_areTheBuiltInTiers_andAutoHideOff() = runBlocking<Unit> {
        val s = withTimeout(10_000) { vm().state.first { it.loaded } }
        assertThat(s.tiers[Category.MY_NUDES]).isEqualTo(VaultTier.HIGH_SECURITY)
        assertThat(s.tiers[Category.OTHER_ADULT]).isEqualTo(VaultTier.STANDARD)
        assertThat(s.tiers.keys).containsExactlyElementsIn(Category.lockedExcluded)
        assertThat(s.autoHide).isFalse()
        assertThat(s.onlyWhileCharging).isTrue()
        assertThat(s.longStandardPin).isFalse()
    }

    @Test fun defaultTier_roundTripsThroughTheIndex() = runBlocking<Unit> {
        val vm = vm()
        withTimeout(10_000) { vm.state.first { it.loaded } }
        vm.setTier(Category.OTHER_ADULT, VaultTier.HIGH_SECURITY).join()
        vm.setTier(Category.IDS, VaultTier.STANDARD).join()

        // A fresh ViewModel reads it back, and the hide/auto-hide default uses it.
        val again = withTimeout(10_000) { vm().state.first { it.loaded } }
        assertThat(again.tiers[Category.OTHER_ADULT]).isEqualTo(VaultTier.HIGH_SECURITY)
        assertThat(again.tiers[Category.IDS]).isEqualTo(VaultTier.STANDARD)
        val overrides = TierSettings(db.settingsDao()).overrides()
        assertThat(defaultTierFor(listOf(Category.OTHER_ADULT), overrides)).isEqualTo(VaultTier.HIGH_SECURITY)
        assertThat(defaultTierFor(listOf(Category.IDS), overrides)).isEqualTo(VaultTier.STANDARD)
    }

    @Test fun toggles_persist() = runBlocking<Unit> {
        val vm = vm()
        withTimeout(10_000) { vm.state.first { it.loaded } }
        vm.setAutoHide(true).join()
        vm.setOnlyWhileCharging(false).join()
        vm.setLongStandardPin(true).join()

        val scan = ScanSettings(db.settingsDao(), deviceModel = "test")
        assertThat(scan.autoHideSensitive()).isTrue()
        assertThat(scan.onlyWhileCharging()).isFalse()
        assertThat(withTimeout(10_000) { vm().state.first { it.loaded } }.longStandardPin).isTrue()
    }
}
