package com.aks.gallerysorter.vault

import android.util.Log
import com.aks.gallerysorter.core.CategoryAssignment
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.dao.SettingsDao
import com.aks.gallerysorter.index.dao.UnhiddenDao
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.SettingRow
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** What auto-hide needs to know about one tier: set up in this index, and when it was last unlocked. */
data class TierState(val setUp: Boolean, val lastUnlockMs: Long?)

/** A tier counts as in use when it was unlocked (or set up) within this window. */
const val AUTO_HIDE_RECENT_UNLOCK_MS = 30L * 24 * 60 * 60 * 1000

/**
 * The tier auto-hide uses for an item whose default tier is [wanted]: [wanted] when it is set up
 * and was unlocked in the last 30 days; otherwise the standard tier if that is; otherwise null
 * (skip: never hide into a vault the user may not be able to open).
 */
fun autoHideTier(wanted: VaultTier, tiers: Map<VaultTier, TierState>, nowMs: Long): VaultTier? {
    fun usable(t: VaultTier): Boolean {
        val s = tiers[t] ?: return false
        val last = s.lastUnlockMs ?: return false
        return s.setUp && nowMs - last <= AUTO_HIDE_RECENT_UNLOCK_MS
    }
    return when {
        usable(wanted) -> wanted
        usable(VaultTier.STANDARD) -> VaultTier.STANDARD
        else -> null
    }
}

/**
 * The scan's auto-hide step for one confidently Sensitive item ([com.aks.gallerysorter.scan.ScanPipeline]).
 * Picks the tier with [autoHideTier]; when none is usable the item stays in the gallery and
 * [blocked] fires at most once a day (a neutral notification). Content the user unhid before is
 * never hidden again automatically ([HideManager.hide] with `auto`). [incomplete] gets the result
 * of a hide that left something undone. Logs carry ids only.
 */
class AutoHider(
    private val hideManager: HideManager,
    private val guard: VaultKeyGuard,
    private val tierSettings: TierSettings,
    private val settings: SettingsDao,
    private val unhidden: UnhiddenDao,
    private val privateHandle: (VaultTier) -> PrivateHandle?,
    private val blocked: () -> Unit,
    private val incomplete: (VaultTier, HideResult) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun autoHide(row: MediaRow, assignments: List<CategoryAssignment>) {
        val wanted = defaultTierFor(assignments.map { it.category }, tierSettings.overrides())
        val tiers = VaultTier.entries.associateWith { TierState(guard.isSetUp(it), guard.lastUnlockMs(it)) }
        val now = clock()
        val tier = autoHideTier(wanted, tiers, now)
        if (tier == null) {
            if (!unhiddenByUser(row)) {
                Log.i(TAG, "auto-hide skipped for item ${row.id}: no vault in use")
                notifyBlocked(now)
            }
            return
        }
        val res = hideManager.hide(listOf(row.id), tier, { _, _ -> }, priv = privateHandle(tier), auto = true)
        if (res.failed.isNotEmpty() || res.aborted != null || res.missing.isNotEmpty() || res.skipped.isNotEmpty()) {
            Log.w(TAG, "auto-hide did not complete for item ${row.id}")
            incomplete(tier, res)
        }
    }

    private suspend fun unhiddenByUser(row: MediaRow): Boolean {
        val sha = row.sha256 ?: try {
            VaultIo.digest(File(row.path)).sha256
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        }
        return unhidden.contains(sha)
    }

    private suspend fun notifyBlocked(now: Long) {
        val last = settings.get(BLOCKED_NOTICE_KEY)?.toLongOrNull()
        if (last != null && now - last in 0 until DAY_MS) return
        settings.put(SettingRow(BLOCKED_NOTICE_KEY, now.toString()))
        blocked()
    }

    companion object {
        const val BLOCKED_NOTICE_KEY = "autohide.blockedNoticeMs"
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val TAG = "AutoHider"
    }
}
