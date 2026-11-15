package com.aks.gallerysorter.ui.dupes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.dao.DupeItem
import com.aks.gallerysorter.lock.LockState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * One duplicate group; [keep] is what the user keeps (initially the suggested keep).
 * [needsConfirm]: more than [LARGE_GROUP] items and not yet confirmed by the user, so it is left
 * out of "Delete N suggested".
 */
data class DupeGroupUi(val id: Long, val items: List<DupeItem>, val keep: Set<Long>, val needsConfirm: Boolean = false)

/** Groups larger than this need an extra confirmation before their copies are deleted. */
const val LARGE_GROUP = 10

/** [missingKeep]: groups where nothing is kept; deleting is blocked until each keeps at least one. */
data class DupesState(val loaded: Boolean = false, val groups: List<DupeGroupUi> = emptyList()) {
    val missingKeep: Set<Long> get() = groups.filter { it.keep.isEmpty() }.map { it.id }.toSet()
}

/**
 * Duplicate groups side by side. While locked, a group with any lockable member is hidden
 * (DupeDao.visibleGroups). The user can change what is kept; [toDelete] is everything else
 * in the visible groups, and is only ever deleted through the system trash dialog.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DuplicatesViewModel(db: () -> AppDatabase, lockState: LockState) : ViewModel() {
    /** User changes to the suggested keep, per group. */
    private val keepChoice = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())

    /** Large groups the user confirmed for deletion. */
    private val confirmedLarge = MutableStateFlow<Set<Long>>(emptySet())

    val state: StateFlow<DupesState> = combine(
        lockState.includeLocked.flatMapLatest { inc ->
            flow<List<DupeItem>?> {
                if (!inc) emit(null) // a lock drops the unlocked groups before the re-query returns
                emitAll(db().dupeDao().visibleGroups(inc))
            }
        }.flowOn(Dispatchers.IO),
        keepChoice,
        confirmedLarge,
    ) { rows, choice, confirmed ->
        if (rows == null) return@combine DupesState()
        DupesState(
            loaded = true,
            groups = rows.groupBy { it.groupId }.map { (id, items) ->
                DupeGroupUi(
                    id, items, choice[id] ?: items.filter { it.suggestedKeep }.map { it.media.id }.toSet(),
                    needsConfirm = items.size > LARGE_GROUP && id !in confirmed,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DupesState())

    fun toggleKeep(groupId: Long, mediaId: Long) {
        val group = state.value.groups.firstOrNull { it.id == groupId } ?: return
        val keep = if (mediaId in group.keep) group.keep - mediaId else group.keep + mediaId
        keepChoice.value = keepChoice.value + (groupId to keep)
    }

    /** After the extra confirmation: group [groupId]'s copies count towards "Delete N suggested". */
    fun includeLargeGroup(groupId: Long) {
        confirmedLarge.value = confirmedLarge.value + groupId
    }

    /**
     * The visible members not kept ("Delete N suggested"), leaving out unconfirmed large groups;
     * empty while any group keeps nothing.
     */
    fun toDelete(): List<Long> {
        val s = state.value
        if (s.missingKeep.isNotEmpty()) return emptyList()
        return s.groups.filterNot { it.needsConfirm }.flatMap { g -> g.items.map { it.media.id }.filter { it !in g.keep } }
    }
}
