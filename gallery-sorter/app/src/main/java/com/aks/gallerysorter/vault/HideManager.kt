package com.aks.gallerysorter.vault

import android.graphics.Bitmap
import android.util.Log
import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.dao.LabelDao
import com.aks.gallerysorter.index.dao.MediaDao
import com.aks.gallerysorter.index.dao.VaultDao
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.VaultRow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class HideResult(val hidden: Int, val missing: List<Long>, val failed: List<Pair<Long, String>>)
data class UnhideResult(val restored: Int, val failed: List<Pair<Long, String>>)
data class MoveResult(val moved: Int, val failed: List<Pair<Long, String>>)

/** Streams plaintext to ciphertext and returns the wrapped file key ([VaultCrypto.encrypt]). */
typealias Encryptor = (PublicHandle, InputStream, OutputStream) -> ByteArray

/**
 * Moves gallery files into the vault and back, one file at a time (peak extra space = one file).
 *
 * Hide, per item: exists → free space → encrypt to `.part` (hashing the plaintext) → fsync →
 * verify → rename to `.enc` → thumbnail → insert the row (committed) → delete the original →
 * remove it from MediaStore. Verify = the original re-hashed now equals the streamed hash, the
 * `.part` re-read from disk has Tink's exact length, header byte and written bytes, and, when a
 * [PrivateHandle] is supplied, a full decrypt whose SHA-256 equals the original's. Any failure
 * before the row is committed deletes the new vault files and leaves the original untouched.
 * After the commit, a failure to delete the original keeps both copies and is reported as
 * "original not deleted" (or "original changed after copy; not deleted").
 *
 * Logs carry only exception classes and ids, never paths or names.
 */
class HideManager(
    private val store: VaultStore,
    private val guard: VaultKeyGuard,
    private val vaultDao: VaultDao,
    private val mediaDao: MediaDao,
    private val labelDao: LabelDao,
    private val media: MediaSource,
    private val fileOps: FileOps,
    private val externalRoot: File = File(VaultStore.EXTERNAL_ROOT),
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val encrypt: Encryptor = VaultCrypto::encrypt,
) {
    /**
     * Hides [ids] (MediaStore ids) into [tier]. Throws [VaultKeyMismatchException] before
     * touching anything if the tier's public key fails its pin. [progress] gets (done, total)
     * after each item, on the IO dispatcher. Pass [priv] when unlocked for decrypt-verification.
     */
    suspend fun hide(
        ids: List<Long>,
        tier: VaultTier,
        progress: (Int, Int) -> Unit,
        priv: PrivateHandle? = null,
    ): HideResult = withContext(io) {
        guard.publicHandle(tier)
        store.ensure(tier)
        var hidden = 0
        val missing = mutableListOf<Long>()
        val failed = mutableListOf<Pair<Long, String>>()
        ids.forEachIndexed { i, id ->
            ensureActive()
            when (val reason = hideOne(id, tier, priv)) {
                null -> hidden++
                MISSING -> missing += id
                else -> failed += id to reason
            }
            progress(i + 1, ids.size)
        }
        HideResult(hidden, missing, failed)
    }

    /** Null on success, else [MISSING] or a failure reason. */
    private suspend fun hideOne(id: Long, tier: VaultTier, priv: PrivateHandle?): String? {
        val row = mediaDao.get(id) ?: return "not indexed"
        if (!fileOps.exists(row.path)) {
            mediaDao.delete(id)
            return MISSING
        }
        val original = File(row.path)
        if (fileOps.freeBytes(store.filesDir(tier)) < original.length() + SPACE_MARGIN) return INSUFFICIENT_SPACE
        val pub = guard.publicHandle(tier) // re-verified before every encrypt; a mismatch aborts the batch
        val uuid = UUID.randomUUID().toString()
        val part = File(store.filesDir(tier), uuid + VaultStore.PART)
        val rel = "${VaultStore.FILES}/$uuid${VaultStore.ENC}"
        var thumb: Thumb? = null
        var committed = false
        try {
            val stamp = original.lastModified()
            val w = FileInputStream(original).use { VaultIo.writeEncrypted(part, pub, it, encrypt) }
            verify(id, part, w, original, priv)?.let { return it }
            VaultIo.moveAtomic(part, store.resolve(tier, rel))
            thumb = encryptThumb(row, tier, uuid, pub)
            val vaultRow = VaultRow(
                originalPath = row.path, tier = tier, vaultFile = rel, kind = row.kind,
                sizeBytes = w.plainLength, categories = categoriesOf(id), wrappedFileKey = w.wrappedKey,
                thumbFile = thumb?.rel, thumbWrappedKey = thumb?.wrappedKey, plainSha256 = w.plainSha256,
            )
            return withContext(NonCancellable) {
                vaultDao.insert(vaultRow) // committed before the original is touched
                committed = true
                finishHide(id, original, stamp)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "hide failed for media $id: ${e.javaClass.name}")
            return HIDE_FAILED
        } finally {
            part.delete()
            if (!committed) {
                store.resolve(tier, rel).delete()
                thumb?.let { store.resolve(tier, it.rel).delete() }
            }
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
            fileOps.removeFromMediaStore(id)
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

    private class Thumb(val rel: String, val wrappedKey: ByteArray)

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
            writeThumb(tier, uuid, pub, jpeg)
        } finally {
            jpeg.fill(0)
        }
    }

    private fun writeThumb(tier: VaultTier, uuid: String, pub: PublicHandle, jpeg: ByteArray): Thumb? {
        val part = File(store.thumbsDir(tier), "$uuid.thumb${VaultStore.PART}")
        val rel = "${VaultStore.THUMBS}/$uuid.thumb${VaultStore.ENC}"
        return try {
            val w = VaultIo.writeEncrypted(part, pub, ByteArrayInputStream(jpeg), encrypt)
            if (!VaultIo.ciphertextIntact(part, w)) return null
            VaultIo.moveAtomic(part, store.resolve(tier, rel))
            Thumb(rel, w.wrappedKey)
        } catch (e: Exception) {
            Log.w(TAG, "thumbnail encrypt failed: ${e.javaClass.name}")
            null
        } finally {
            part.delete()
        }
    }

    /**
     * Restores each item to its original path (`name (1).ext`… if taken; `Pictures/Restored/` if
     * the folder is gone), verifying the SHA-256 before the file gets its final name. The vault
     * copy and row are deleted only after that. [priv] must be the items' tier key.
     */
    suspend fun unhide(vaultIds: List<Long>, priv: PrivateHandle): UnhideResult = withContext(io) {
        var restored = 0
        val failed = mutableListOf<Pair<Long, String>>()
        for (vid in vaultIds) {
            ensureActive()
            val reason = unhideOne(vid, priv)
            if (reason == null) restored++ else failed += vid to reason
        }
        UnhideResult(restored, failed)
    }

    private suspend fun unhideOne(vid: Long, priv: PrivateHandle): String? {
        val row = vaultDao.get(vid) ?: return "not found"
        val original = File(row.originalPath)
        val dir = original.parentFile?.takeIf { it.isDirectory } ?: File(externalRoot, RESTORED_DIR)
        if (!dir.isDirectory && !dir.mkdirs()) return "cannot create folder"
        if (fileOps.freeBytes(dir) < row.sizeBytes + SPACE_MARGIN) return INSUFFICIENT_SPACE
        val target = freeName(dir, original.name) ?: return "no free name"
        val part = File(dir, target.name + VaultStore.PART)
        // decryptTo creates the part (never reusing an existing file) and removes it on failure.
        val got = try {
            VaultIo.decryptTo(priv, row.wrappedFileKey, store.resolve(row.tier, row.vaultFile), part)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "unhide decrypt failed for vault item $vid: ${e.javaClass.name}")
            return "decrypt failed"
        }
        try {
            if (got != Digest(row.plainSha256, row.sizeBytes)) return VERIFY_FAILED
            VaultIo.moveNoReplace(part, target)
        } catch (e: Exception) {
            Log.w(TAG, "unhide rename failed for vault item $vid: ${e.javaClass.name}")
            return "restore failed"
        } finally {
            part.delete() // ours from here on; gone already if the rename succeeded
        }
        withContext(NonCancellable) {
            try {
                fileOps.scanIntoMediaStore(target.path)
            } catch (e: Exception) {
                Log.w(TAG, "media scan failed for vault item $vid: ${e.javaClass.name}")
            }
            deleteVaultFiles(row)
            vaultDao.delete(row.id)
        }
        return null
    }

    /** First of `name.ext`, `name (1).ext`, `name (2).ext`… where neither it nor its `.part` exists. */
    private fun freeName(dir: File, name: String): File? {
        val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
        val base = name.substring(0, dot)
        val ext = name.substring(dot)
        for (n in 0..MAX_SUFFIX) {
            val f = File(dir, if (n == 0) name else "$base ($n)$ext")
            if (!fileOps.exists(f.path) && !fileOps.exists(f.path + VaultStore.PART)) return f
        }
        return null
    }

    /**
     * Re-encrypts each item to [to] (pinned key) with [priv], the items' current tier key. The new
     * row is committed before the old files and row are deleted, so one copy always exists.
     */
    suspend fun moveTier(vaultIds: List<Long>, to: VaultTier, priv: PrivateHandle): MoveResult = withContext(io) {
        guard.publicHandle(to)
        store.ensure(to)
        var moved = 0
        val failed = mutableListOf<Pair<Long, String>>()
        for (vid in vaultIds) {
            ensureActive()
            val reason = moveOne(vid, to, priv)
            if (reason == null) moved++ else failed += vid to reason
        }
        MoveResult(moved, failed)
    }

    private suspend fun moveOne(vid: Long, to: VaultTier, priv: PrivateHandle): String? {
        val row = vaultDao.get(vid) ?: return "not found"
        if (row.tier == to) return null
        if (fileOps.freeBytes(store.filesDir(to)) < row.sizeBytes + SPACE_MARGIN) return INSUFFICIENT_SPACE
        val pub = guard.publicHandle(to)
        val uuid = UUID.randomUUID().toString()
        val part = File(store.filesDir(to), uuid + VaultStore.PART)
        val rel = "${VaultStore.FILES}/$uuid${VaultStore.ENC}"
        var thumb: Thumb? = null
        var committed = false
        try {
            val source = VaultCrypto.decryptingStream(priv, row.wrappedFileKey, store.resolve(row.tier, row.vaultFile))
            val w = Channels.newInputStream(source).use { VaultIo.writeEncrypted(part, pub, it, encrypt) }
            // Without the target key: the plaintext that went in is the original, and the
            // ciphertext on disk is complete and as written.
            if (Digest(w.plainSha256, w.plainLength) != Digest(row.plainSha256, row.sizeBytes) ||
                !VaultIo.ciphertextIntact(part, w)
            ) {
                return VERIFY_FAILED
            }
            VaultIo.moveAtomic(part, store.resolve(to, rel))
            thumb = row.thumbFile?.let { reencryptThumb(row, it, to, uuid, pub, priv) }
            val moved = VaultRow(
                originalPath = row.originalPath, tier = to, vaultFile = rel, kind = row.kind,
                sizeBytes = row.sizeBytes, categories = row.categories, wrappedFileKey = w.wrappedKey,
                thumbFile = thumb?.rel, thumbWrappedKey = thumb?.wrappedKey, plainSha256 = row.plainSha256,
            )
            withContext(NonCancellable) {
                vaultDao.insert(moved)
                committed = true
                vaultDao.delete(row.id)
                deleteVaultFiles(row)
            }
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "move failed for vault item $vid: ${e.javaClass.name}")
            return "move failed"
        } finally {
            part.delete()
            if (!committed) {
                store.resolve(to, rel).delete()
                thumb?.let { store.resolve(to, it.rel).delete() }
            }
        }
    }

    private fun reencryptThumb(row: VaultRow, rel: String, to: VaultTier, uuid: String, pub: PublicHandle, priv: PrivateHandle): Thumb? {
        val key = row.thumbWrappedKey ?: return null
        val jpeg = try {
            VaultIo.decryptSmall(priv, key, store.resolve(row.tier, rel))
        } catch (e: Exception) {
            Log.w(TAG, "thumbnail decrypt failed for vault item ${row.id}: ${e.javaClass.name}")
            return null
        }
        return try {
            writeThumb(to, uuid, pub, jpeg)
        } finally {
            jpeg.fill(0)
        }
    }

    private fun deleteVaultFiles(row: VaultRow) {
        store.resolve(row.tier, row.vaultFile).delete()
        row.thumbFile?.let { store.resolve(row.tier, it).delete() }
    }

    /**
     * Startup: deletes leftover `*.part` / `*.tmp` under both tiers' `files/` and `thumbs/`.
     * `.enc` files without an index row are never deleted (they may be the only copy if the
     * index was lost); only their count is logged. Blocking; call off the main thread.
     */
    fun cleanupPartials() {
        val known = try {
            vaultDao.fileRefs().flatMap { r -> listOfNotNull(r.tier to r.vaultFile, r.thumbFile?.let { r.tier to it }) }.toSet()
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
                    } else if (name.endsWith(VaultStore.ENC) && known != null && (tier to "${dir.name}/$name") !in known) {
                        orphans++
                    }
                }
            }
        }
        if (orphans > 0) Log.w(TAG, "$orphans vault files have no index row; left in place")
    }

    private fun MediaRow.toItem() = MediaItem(
        id = id, path = path, kind = kind, sizeBytes = sizeBytes, mime = mime, width = width,
        height = height, dateTakenMs = dateTakenMs, durationMs = durationMs, generation = generation,
    )

    private companion object {
        const val TAG = "HideManager"
        const val SPACE_MARGIN = 52_428_800L // 50 MB
        const val THUMB_PX = 320
        const val MAX_SUFFIX = 9999
        const val RESTORED_DIR = "Pictures/Restored"
        const val MISSING = "missing"
        const val INSUFFICIENT_SPACE = "insufficient space"
        const val ORIGINAL_CHANGED = "original changed during hide"
        const val ORIGINAL_NOT_DELETED = "original not deleted"
        const val CHANGED_AFTER_COPY = "original changed after copy; not deleted"
        const val VERIFY_FAILED = "verify failed"
        const val HIDE_FAILED = "hide failed"
    }
}
