package com.aks.gallerysorter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryGroup
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.lock.LockState
import com.aks.gallerysorter.scan.ScanSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * One category tile. [locked]: a Sensitive/Documents tile while no vault is unlocked; it then
 * shows its count behind a lock and [previewIds] is always empty (never a thumbnail).
 */
data class CategoryTile(val category: Category, val count: Int, val locked: Boolean, val previewIds: List<Long>)

data class TileGroup(val group: CategoryGroup, val tiles: List<CategoryTile>)

data class HomeState(
    val loaded: Boolean = false,
    val groups: List<TileGroup> = emptyList(),
    val people: Int = 0,
    val cities: Int = 0,
    val trips: Int = 0,
    val duplicateGroups: Int = 0,
    val review: Int = 0,
    val unlocked: Boolean = false,
)

/** Home: category tiles by group with counts, plus the People/Places/Duplicates/Review/Vault entries. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    db: () -> AppDatabase,
    private val scanSettings: () -> ScanSettings,
    lockState: LockState,
) : ViewModel() {
    /** Resolved on IO: the lazy database must never be opened from the main thread. */
    private val labelDao by lazy { db().labelDao() }

    private data class Inputs(val counts: Map<Category, Int>, val unlocked: Boolean, val people: Int, val cities: Int, val dupes: Int)

    val state: StateFlow<HomeState> = flow {
        val d = db()
        emitAll(
            combine(
                d.labelDao().counts(), lockState.includeLocked, d.peopleDao().people(), d.mediaDao().cityCount(), d.dupeDao().groupCount(),
            ) { counts, unlocked, people, cities, dupes -> Inputs(counts, unlocked, people.size, cities, dupes) },
        )
    }
        // Re-queried whenever counts or the lock change, so a lock drops previews at once.
        .mapLatest { build(it) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private suspend fun build(i: Inputs): HomeState {
        val groups = TILE_GROUPS.map { group ->
            TileGroup(
                group,
                Category.entries.filter { it.group == group && it != Category.DUPLICATES }.map { c ->
                    val locked = c in Category.lockedExcluded && !i.unlocked
                    val previews = if (locked) emptyList() else labelDao.previewIds(c, i.unlocked, PREVIEWS)
                    CategoryTile(c, i.counts[c] ?: 0, locked, previews)
                },
            )
        }
        return HomeState(
            loaded = true, groups = groups, people = i.people, cities = i.cities,
            trips = scanSettings().trips().size, duplicateGroups = i.dupes,
            review = i.counts[Category.REVIEW] ?: 0, unlocked = i.unlocked,
        )
    }

    private companion object {
        const val PREVIEWS = 1
        val TILE_GROUPS = listOf(
            CategoryGroup.SENSITIVE, CategoryGroup.DOCUMENTS, CategoryGroup.SOURCE, CategoryGroup.CONTENT, CategoryGroup.CLEANUP,
        )
    }
}
