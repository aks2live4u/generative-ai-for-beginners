package com.aks.gallerysorter.vault

import android.content.Context
import com.aks.gallerysorter.core.VaultTier
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.DigestOutputStream
import java.security.MessageDigest

/**
 * On-disk layout of both vault tiers:
 * `<root>/keys/` ([VaultKeys]), `<root>/files/<uuid>.enc`, `<root>/thumbs/<uuid>.thumb.enc`.
 * New files are written as `.part` and renamed only after verification. Paths stored in the
 * index are relative to the tier root ([resolve]).
 */
class VaultStore(private val standardRoot: File, private val highSecurityRoot: File) {

    fun root(tier: VaultTier): File = when (tier) {
        VaultTier.STANDARD -> standardRoot
        VaultTier.HIGH_SECURITY -> highSecurityRoot
    }

    fun filesDir(tier: VaultTier): File = File(root(tier), FILES)

    fun thumbsDir(tier: VaultTier): File = File(root(tier), THUMBS)

    fun resolve(tier: VaultTier, relative: String): File = File(root(tier), relative)

    /**
     * Creates the tier's root, `files/` and `thumbs/`. The standard tier lives on shared storage,
     * so each of those also gets a `.nomedia` (other galleries and cloud backup skip it).
     */
    fun ensure(tier: VaultTier) {
        for (dir in listOf(root(tier), filesDir(tier), thumbsDir(tier))) {
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create vault directory")
            if (tier == VaultTier.STANDARD) {
                val marker = File(dir, NOMEDIA)
                if (!marker.isFile && !marker.createNewFile()) throw IOException("Cannot create .nomedia")
            }
        }
    }

    companion object {
        const val FILES = "files"
        const val THUMBS = "thumbs"
        const val NOMEDIA = ".nomedia"
        const val PART = ".part"
        const val TMP = ".tmp"
        const val ENC = ".enc"
        const val STANDARD_ROOT = "/storage/emulated/0/.vault-gs"
        const val EXTERNAL_ROOT = "/storage/emulated/0"

        fun forDevice(context: Context) = VaultStore(File(STANDARD_ROOT), File(context.filesDir, "hsvault"))
    }
}

/** What writing one encrypted `.part` produced; hashes are lowercase hex SHA-256. */
internal class Written(
    val wrappedKey: ByteArray,
    val plainSha256: String,
    val plainLength: Long,
    val cipherSha256: String,
)

/** SHA-256 and length of a byte stream. */
internal data class Digest(val sha256: String, val length: Long)

/** File-level helpers for vault writes: hashing, fsync, verification, renames. */
internal object VaultIo {
    private const val BUFFER = 64 * 1024

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun sha256(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun digest(input: InputStream): Digest {
        val md = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(BUFFER)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            md.update(buf, 0, n)
            total += n
        }
        return Digest(hex(md.digest()), total)
    }

    fun digest(file: File): Digest = FileInputStream(file).use { digest(it) }

    /**
     * Encrypts [input] into a new file [part] (never an existing one), hashing the plaintext and
     * the ciphertext on the way, then fsyncs it. On failure the partial file is deleted.
     */
    fun writeEncrypted(part: File, pub: PublicHandle, input: InputStream, encrypt: Encryptor): Written {
        val plain = HashingInputStream(input)
        val cipherMd = MessageDigest.getInstance("SHA-256")
        val channel = FileChannel.open(part.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        try {
            channel.use { ch ->
                val out = DigestOutputStream(BufferedOutputStream(Channels.newOutputStream(ch), BUFFER), cipherMd)
                val wrapped = encrypt(pub, plain, out)
                out.flush()
                ch.force(true)
                return Written(wrapped, hex(plain.md.digest()), plain.count, hex(cipherMd.digest()))
            }
        } catch (t: Throwable) {
            part.delete()
            throw t
        }
    }

    /**
     * Checks a written `.part` without the private key: re-read from disk, it has exactly the
     * length Tink produces for the plaintext size, a valid header length byte, and the same bytes
     * that were written.
     */
    fun ciphertextIntact(part: File, w: Written): Boolean {
        if (part.length() != VaultCrypto.expectedCiphertextSize(w.plainLength)) return false
        val first = FileInputStream(part).use { it.read() }
        if (first != VaultCrypto.headerLength) return false
        return digest(part).sha256 == w.cipherSha256
    }

    /** Decrypts [file] fully (authenticating every segment) and returns the plaintext digest. */
    fun decryptedDigest(priv: PrivateHandle, wrappedKey: ByteArray, file: File): Digest =
        Channels.newInputStream(VaultCrypto.decryptingStream(priv, wrappedKey, file)).use { digest(it) }

    /** Decrypts [file] fully into memory (thumbnails only). */
    fun decryptSmall(priv: PrivateHandle, wrappedKey: ByteArray, file: File): ByteArray =
        Channels.newInputStream(VaultCrypto.decryptingStream(priv, wrappedKey, file)).use { it.readBytes() }

    /**
     * Decrypts [enc] into a new file [part] (never an existing one), fsyncs it and returns the
     * plaintext digest. On failure the partial file is deleted.
     */
    fun decryptTo(priv: PrivateHandle, wrappedKey: ByteArray, enc: File, part: File): Digest {
        val channel = FileChannel.open(part.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        try {
            channel.use { ch ->
                val out = BufferedOutputStream(Channels.newOutputStream(ch), BUFFER)
                val hashing = HashingInputStream(Channels.newInputStream(VaultCrypto.decryptingStream(priv, wrappedKey, enc)))
                hashing.use { it.copyTo(out, BUFFER) }
                out.flush()
                ch.force(true)
                return Digest(hex(hashing.md.digest()), hashing.count)
            }
        } catch (t: Throwable) {
            part.delete()
            throw t
        }
    }

    /** Atomic rename inside one vault directory (unique uuid names), then a directory fsync. */
    fun moveAtomic(from: File, to: File) {
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE)
        fsyncDir(to.parentFile)
    }

    /** Rename that fails with FileAlreadyExistsException rather than replace [to]. */
    fun moveNoReplace(from: File, to: File) {
        Files.move(from.toPath(), to.toPath())
        fsyncDir(to.parentFile)
    }

    private fun fsyncDir(dir: File?) {
        if (dir == null) return
        try {
            FileChannel.open(dir.toPath(), StandardOpenOption.READ).use { it.force(true) }
        } catch (_: IOException) {
            // Best effort; not every filesystem allows opening a directory.
        }
    }

    private class HashingInputStream(input: InputStream) : FilterInputStream(input) {
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        var count = 0L

        override fun read(): Int {
            val b = super.read()
            if (b >= 0) { md.update(b.toByte()); count++ }
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n > 0) { md.update(b, off, n); count += n }
            return n
        }

        override fun skip(n: Long): Long = throw IOException("skip not supported")
        override fun markSupported() = false
    }
}
