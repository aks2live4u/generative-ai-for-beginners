package com.aks.gallerysorter.vault

import android.util.Log
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.dao.VaultDao
import com.aks.gallerysorter.index.entities.VaultRow
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.RegistryConfiguration
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Everything needed to recreate an item's index row, stored next to its `.enc` as
 * `files/<uuid>.meta`, hybrid-encrypted to the tier's public key (contextInfo [CONTEXT]). With
 * it the vault survives losing the index (e.g. a reinstall, for the standard tier).
 */
internal class VaultMeta(
    val wrappedFileKey: ByteArray,
    val thumbWrappedKey: ByteArray?,
    val plainSha256: String,
    val originalPath: String,
    val kind: MediaKind,
    val sizeBytes: Long,
    val categories: String,
    val createdAtMs: Long,
    val verified: Boolean,
) {
    fun seal(pub: PublicHandle): ByteArray {
        val plain = encode()
        try {
            return pub.keyset.getPrimitive(RegistryConfiguration.get(), HybridEncrypt::class.java).encrypt(plain, CONTEXT)
        } finally {
            plain.fill(0)
        }
    }

    private fun encode(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeByte(VERSION)
            out.blob(wrappedFileKey)
            if (thumbWrappedKey == null) out.writeInt(-1) else out.blob(thumbWrappedKey)
            out.blob(plainSha256.toByteArray(Charsets.UTF_8))
            out.blob(originalPath.toByteArray(Charsets.UTF_8))
            out.blob(kind.name.toByteArray(Charsets.UTF_8))
            out.writeLong(sizeBytes)
            out.blob(categories.toByteArray(Charsets.UTF_8))
            out.writeLong(createdAtMs)
            out.writeBoolean(verified)
        }
        return bytes.toByteArray()
    }

    companion object {
        val CONTEXT = "gs-meta-v1".toByteArray(Charsets.US_ASCII)
        private const val VERSION = 1
        private const val MAX_BLOB = 1 shl 16

        private fun DataOutputStream.blob(b: ByteArray) {
            writeInt(b.size)
            write(b)
        }

        private fun DataInputStream.blob(): ByteArray? {
            val n = readInt()
            if (n == -1) return null
            if (n !in 0..MAX_BLOB) throw IOException("Corrupt vault metadata")
            return ByteArray(n).also { readFully(it) }
        }

        private fun DataInputStream.text(): String =
            String(blob() ?: throw IOException("Corrupt vault metadata"), Charsets.UTF_8)

        /** Decrypts and parses a `.meta` file; throws on a wrong key, tampering or a bad format. */
        fun open(priv: PrivateHandle, sealed: ByteArray): VaultMeta {
            val plain = priv.keyset.getPrimitive(RegistryConfiguration.get(), HybridDecrypt::class.java)
                .decrypt(sealed, CONTEXT)
            try {
                DataInputStream(ByteArrayInputStream(plain)).use { inp ->
                    if (inp.readByte().toInt() != VERSION) throw IOException("Unsupported vault metadata")
                    val meta = VaultMeta(
                        wrappedFileKey = inp.blob() ?: throw IOException("Corrupt vault metadata"),
                        thumbWrappedKey = inp.blob(),
                        plainSha256 = inp.text(),
                        originalPath = inp.text(),
                        kind = MediaKind.valueOf(inp.text()),
                        sizeBytes = inp.readLong(),
                        categories = inp.text(),
                        createdAtMs = inp.readLong(),
                        verified = inp.readBoolean(),
                    )
                    if (inp.read() != -1) throw IOException("Corrupt vault metadata")
                    return meta
                }
            } finally {
                plain.fill(0)
            }
        }
    }
}

/** Recreates index rows from `.meta` files (see [HideManager.rebuildIndex]). */
internal class VaultIndexRebuild(
    private val store: VaultStore,
    private val files: VaultFiles,
    private val vaultDao: VaultDao,
) {
    /**
     * For each `files/<uuid>.meta` in [tier] whose `.enc` exists but has no row, decrypts it with
     * [priv] and inserts the row, unless a row with the same original path and SHA-256 already
     * exists (e.g. the other half of an interrupted tier move). Returns the number of rows added.
     */
    suspend fun rebuild(tier: VaultTier, priv: PrivateHandle): Int {
        val known = vaultDao.fileRefs().filter { it.tier == tier }.map { it.vaultFile }.toSet()
        val metas = store.filesDir(tier).listFiles().orEmpty()
            .filter { it.name.endsWith(VaultStore.META) }
            .sortedBy { it.name }
        var added = 0
        for (f in metas) {
            val uuid = f.name.removeSuffix(VaultStore.META)
            val rel = files.encRel(uuid)
            if (rel in known || !store.resolve(tier, rel).isFile) continue
            val meta = try {
                VaultMeta.open(priv, f.readBytes())
            } catch (e: Exception) {
                Log.w(TAG, "unreadable vault metadata: ${e.javaClass.name}")
                continue
            }
            if (vaultDao.byOriginalPath(meta.originalPath).any { it.plainSha256 == meta.plainSha256 }) continue
            val thumb = files.thumbRel(uuid).takeIf { meta.thumbWrappedKey != null && store.resolve(tier, it).isFile }
            vaultDao.insert(
                VaultRow(
                    originalPath = meta.originalPath, tier = tier, vaultFile = rel, kind = meta.kind,
                    sizeBytes = meta.sizeBytes, categories = meta.categories,
                    wrappedFileKey = meta.wrappedFileKey, thumbFile = thumb,
                    thumbWrappedKey = if (thumb != null) meta.thumbWrappedKey else null,
                    plainSha256 = meta.plainSha256, verified = meta.verified,
                ),
            )
            added++
        }
        return added
    }

    private companion object {
        const val TAG = "VaultIndexRebuild"
    }
}
