package com.aks.gallerysorter.ui.people

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.index.dao.PersonSummary
import com.aks.gallerysorter.index.dao.visiblePeople
import com.aks.gallerysorter.ui.recategorize
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PeopleViewModel(private val graph: AppGraph) : ViewModel() {
    /** Null until loaded (and again right after a lock, so no stale cover is shown). */
    val people: StateFlow<List<PersonSummary>?> = graph.lockState.includeLocked.flatMapLatest { inc ->
        flow<List<PersonSummary>?> {
            if (!inc) emit(null)
            emitAll(graph.db.peopleDao().visiblePeople(inc))
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    init {
        viewModelScope.launch { graph.lockState.includeLocked.drop(1).collect { clearSelection() } }
    }

    fun toggle(id: Long) {
        _selection.value = _selection.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    fun rename(id: Long, name: String) = write("Couldn't rename") {
        val dao = graph.db.peopleDao()
        dao.person(id)?.let { dao.updatePerson(it.copy(name = name.trim().ifEmpty { null })) }
    }

    fun markMe(id: Long) = write("Couldn't mark as you") {
        val dao = graph.db.peopleDao()
        val old = dao.people().first().filter { it.isMe }.map { it.id }
        dao.setMe(id)
        recategorize(graph, dao.mediaIdsOf(old + id))
        graph.notify("Marked as you; affected items are being re-sorted")
    }

    /** Merges every selected person into [into] (the named one, if the user picked it). */
    fun merge(ids: Set<Long>, into: Long) = write("Couldn't merge") {
        val dao = graph.db.peopleDao()
        val meInvolved = ids.any { dao.person(it)?.isMe == true }
        for (from in ids - into) dao.merge(from, into)
        if (meInvolved) recategorize(graph, dao.mediaIdsOf(listOf(into)))
        graph.notify("Merged ${ids.size} people")
    }

    private fun write(failure: String, block: suspend () -> Unit) {
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "people update failed: ${e.javaClass.name}")
                graph.notify(failure)
            }
        }
    }

    private companion object {
        const val TAG = "PeopleViewModel"
    }
}
