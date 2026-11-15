package com.aks.gallerysorter.ui.places

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.dao.CityCount
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.lock.LockState
import com.aks.gallerysorter.places.Trip
import com.aks.gallerysorter.scan.ScanSettings
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
class PlacesViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase
    private val lockState = LockState()
    private val goa = Trip("Goa, Mar 2026", 100, 200, listOf(4L, 5L))
    private val pune = Trip("Pune, Apr 2026", 300, 400, listOf(6L))

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = TestIndex.db()
        runBlocking {
            db.mediaDao().upsertAll(
                listOf(
                    TestIndex.media(1, "Mumbai"), TestIndex.media(2, "Mumbai"), TestIndex.media(3, "Mumbai"),
                    TestIndex.media(4, "Goa"), TestIndex.media(5, "Goa"), TestIndex.media(6, "Pune"), TestIndex.media(7),
                ),
            )
            // Items 3 and 6 are Sensitive; 5 is in the Review band.
            db.labelDao().upsertAll(
                listOf(LabelRow(3, Category.MY_NUDES, 0.9f), LabelRow(6, Category.IDS, 0.9f), LabelRow(5, Category.REVIEW, 0.6f)),
            )
            ScanSettings(db.settingsDao(), deviceModel = "test").setTrips(listOf(goa, pune))
        }
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun vm() = PlacesViewModel(db = { db }, scanSettings = { ScanSettings(db.settingsDao(), deviceModel = "test") }, lockState = lockState)

    @Test fun locked_countsOnlyVisibleItems() = runBlocking<Unit> {
        val s = withTimeout(10_000) { vm().state.first { it.loaded } }
        assertThat(s.cities).containsExactly(CityCount("Mumbai", 2), CityCount("Goa", 1)).inOrder()
        // A trip with no visible item is left out.
        assertThat(s.trips.map { it.trip.name to it.count }).containsExactly(goa.name to 1)
    }

    @Test fun unlocked_groupsByCityByCount_andListsEveryTrip() = runBlocking<Unit> {
        TestIndex.unlock(lockState, tmp.newFolder("keys"))
        val s = withTimeout(10_000) { vm().state.first { it.loaded && it.cities.size == 3 } }
        assertThat(s.cities).containsExactly(CityCount("Mumbai", 3), CityCount("Goa", 2), CityCount("Pune", 1)).inOrder()
        assertThat(s.trips.map { it.trip.name to it.count }).containsExactly(goa.name to 2, pune.name to 1).inOrder()
    }
}
