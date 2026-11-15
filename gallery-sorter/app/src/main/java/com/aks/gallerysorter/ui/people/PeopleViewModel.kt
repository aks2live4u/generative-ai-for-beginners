package com.aks.gallerysorter.ui.people

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.dao.PersonSummary
import com.aks.gallerysorter.index.dao.visiblePeople
import com.aks.gallerysorter.lock.LockState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Face clusters: name, merge, mark "Me". Only people with a visible item are listed, and covers
 * are visible items (lock filter). Changing who is "Me" re-categorizes the items of the old and
 * new "Me" (My nudes → Of me depends on it); a merge that moves "Me" does the same.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModel(
    private val db: () -> AppDatabase,
    lockState: LockState,
    private val recategorize: suspend (Collection<Long>) -> Unit,
    private val notify: (String) -> Unit,
) : ViewModel() {
    /** A list and the lock state it was queried under. */
    private class Built(val includeLocked: Boolean, val people: List<PersonSummary>)

    private val cached: StateFlow<Built?> = lockState.includeLocked.flatMapLatest { inc ->
        flow<Built?> {
            if (!inc) emit(null)
            emitAll(db().peopleDao().visiblePeople(inc).map { Built(inc, it) })
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The people to show; null until loaded. stateIn replays its last value to a new collector,
     * which can be a list built while unlocked (Sensitive covers) after a lock that happened while
     * People was on the back stack, so a list is only shown under the lock state it was built for.
     */
    val people: Flow<List<PersonSummary>?> = combine(cached, lockState.includeLocked) { b, inc ->
        b?.takeIf { it.includeLocked == inc }?.people
    }

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    init {
        viewModelScope.launch { lockState.includeLocked.drop(1).collect { clearSelection() } }
    }

    fun toggle(id: Long) {
        _selection.value = _selection.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    fun rename(id: Long, name: String) = write("Couldn't rename") {
        val dao = db().peopleDao()
        dao.person(id)?.let { dao.updatePerson(it.copy(name = name.trim().ifEmpty { null })) }
    }

    fun markMe(id: Long) = write("Couldn't mark as you") {
        val dao = db().peopleDao()
        val old = dao.people().first().filter { it.isMe }.map { it.id }
        dao.setMe(id)
        recategorize(dao.mediaIdsOf(old + id))
        notify("Marked as you; affected items are being re-sorted")
    }

    /** Merges every person in [ids] into [into] (the one the user picked), all in one transaction. */
    fun merge(ids: Set<Long>, into: Long) = write("Couldn't merge") {
        val d = db()
        val dao = d.peopleDao()
        val meInvolved = d.withTransaction {
            val me = ids.any { dao.person(it)?.isMe == true }
            for (from in ids - into) dao.merge(from, into)
            me
        }
        if (meInvolved) recategorize(dao.mediaIdsOf(listOf(into)))
        notify("Merged ${ids.size} people")
    }

    private fun write(failure: String, block: suspend () -> Unit): Job {
        clearSelection()
        return viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "people update failed: ${e.javaClass.name}")
                notify(failure)
            }
        }
    }

    private companion object {
        const val TAG = "PeopleViewModel"
    }
}
