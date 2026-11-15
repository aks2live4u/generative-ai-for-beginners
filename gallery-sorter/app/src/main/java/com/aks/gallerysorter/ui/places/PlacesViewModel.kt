package com.aks.gallerysorter.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.dao.CityCount
import com.aks.gallerysorter.index.dao.cities
import com.aks.gallerysorter.index.dao.idsOf
import com.aks.gallerysorter.lock.LockState
import com.aks.gallerysorter.places.Trip
import com.aks.gallerysorter.scan.ScanSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A trip and how many of its items are visible now. */
data class TripEntry(val trip: Trip, val count: Int)

data class PlacesState(val loaded: Boolean = false, val cities: List<CityCount> = emptyList(), val trips: List<TripEntry> = emptyList())

/**
 * Places: cities by item count and the trips of the last scan. Both apply the lock filter, and
 * a trip with no visible item is left out while locked. Re-queried when the lock changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlacesViewModel(
    db: () -> AppDatabase,
    scanSettings: () -> ScanSettings,
    lockState: LockState,
) : ViewModel() {
    val state: StateFlow<PlacesState> = lockState.includeLocked.flatMapLatest { inc ->
        flow {
            if (!inc) emit(PlacesState()) // a lock drops the unlocked lists before the re-query returns
            val d = db()
            emitAll(
                d.mediaDao().cities(inc).map { cities ->
                    val trips = scanSettings().trips()
                        .map { TripEntry(it, d.mediaDao().idsOf(it.itemIds, inc).size) }
                        .filter { it.count > 0 }
                    PlacesState(loaded = true, cities = cities, trips = trips)
                },
            )
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, PlacesState())
}
