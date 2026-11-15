package com.aks.gallerysorter.ui.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryGroup
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.OverrideRow
import com.aks.gallerysorter.lock.LockState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The item under review: [suggested] is its most confident Sensitive/Documents label (null if none). */
data class ReviewItem(val row: MediaRow, val suggested: Category?, val labels: List<Category>)

data class ReviewState(
    val loaded: Boolean = false,
    val locked: Boolean = true,
    val item: ReviewItem? = null,
    val remaining: Int = 0,
)

/**
 * The Review queue, one item at a time (newest first). Needs the standard tier unlocked.
 * Confirm → an included override for the suggested category; Reject → excluded overrides for the
 * item's Sensitive/Documents labels. Either way the stored labels are re-categorized at once the
 * way [com.aks.gallerysorter.classify.categorize] treats those overrides (REVIEW is dropped), so a
 * later scan reaches the same result.
 */
class ReviewViewModel(
    private val db: () -> AppDatabase,
    private val lockState: LockState,
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    private val skipped = mutableSetOf<Long>()
    private val mutex = Mutex()

    init {
        // Re-loaded on every lock change; a lock empties the screen at once.
        viewModelScope.launch {
            lockState.unlocked.collect { open ->
                if (VaultTier.STANDARD !in open) _state.value = ReviewState(loaded = true, locked = true)
                load().join()
            }
        }
    }

    fun confirm(): Job = decide { item ->
        val c = item.suggested ?: return@decide
        val d = db()
        d.withTransaction {
            d.labelDao().upsertOverride(OverrideRow(item.row.id, c, isExcluded = false))
            d.labelDao().upsertAll(forcedLabels(item.row.id, c))
            d.labelDao().deleteLabel(item.row.id, Category.REVIEW)
        }
    }

    fun reject(): Job = decide { item ->
        val d = db()
        val rejected = item.labels.filter { it.isReviewable }
        d.withTransaction {
            if (rejected.isEmpty()) {
                // Nothing to reject but the Review flag itself (e.g. a forced REVIEW override).
                d.labelDao().upsertOverride(OverrideRow(item.row.id, Category.REVIEW, isExcluded = true))
            }
            for (c in rejected) {
                d.labelDao().upsertOverride(OverrideRow(item.row.id, c, isExcluded = true))
                d.labelDao().deleteLabel(item.row.id, c)
            }
            d.labelDao().deleteLabel(item.row.id, Category.REVIEW)
        }
    }

    /** Leaves the item in the queue and shows the next one (until the screen is reopened). */
    fun skip(): Job = viewModelScope.launch(Dispatchers.IO) {
        mutex.withLock { _state.value.item?.let { skipped += it.row.id } }
        load().join()
    }

    private fun decide(write: suspend (ReviewItem) -> Unit): Job = viewModelScope.launch(Dispatchers.IO) {
        mutex.withLock {
            val item = _state.value.item ?: return@withLock
            if (!lockState.isUnlocked(VaultTier.STANDARD)) return@withLock
            try {
                write(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "review decision failed for item ${item.row.id}: ${e.javaClass.name}")
            }
        }
        load().join()
    }

    private fun load(): Job = viewModelScope.launch(Dispatchers.IO) {
        mutex.withLock {
            if (!lockState.isUnlocked(VaultTier.STANDARD)) {
                _state.value = ReviewState(loaded = true, locked = true)
                return@withLock
            }
            val labels = db().labelDao()
            val queue = labels.idsIn(Category.REVIEW, includeLocked = true)
            val next = queue.firstOrNull { it !in skipped }?.let { id -> db().mediaDao().get(id) }
            val item = next?.let { row ->
                val own = labels.labelsFor(row.id).sortedByDescending { it.confidence }
                ReviewItem(row, own.firstOrNull { it.category.isReviewable }?.category, own.map { it.category })
            }
            // Re-check: a lock during the query must not publish an item.
            _state.value = if (lockState.isUnlocked(VaultTier.STANDARD)) {
                ReviewState(loaded = true, locked = false, item = item, remaining = queue.size)
            } else {
                ReviewState(loaded = true, locked = true)
            }
        }
    }

    private companion object {
        const val TAG = "ReviewViewModel"

        val Category.isReviewable get() = group == CategoryGroup.SENSITIVE || group == CategoryGroup.DOCUMENTS

        /** As the categorizer applies an included override: forced at 1.0, a My-nudes sub-folder implies My nudes. */
        fun forcedLabels(id: Long, c: Category): List<LabelRow> = buildList {
            add(LabelRow(id, c, 1f))
            if (c == Category.MY_NUDES_CAMERA || c == Category.MY_NUDES_OF_ME) add(LabelRow(id, Category.MY_NUDES, 1f))
        }
    }
}
