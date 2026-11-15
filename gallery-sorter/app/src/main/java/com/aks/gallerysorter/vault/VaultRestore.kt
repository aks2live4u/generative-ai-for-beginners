package com.aks.gallerysorter.vault

import android.util.Log
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.dao.SettingsDao
import com.aks.gallerysorter.index.dao.VaultDao
import com.aks.gallerysorter.index.entities.SettingRow
import com.aks.gallerysorter.index.entities.VaultRow
import java.io.File
import java.nio.channels.Channels
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Per-item unhide, tier move and verification for [HideManager] (which serialises all calls).
 * Each function returns null on success or a failure reason; logs carry ids and exception
 * classes only.
 */
internal class VaultRestore(
    private val store: VaultStore,
    private val files: VaultFiles,
    private val guard: VaultKeyGuard,
    private val vaultDao: VaultDao,
    private val settings: SettingsDao,
    private val fileOps: FileOps,
    private val externalRoot: File,
    private val encrypt: Encryptor,
) {
    /**
     * Decrypts to a hidden `.gs-<uuid>.part` in the destination folder (its path recorded in
     * [INFLIGHT_KEY] first, so [HideManager.cleanupPartials] can remove the plaintext after a
     * crash), fsyncs, checks the SHA-256, then renames it to the first free name without ever
     * replacing a file. Only then are the vault files and row deleted.
     */
    suspend fun unhideOne(vid: Long, priv: PrivateHandle): String? {
        val row = vaultDao.get(vid) ?: return NOT_FOUND
        val enc = store.resolve(row.tier, row.vaultFile)
        if (!enc.isFile) return VAULT_FILE_MISSING
        val original = File(row.originalPath)
        val dir = original.parentFile?.takeIf { it.isDirectory && it.canWrite() } ?: File(externalRoot, RESTORED_DIR)
        if (!dir.isDirectory && !dir.mkdirs()) return "cannot create folder"
        if (fileOps.freeBytes(dir) < row.sizeBytes + SPACE_MARGIN) return INSUFFICIENT_SPACE
        val target = freeName(dir, original.name) ?: return "no free name"
        val part = File(dir, ".gs-${UUID.randomUUID()}${VaultStore.PART}")
        settings.put(SettingRow(INFLIGHT_KEY, part.absolutePath))
        try {
            // decryptTo creates the part (never reusing an existing file) and removes it on failure.
            val got = try {
                VaultIo.decryptTo(priv, row.wrappedFileKey, enc, part)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "unhide decrypt failed for vault item $vid: ${e.javaClass.name}")
                return DECRYPT_FAILED
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
        } finally {
            withContext(NonCancellable) { settings.delete(INFLIGHT_KEY) }
        }
        withContext(NonCancellable) {
            try {
                fileOps.scanIntoMediaStore(target.path)
            } catch (e: Exception) {
                Log.w(TAG, "media scan failed for vault item $vid: ${e.javaClass.name}")
            }
            files.delete(row)
            vaultDao.delete(row.id)
        }
        return null
    }

    /** First of `name.ext`, `name (1).ext`, `name (2).ext`… that does not exist. */
    private fun freeName(dir: File, name: String): File? {
        val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
        val base = name.substring(0, dot)
        val ext = name.substring(dot)
        for (n in 0..MAX_SUFFIX) {
            val f = File(dir, if (n == 0) name else "$base ($n)$ext")
            if (!fileOps.exists(f.path)) return f
        }
        return null
    }

    /**
     * Stream-decrypts with [priv] (the item's current tier) and re-encrypts to [to]'s pinned key.
     * Order: new `.enc`, thumbnail and `.meta` → target private key confirmed → new row committed
     * → old `.meta`, `.enc`, thumbnail → old row. Batch-level key failures propagate.
     */
    suspend fun moveOne(vid: Long, to: VaultTier, priv: PrivateHandle): String? {
        val row = vaultDao.get(vid) ?: return NOT_FOUND
        if (row.tier == to) return null
        val oldEnc = store.resolve(row.tier, row.vaultFile)
        if (!oldEnc.isFile) return VAULT_FILE_MISSING
        if (fileOps.freeBytes(store.filesDir(to)) < row.sizeBytes + SPACE_MARGIN) return INSUFFICIENT_SPACE
        val pub = guard.publicHandle(to)
        val uuid = UUID.randomUUID().toString()
        val part = File(store.filesDir(to), uuid + VaultStore.PART)
        var committed = false
        try {
            val source = VaultCrypto.decryptingStream(priv, row.wrappedFileKey, oldEnc)
            val w = Channels.newInputStream(source).use { VaultIo.writeEncrypted(part, pub, it, encrypt) }
            // Without the target key: the plaintext that went in is the original, and the
            // ciphertext on disk is complete and as written. verifyPending checks it fully later.
            if (Digest(w.plainSha256, w.plainLength) != Digest(row.plainSha256, row.sizeBytes) ||
                !VaultIo.ciphertextIntact(part, w)
            ) {
                return VERIFY_FAILED
            }
            VaultIo.moveAtomic(part, store.resolve(to, files.encRel(uuid)))
            val thumb = row.thumbFile?.let { reencryptThumb(row, it, to, uuid, pub, priv) }
            files.writeMeta(
                to, uuid, pub,
                VaultMeta(
                    w.wrappedKey, thumb?.wrappedKey, row.plainSha256, row.originalPath, row.kind,
                    row.sizeBytes, row.categories, System.currentTimeMillis(), verified = false,
                ),
            )
            guard.requirePrivateKey(to) // the old copy is about to go: the new one must be openable
            val moved = VaultRow(
                originalPath = row.originalPath, tier = to, vaultFile = files.encRel(uuid), kind = row.kind,
                sizeBytes = row.sizeBytes, categories = row.categories, wrappedFileKey = w.wrappedKey,
                thumbFile = thumb?.rel, thumbWrappedKey = thumb?.wrappedKey, plainSha256 = row.plainSha256,
                verified = false,
            )
            withContext(NonCancellable) {
                vaultDao.insert(moved)
                committed = true
                files.delete(row)
                vaultDao.delete(row.id)
            }
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: VaultKeyMismatchException) {
            throw e
        } catch (e: VaultPrivateKeyMissingException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "move failed for vault item $vid: ${e.javaClass.name}")
            return "move failed"
        } finally {
            part.delete()
            if (!committed) files.discard(to, uuid)
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
            files.writeThumb(to, uuid, pub, jpeg)
        } finally {
            jpeg.fill(0)
        }
    }

    /** Full decrypt and SHA-256 check of one row; marks it verified on success. */
    suspend fun verifyOne(row: VaultRow, priv: PrivateHandle): String? {
        val enc = store.resolve(row.tier, row.vaultFile)
        if (!enc.isFile) return VAULT_FILE_MISSING
        val got = try {
            VaultIo.decryptedDigest(priv, row.wrappedFileKey, enc)
        } catch (e: Exception) {
            Log.w(TAG, "verify failed for vault item ${row.id}: ${e.javaClass.name}")
            return VERIFY_FAILED
        }
        if (got != Digest(row.plainSha256, row.sizeBytes)) return VERIFY_FAILED
        vaultDao.setVerified(row.id)
        return null
    }

    companion object {
        /** Settings key holding the absolute path of the plaintext partial of a running unhide. */
        const val INFLIGHT_KEY = "vault.unhide.inflight"
        const val SPACE_MARGIN = 52_428_800L // 50 MB
        const val NOT_FOUND = "not found"
        const val VAULT_FILE_MISSING = "vault file missing"
        const val INSUFFICIENT_SPACE = "insufficient space"
        const val VERIFY_FAILED = "verify failed"
        const val DECRYPT_FAILED = "decrypt failed"
        private const val TAG = "VaultRestore"
        private const val MAX_SUFFIX = 9999
        private const val RESTORED_DIR = "Pictures/Restored"
    }
}
