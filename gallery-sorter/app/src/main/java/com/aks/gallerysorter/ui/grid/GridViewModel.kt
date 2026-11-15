package com.aks.gallerysorter.ui.grid

import android.content.ContentUris
import android.content.IntentSender
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.entities.AlbumItemRow
import com.aks.gallerysorter.index.entities.AlbumRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.OverrideRow
import com.aks.gallerysorter.scan.ScanWorker
import com.aks.gallerysorter.ui.label
import com.aks.gallerysorter.vault.defaultTierFor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A paged grid over a [GridSource] with selection actions. Re-queries when the lock changes
 * (LabelDao.itemsIn with includeLocked); a lock change also clears the selection.
 * Long-running actions (hide) run in the app scope so leaving the screen doesn't stop them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GridViewModel(val source: GridSource, private val graph: AppGraph) : ViewModel() {
    private val lockState = graph.lockState

    val locked: StateFlow<Boolean> = lockState.includeLocked.map { !it && source.requiresUnlock }
        .stateIn(viewModelScope, SharingStarted.Eagerly, !lockState.includeLocked.value && source.requiresUnlock)

    val unlocked: StateFlow<Boolean> get() = lockState.includeLocked

    val items: Flow<PagingData<MediaRow>> = lockState.includeLocked
        .flatMapLatest { inc -> Pager(PagingConfig(pageSize = 60)) { source.paging(graph, inc) }.flow }
        .cachedIn(viewModelScope)

    val albums: StateFlow<List<AlbumRow>> = graph.db.albumDao().albums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    private val _busy = MutableStateFlow<String?>(null)
    /** Progress text of a running action, or null. */
    val busy: StateFlow<String?> = _busy.asStateFlow()

    init {
        viewModelScope.launch { lockState.includeLocked.drop(1).collect { _selection.value = emptySet() } }
    }

    fun toggle(id: Long) {
        _selection.value = _selection.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    suspend fun allIds(): List<Long> = withContext(Dispatchers.IO) { source.ids(graph, lockState.includeLocked.value) }

    /** The hide dialog's preselected tier: the category's default (user setting, else built-in). */
    suspend fun defaultTier(): VaultTier {
        val category = (source as? GridSource.OfCategory)?.category ?: return VaultTier.STANDARD
        return defaultTierFor(listOf(category), graph.tierSettings.overrides())
    }

    fun vaultExists(tier: VaultTier) = graph.guard.keys(tier).isCreated()

    fun hide(ids: List<Long>, tier: VaultTier) {
        clearSelection()
        graph.appScope.launch {
            try {
                graph.awaitStartup()
                _busy.value = "Hiding…"
                val res = graph.hideManager.hide(ids, tier, { done, total -> _busy.value = "Hiding $done of $total" }, priv = lockState.privateHandle(tier))
                graph.notify(
                    "Hid ${res.hidden} in the ${tier.label.lowercase()} vault" +
                        (if (res.failed.isNotEmpty()) "; ${res.failed.size} failed" else "") +
                        (res.aborted?.let { "; stopped: $it" } ?: ""),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "hide failed: ${e.javaClass.name}")
                graph.notify("Couldn't hide: the ${tier.label.lowercase()} vault key failed its check")
            } finally {
                _busy.value = null
            }
        }
    }

    fun addToAlbum(ids: List<Long>, albumId: Long?, newName: String?) {
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            val dao = graph.db.albumDao()
            val target = albumId ?: dao.insert(AlbumRow(name = newName?.trim().orEmpty().ifEmpty { "Album" }))
            dao.addItems(ids.map { AlbumItemRow(target, it) })
            graph.notify("Added ${ids.size} to the album")
        }
    }

    /**
     * "Not this category": an excluded override (never re-classified into it), and the item goes
     * back to PENDING so the next scan re-categorizes it with the override applied.
     */
    fun notThisCategory(ids: List<Long>) {
        val category = (source as? GridSource.OfCategory)?.category ?: return
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            val db = graph.db
            for (id in ids) {
                db.labelDao().upsertOverride(OverrideRow(id, category, isExcluded = true))
                db.mediaDao().setStatus(id, ScanStatus.PENDING)
            }
            graph.notify("Removed ${ids.size} from ${category.label}")
            try {
                ScanWorker.enqueueOnChange(graph.appContext)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "cannot enqueue re-categorize: ${e.javaClass.name}")
            }
        }
    }

    /** The system trash confirmation for [ids] (MediaStore.createTrashRequest); never deletes by itself. */
    suspend fun trashRequest(ids: List<Long>): IntentSender? = withContext(Dispatchers.IO) {
        val uris = ids.mapNotNull { graph.db.mediaDao().get(it) }.map { row ->
            val base = if (row.kind == MediaKind.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ContentUris.withAppendedId(base, row.id)
        }
        if (uris.isEmpty()) null else MediaStore.createTrashRequest(graph.appContext.contentResolver, uris, true).intentSender
    }

    private companion object {
        const val TAG = "GridViewModel"
    }
}
