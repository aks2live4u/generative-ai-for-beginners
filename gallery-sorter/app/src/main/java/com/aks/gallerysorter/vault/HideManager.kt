package com.aks.gallerysorter.vault

import android.graphics.Bitmap
import android.util.Log
import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.dao.LabelDao
import com.aks.gallerysorter.index.dao.MediaDao
import com.aks.gallerysorter.index.dao.SettingsDao
import com.aks.gallerysorter.index.dao.UnhiddenDao
import com.aks.gallerysorter.index.dao.VaultDao
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.VaultRow
import com.aks.gallerysorter.vault.VaultRestore.Companion.INSUFFICIENT_SPACE
import com.aks.gallerysorter.vault.VaultRestore.Companion.SPACE_MARGIN
import com.aks.gallerysorter.vault.VaultRestore.Companion.VERIFY_FAILED
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [hidden] includes items whose identical bytes were already in the vault (their leftover
 * original is deleted now). [missing]: the file is gone from the phone. [skipped]: ids no longer
 * indexed. [keptUnhidden]: auto-hide only, content the user unhid earlier (never re-hidden
 * automatically). [aborted]: set when a batch-level failure (vault key check) stopped the batch
 * after the items already counted; the remaining ids were not attempted.
 */
data class HideResult(
    val hidden: Int,
    val missing: List<Long>,
    val failed: List<Pair<Long, String>>,
    val skipped: List<Long> = emptyList(),
    val aborted: String? = null,
    val keptUnhidden: List<Long> = emptyList(),
)
data class UnhideResult(val restored: Int, val failed: List<Pair<Long, String>>)
data class MoveResult(val moved: Int, val failed: List<Pair<Long, String>>, val aborted: String? = null)
data class VerifyResult(val verified: Int, val failed: List<Pair<Long, String>>)

/** Streams plaintext to ciphertext and returns the wrapped file key ([VaultCrypto.encrypt]). */
typealias Encryptor = (PublicHandle, InputStream, OutputStream) -> ByteArray

/**
 * Moves gallery files into the vault and back, one file at a time (peak extra space = one file).
 * Must be a single app-wide instance: one [Mutex] serialises every operation (hide, unhide,
 * moveTier, verifyPending, rebuildIndex, cleanupPartials), so bulk hide, auto-hide and unhide
 * never race on the same item.
 *
 * Hide, per item: still indexed → exists → (auto-hide: not unhidden by the user before) → not
 * already vaulted (if it is, see [hideVaulted]) → free space → encrypt to
 * `.part` (hashing the plaintext) → fsync → verify → rename to `.enc` → thumbnail → `.meta` →
 * private key confirmed → insert the row (committed) → delete the original → remove its
 * MediaStore entry. Verify = the original re-hashed now equals the streamed hash, the `.part`
 * re-read from disk has Tink's exact length, header byte and written bytes, and, when a
 * [PrivateHandle] is supplied, a full decrypt whose SHA-256 equals the original's (row
 * `verified`; locked hides are checked later by [verifyPending]). Any failure before the commit
 * deletes the new vault files and leaves the original untouched. After the commit, a failure to
 * delete the original keeps both copies and is reported as "original not deleted"; hiding it
 * again later finds the vault copy and only deletes the original. A successful unhide records
 * the content's SHA-256 in `user_unhidden` so auto-hide leaves it alone; an explicit hide of
 * that content removes the record.
 *
 * Logs carry only exception classes and ids, never paths or names.
 */
class HideManager(
    private val store: VaultStore,
    private val guard: VaultKeyGuard,
    private val vaultDao: VaultDao,
    private val mediaDao: MediaDao,
    private val labelDao: LabelDao,
    private val settings: SettingsDao,
    private val unhidden: UnhiddenDao,
    private val media: MediaSource,
    private val fileOps: FileOps,
    externalRoot: File = File(VaultStore.EXTERNAL_ROOT),
    privateDirs: List<File> = emptyList(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val encrypt: Encryptor = VaultCrypto::encrypt,
) {
    private val lock = Mutex()
    private val files = VaultFiles(store, encrypt)
    private val restore =
        VaultRestore(store, files, guard, vaultDao, settings, unhidden, fileOps, externalRoot, privateDirs, encrypt)
    private val rebuilder = VaultIndexRebuild(store, files, vaultDao)

    /**
     * Hides [ids] (MediaStore ids) into [tier]. Throws [VaultKeyMismatchException] or
     * [VaultPrivateKeyMissingException] before touching anything if the tier's keys fail their
     * pins; the same failure mid-batch stops it and is returned as [HideResult.aborted].
     * [progress] gets (done, total) after each item, on the IO dispatcher. Pass [priv] when
     * unlocked for decrypt-verification. [auto]: an automatic hide, which leaves content the user
     * unhid earlier in the gallery ([HideResult.keptUnhidden]).
     */
    suspend fun hide(
        ids: List<Long>,
        tier: VaultTier,
        progress: (Int, Int) -> Unit,
        priv: PrivateHandle? = null,
        auto: Boolean = false,
    ): HideResult = serialized {
        guard.publicHandle(tier)
        guard.requirePrivateKey(tier)
        store.ensure(tier)
        var hidden = 0
        var aborted: String? = null
        val missing = mutableListOf<Long>()
        val skipped = mutableListOf<Long>()
        val keptUnhidden = mutableListOf<Long>()
        val failed = mutableListOf<Pair<Long, String>>()
        for ((i, id) in ids.withIndex()) {
            currentCoroutineContext().ensureActive()
            val reason = perItem("hide", id, { aborted = it }) { hideOne(id, tier, priv, auto) }
            when (reason) {
                null -> hidden++
                MISSING -> missing += id
                SKIPPED -> skipped += id
                USER_UNHIDDEN -> keptUnhidden += id
                else -> failed += id to reason
            }
            progress(i + 1, ids.size)
            if (aborted != null) break
        }
        HideResult(hidden, missing, failed, skipped, aborted, keptUnhidden)
    }

    /** Null on success, else [MISSING], [SKIPPED], [USER_UNHIDDEN] or a failure reason. */
    private suspend fun hideOne(id: Long, tier: VaultTier, priv: PrivateHandle?, auto: Boolean): String? {
        val row = mediaDao.get(id) ?: return SKIPPED
        if (!fileOps.exists(row.path)) {
            mediaDao.delete(id)
            return MISSING
        }
        val original = File(row.path)
        val stamp = original.lastModified() // before hashing: any later write shows in finishHide
        val now by lazy { VaultIo.digest(original) }
        if (auto && unhidden.contains(now.sha256)) return USER_UNHIDDEN
        vaultedCopy(original) { now }?.let { return hideVaulted(id, it, original, stamp, tier, priv) }
        if (fileOps.freeBytes(store.filesDir(tier)) < original.length() + SPACE_MARGIN) return INSUFFICIENT_SPACE
        val pub = guard.publicHandle(tier) // re-verified before every encrypt
        val uuid = UUID.randomUUID().toString()
        val part = File(store.filesDir(tier), uuid + VaultStore.PART)
        var committed = false
        try {
            val w = FileInputStream(original).use { VaultIo.writeEncrypted(part, pub, it, encrypt) }
            verify(id, part, w, original, priv)?.let { return it }
            VaultIo.moveAtomic(part, store.resolve(tier, files.encRel(uuid)))
            val thumb = encryptThumb(row, tier, uuid, pub)
            val categories = categoriesOf(id)
            val verified = priv != null
            files.writeMeta(
                tier, uuid, pub,
                VaultMeta(
                    w.wrappedKey, thumb?.wrappedKey, w.plainSha256, row.path, row.kind, w.plainLength,
                    categories, System.currentTimeMillis(), verified,
                ),
            )
            guard.requirePrivateKey(tier) // never delete an original into a vault nobody can open
            val vaultRow = VaultRow(
                originalPath = row.path, tier = tier, vaultFile = files.encRel(uuid), kind = row.kind,
                sizeBytes = w.plainLength, categories = categories, wrappedFileKey = w.wrappedKey,
                thumbFile = thumb?.rel, thumbWrappedKey = thumb?.wrappedKey, plainSha256 = w.plainSha256,
                verified = verified,
            )
            return withContext(NonCancellable) {
                vaultDao.insert(vaultRow) // committed before the original is touched
                committed = true
                unhidden.delete(w.plainSha256) // hidden on purpose again
                finishHide(id, original, stamp)
            }
        } finally {
            part.delete()
            if (!committed) files.discard(tier, uuid)
        }
    }

    /**
     * The vault row holding the same bytes from the same path, if any: left by an earlier hide
     * whose delete failed, a crash between the row insert and the delete, or a rebuild from the
     * `.meta` of a hide that crashed before its insert. Verified rows first.
     */
    private suspend fun vaultedCopy(original: File, now: () -> Digest): VaultRow? {
        val rows = vaultDao.byOriginalPath(original.path)
        if (rows.isEmpty()) return null
        val digest = now()
        return rows.sortedByDescending { it.verified }.firstOrNull { Digest(it.plainSha256, it.sizeBytes) == digest }
    }

    /**
     * The original's bytes are already in the vault as [row]: finish that hide by deleting the
     * original (and its MediaStore entry), once the vault copy is checked as on a fresh hide (the
     * `.enc` has Tink's exact length and header byte; fully decrypted when [priv] is [row]'s tier
     * key) and [row]'s tier private key passes its pin. No second copy is made.
     */
    private suspend fun hideVaulted(
        id: Long, row: VaultRow, original: File, stamp: Long, tier: VaultTier, priv: PrivateHandle?,
    ): String? {
        val enc = store.resolve(row.tier, row.vaultFile)
        if (!enc.isFile || enc.length() != VaultCrypto.expectedCiphertextSize(row.sizeBytes) ||
            FileInputStream(enc).use { it.read() } != VaultCrypto.headerLength
        ) {
            return VERIFY_FAILED
        }
        if (priv != null && row.tier == tier) {
            val dec = try {
                VaultIo.decryptedDigest(priv, row.wrappedFileKey, enc)
            } catch (e: Exception) {
                Log.w(TAG, "verify failed for vault item ${row.id}: ${e.javaClass.name}")
                return VERIFY_FAILED
            }
            if (dec != Digest(row.plainSha256, row.sizeBytes)) return VERIFY_FAILED
            if (!row.verified) vaultDao.setVerified(row.id)
        }
        try {
            guard.requirePrivateKey(row.tier) // never delete an original into a vault nobody can open
        } catch (e: VaultPrivateKeyMissingException) {
            if (row.tier == tier) throw e // the batch's own tier: stop the batch as usual
            return PRIVATE_KEY_MISSING
        }
        return withContext(NonCancellable) {
            unhidden.delete(row.plainSha256)
            finishHide(id, original, stamp)
        }
    }

    /** Null when [part] is a faithful encryption of [original] as it is now; else the reason. */
    private fun verify(id: Long, part: File, w: Written, original: File, priv: PrivateHandle?): String? {
        if (VaultIo.digest(original) != Digest(w.plainSha256, w.plainLength)) return ORIGINAL_CHANGED
        if (!VaultIo.ciphertextIntact(part, w)) return VERIFY_FAILED
        if (priv != null) {
            val dec = try {
                VaultIo.decryptedDigest(priv, w.wrappedKey, part)
            } catch (e: Exception) {
                Log.w(TAG, "verify failed for media $id: ${e.javaClass.name}")
                return VERIFY_FAILED
            }
            if (dec != Digest(w.plainSha256, w.plainLength)) return VERIFY_FAILED
        }
        return null
    }

    /** Runs only after the row is committed: the vault copy is kept whatever happens here. */
    private suspend fun finishHide(id: Long, original: File, stamp: Long): String? {
        if (original.lastModified() != stamp) return CHANGED_AFTER_COPY
        val deleted = try {
            fileOps.deleteOriginal(original.path)
        } catch (e: Exception) {
            Log.w(TAG, "delete failed for media $id: ${e.javaClass.name}")
            false
        }
        if (!deleted) return ORIGINAL_NOT_DELETED
        try {
            fileOps.removeFromMediaStore(id, original.path)
            mediaDao.delete(id)
        } catch (e: Exception) {
            Log.w(TAG, "index cleanup failed for media $id: ${e.javaClass.name}") // next scan drops it
        }
        return null
    }

    private suspend fun categoriesOf(id: Long): String {
        val overrides = labelDao.overridesFor(id)
        val excluded = overrides.filter { it.isExcluded }.map { it.category }.toSet()
        val cats = labelDao.labelsFor(id).map { it.category }.filter { it !in excluded } +
            overrides.filter { !it.isExcluded }.map { it.category }
        return cats.distinct().joinToString(",") { it.name }
    }

    /** 320 px JPEG (q80) under its own per-file key; null (no thumbnail) on any failure. */
    private suspend fun encryptThumb(row: MediaRow, tier: VaultTier, uuid: String, pub: PublicHandle): Thumb? {
        val jpeg = try {
            val bmp = media.thumbnail(row.toItem(), THUMB_PX) ?: return null
            ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it); bmp.recycle() }
                .toByteArray()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "thumbnail failed for media ${row.id}: ${e.javaClass.name}")
            return null
        }
        return try {
            files.writeThumb(tier, uuid, pub, jpeg)
        } finally {
            jpeg.fill(0)
        }
    }

    /**
     * Restores each item to its original path (`name (1).ext`… if taken; `Pictures/Restored/` if
     * the folder is gone or unwritable), verifying the SHA-256 before the file gets its final
     * name. The vault copy and row are deleted only after that. [priv] must be the items' tier key.
     * A missing `.enc` is reported as "vault file missing" (the row is kept for the UI to remove).
     */
    suspend fun unhide(vaultIds: List<Long>, priv: PrivateHandle): UnhideResult = serialized {
        var restored = 0
        val failed = mutableListOf<Pair<Long, String>>()
        for (vid in vaultIds) {
            currentCoroutineContext().ensureActive()
            val reason = perItem("unhide", vid, {}) { restore.unhideOne(vid, priv) }
            if (reason == null) restored++ else failed += vid to reason
        }
        UnhideResult(restored, failed)
    }

    /**
     * Re-encrypts each item to [to] (pinned key) with [priv], the items' current tier key. The new
     * row is committed before the old files and row are deleted, so one copy always exists.
     * Moved rows are unverified until [verifyPending] runs for [to].
     */
    suspend fun moveTier(vaultIds: List<Long>, to: VaultTier, priv: PrivateHandle): MoveResult = serialized {
        guard.publicHandle(to)
        guard.requirePrivateKey(to)
        store.ensure(to)
        var moved = 0
        var aborted: String? = null
        val failed = mutableListOf<Pair<Long, String>>()
        for (vid in vaultIds) {
            currentCoroutineContext().ensureActive()
            val reason = perItem("move", vid, { aborted = it }) { restore.moveOne(vid, to, priv) }
            if (reason == null) moved++ else failed += vid to reason
            if (aborted != null) break
        }
        MoveResult(moved, failed, aborted)
    }

    /** After unlock: fully decrypts each unverified row of [tier] and checks its SHA-256. */
    suspend fun verifyPending(tier: VaultTier, priv: PrivateHandle): VerifyResult = serialized {
        var verified = 0
        val failed = mutableListOf<Pair<Long, String>>()
        for (row in vaultDao.unverified(tier)) {
            currentCoroutineContext().ensureActive()
            val reason = perItem("verify", row.id, {}) { restore.verifyOne(row, priv) }
            if (reason == null) verified++ else failed += row.id to reason
        }
        VerifyResult(verified, failed)
    }

    /**
     * "Start a new vault": moves [tier]'s whole vault folder aside, never deleting it (see
     * [VaultKeyGuard.archiveStandardVault] and [VaultKeyGuard.archiveHighSecurityVault]). For the
     * high-security tier its index rows are dropped too: they point into the archived folder,
     * whose `.meta` files still hold everything needed with the old passcode. Returns the archive.
     */
    suspend fun archiveVault(tier: VaultTier, now: LocalDateTime): File = serialized {
        when (tier) {
            VaultTier.STANDARD -> guard.archiveStandardVault(now)
            VaultTier.HIGH_SECURITY -> guard.archiveHighSecurityVault(now).also {
                withContext(NonCancellable) { vaultDao.deleteTier(tier) }
            }
        }
    }

    /** Recreates rows lost from the index from the tier's `.meta` files; returns how many. */
    suspend fun rebuildIndex(tier: VaultTier, priv: PrivateHandle): Int = serialized {
        rebuilder.rebuild(tier, priv)
    }

    /**
     * Startup: deletes leftover `*.part` / `*.tmp` under both tiers' `files/` and `thumbs/`, and
     * the plaintext partial of an interrupted unhide. `.enc` files without an index row are never
     * deleted (they may be the only copy if the index was lost); only their count is logged.
     */
    suspend fun cleanupPartials() = serialized {
        settings.get(VaultRestore.INFLIGHT_KEY)?.let { path ->
            val f = File(path)
            if (f.name.startsWith(".gs-") && f.name.endsWith(VaultStore.PART) && f.exists() && !f.delete()) {
                Log.w(TAG, "cannot delete an unhide partial")
            }
            settings.delete(VaultRestore.INFLIGHT_KEY)
        }
        val known = try {
            vaultDao.fileRefs().map { it.tier to it.vaultFile }.toSet()
        } catch (e: Exception) {
            Log.w(TAG, "cannot read vault index: ${e.javaClass.name}")
            null
        }
        var orphans = 0
        for (tier in VaultTier.entries) {
            for (dir in listOf(store.filesDir(tier), store.thumbsDir(tier))) {
                for (f in dir.listFiles().orEmpty()) {
                    val name = f.name
                    if (name.endsWith(VaultStore.PART) || name.endsWith(VaultStore.TMP)) {
                        if (!f.delete()) Log.w(TAG, "cannot delete a partial vault file")
                    } else if (dir.name == VaultStore.FILES && name.endsWith(VaultStore.ENC) && known != null &&
                        (tier to "${VaultStore.FILES}/$name") !in known
                    ) {
                        orphans++
                    }
                }
            }
        }
        if (orphans > 0) Log.w(TAG, "$orphans vault files have no index row; left in place")
    }

    private suspend fun <T> serialized(block: suspend () -> T): T = lock.withLock { withContext(io) { block() } }

    /**
     * Runs one item; a vault key failure is reported to [abort] (stop the batch), any other
     * exception becomes that item's failure. Cancellation propagates.
     */
    private suspend fun perItem(op: String, id: Long, abort: (String) -> Unit, block: suspend () -> String?): String? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: VaultKeyMismatchException) {
            abort(KEY_MISMATCH)
            KEY_MISMATCH
        } catch (e: VaultPrivateKeyMissingException) {
            abort(PRIVATE_KEY_MISSING)
            PRIVATE_KEY_MISSING
        } catch (e: Exception) {
            Log.w(TAG, "$op failed for item $id: ${e.javaClass.name}")
            "$op failed"
        }

    private fun MediaRow.toItem() = MediaItem(
        id = id, path = path, kind = kind, sizeBytes = sizeBytes, mime = mime, width = width,
        height = height, dateTakenMs = dateTakenMs, durationMs = durationMs, generation = generation,
    )

    companion object {
        const val KEY_MISMATCH = "vault public key mismatch"
        const val PRIVATE_KEY_MISSING = "vault private key missing"
        private const val TAG = "HideManager"
        private const val THUMB_PX = 320
        private const val MISSING = "missing"
        private const val SKIPPED = "skipped"
        private const val USER_UNHIDDEN = "unhidden by the user"
        private const val ORIGINAL_CHANGED = "original changed during hide"
        private const val ORIGINAL_NOT_DELETED = "original not deleted"
        private const val CHANGED_AFTER_COPY = "original changed after copy; not deleted"
    }
}
