package com.aks.gallerysorter.ui.vault

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryGroup
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.entities.VaultRow
import com.aks.gallerysorter.ui.label
import com.aks.gallerysorter.vault.PrivateHandle
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The vault's two tiers: rows, unlock state, and Unhide / Move tier (through the one HideManager). */
class VaultViewModel(private val graph: AppGraph) : ViewModel() {
    val unlocked: StateFlow<Set<VaultTier>> = graph.lockState.unlocked

    val rows: Map<VaultTier, StateFlow<List<VaultRow>>> = VaultTier.entries.associateWith { tier ->
        graph.db.vaultDao().byTier(tier).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    private val _busy = MutableStateFlow<String?>(null)
    val busy: StateFlow<String?> = _busy.asStateFlow()

    init {
        viewModelScope.launch { unlocked.drop(1).collect { clearSelection() } }
    }

    fun exists(tier: VaultTier) = graph.guard.keys(tier).isCreated()

    fun toggle(id: Long) {
        _selection.value = _selection.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    /** True if any selected row was Sensitive: unhiding it needs the visibility warning (spec 6.5). */
    fun anySensitive(tier: VaultTier, ids: Set<Long>): Boolean =
        rows.getValue(tier).value.filter { it.id in ids }.any { isSensitive(it) }

    fun unhide(tier: VaultTier, ids: List<Long>) = run("Restoring", tier) { priv ->
        val res = graph.hideManager.unhide(ids, priv)
        "Restored ${res.restored}" + if (res.failed.isNotEmpty()) "; ${res.failed.size} failed" else ""
    }

    fun moveTier(from: VaultTier, ids: List<Long>) {
        val to = other(from)
        if (!exists(to)) {
            graph.notify("Set up the ${to.label.lowercase()} vault first")
            return
        }
        run("Moving", from) { priv ->
            val res = graph.hideManager.moveTier(ids, to, priv)
            "Moved ${res.moved} to ${to.label.lowercase()}" +
                (if (res.failed.isNotEmpty()) "; ${res.failed.size} failed" else "") +
                (res.aborted?.let { "; stopped: $it" } ?: "")
        }
    }

    private fun run(verb: String, tier: VaultTier, op: suspend (PrivateHandle) -> String) {
        val priv = graph.lockState.privateHandle(tier) ?: run {
            graph.notify("Unlock the ${tier.label.lowercase()} vault first")
            return
        }
        clearSelection()
        graph.appScope.launch {
            _busy.value = "$verb…"
            try {
                graph.awaitStartup()
                graph.notify(op(priv))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "vault action failed: ${e.javaClass.name}")
                graph.notify("$verb failed: the vault key failed its check")
            } finally {
                _busy.value = null
            }
        }
    }

    companion object {
        private const val TAG = "VaultViewModel"

        fun other(tier: VaultTier) = if (tier == VaultTier.STANDARD) VaultTier.HIGH_SECURITY else VaultTier.STANDARD

        fun isSensitive(row: VaultRow): Boolean = row.categories.split(',').any { name ->
            Category.entries.firstOrNull { it.name == name }?.group == CategoryGroup.SENSITIVE
        }
    }
}
