package com.aks.gallerysorter.vault

import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKey
import com.google.crypto.tink.streamingaead.PredefinedStreamingAeadParameters
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import com.google.crypto.tink.subtle.AesGcmHkdfStreaming
import java.io.File
import java.io.FileInputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.NonWritableChannelException
import java.nio.channels.SeekableByteChannel
import java.nio.file.StandardOpenOption
import java.security.GeneralSecurityException

/** A vault's public hybrid keyset: enough to encrypt into the vault, never to decrypt. */
class PublicHandle internal constructor(internal val keyset: KeysetHandle)

/** A vault's private hybrid keyset; only obtainable by unlocking. */
class PrivateHandle internal constructor(internal val keyset: KeysetHandle)

/**
 * Per-file encryption. Each file gets a fresh Tink streaming AEAD keyset
 * (AES-256-GCM-HKDF, 1 MB segments, seekable), which is hybrid-encrypted to the vault's public key.
 * That hybrid ciphertext is the "wrapped file key" stored in the vault index.
 */
object VaultCrypto {
    /** Hybrid contextInfo: binds wrapped file keys to this vault format. */
    const val CONTEXT_INFO = "gs-vault-v1"

    /** Streaming AEAD associated data for every vault file. */
    const val FILE_AD = "gs-file-v1"

    internal const val HYBRID_TEMPLATE = "DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM"
    private const val FILE_KEY_TEMPLATE = "AES256_GCM_HKDF_1MB"
    private const val BUFFER = 64 * 1024

    private val contextInfo = CONTEXT_INFO.toByteArray(Charsets.US_ASCII)
    private val fileAd = FILE_AD.toByteArray(Charsets.US_ASCII)

    init {
        HybridConfig.register()
        StreamingAeadConfig.register()
    }

    /** Makes sure Tink is registered (touching the object runs [init] once, thread-safely). */
    internal fun ensureRegistered() = Unit

    /**
     * Encrypts [input] to [out] for the vault owning [pub] and returns the wrapped per-file key.
     * Neither stream is closed. If reading or writing fails, the exception propagates and the
     * final segment is never written, so the partial ciphertext fails authentication at its end
     * rather than passing as a shorter file.
     */
    fun encrypt(pub: PublicHandle, input: InputStream, out: OutputStream): ByteArray {
        val fileKey = KeysetHandle.generateNew(KeyTemplates.get(FILE_KEY_TEMPLATE))
        val streaming = fileStreamingAead(fileKey)
        val serialized = TinkProtoKeysetFormat.serializeKeyset(fileKey, InsecureSecretKeyAccess.get())
        val wrapped = try {
            pub.keyset.getPrimitive(RegistryConfiguration.get(), HybridEncrypt::class.java)
                .encrypt(serialized, contextInfo)
        } finally {
            serialized.fill(0)
        }

        val enc = streaming.newEncryptingStream(NonClosingOutputStream(out), fileAd)
        val buf = ByteArray(BUFFER)
        try {
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                enc.write(buf, 0, n)
            }
        } finally {
            buf.fill(0)
        }
        enc.close() // writes the final segment; only reached on success
        out.flush()
        return wrapped
    }

    /**
     * Opens [file] for random-access decryption. Each read authenticates its segment, and
     * reaching the end authenticates the final segment (so truncation is detected); tampering
     * surfaces as an IOException from read. The caller must close the channel.
     */
    fun decryptingStream(priv: PrivateHandle, wrappedKey: ByteArray, file: File): SeekableByteChannel {
        val serialized = priv.keyset.getPrimitive(RegistryConfiguration.get(), HybridDecrypt::class.java)
            .decrypt(wrappedKey, contextInfo)
        val streaming = try {
            fileStreamingAead(TinkProtoKeysetFormat.parseKeyset(serialized, InsecureSecretKeyAccess.get()))
        } finally {
            serialized.fill(0)
        }
        val channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)
        try {
            return VerifyingChannel(streaming.newSeekableDecryptingChannel(channel, fileAd)) {
                // Empty plaintext: the seekable channel never loads the (only) segment, so
                // authenticate it with the sequential decrypter instead.
                FileInputStream(file).use { raw ->
                    streaming.newDecryptingStream(raw, fileAd).use { dec ->
                        if (dec.read() != -1) throw IOException("Vault file size mismatch")
                    }
                }
            }
        } catch (t: Throwable) {
            channel.close()
            throw t
        }
    }

    /**
     * The raw AES-GCM-HKDF streaming primitive for a per-file keyset. Tink's keyset wrapper is
     * bypassed on purpose: its seekable channel cannot report size() before the first read and
     * returns 0 forever on an empty file. Only the expected key type and parameters are accepted.
     */
    private fun fileStreamingAead(keyset: KeysetHandle): StreamingAead {
        val key = keyset.primary.key
        if (keyset.size() != 1 || key !is AesGcmHkdfStreamingKey ||
            key.parameters != PredefinedStreamingAeadParameters.AES256_GCM_HKDF_1MB
        ) {
            throw GeneralSecurityException("Unexpected per-file key type")
        }
        return AesGcmHkdfStreaming.create(key)
    }

    /**
     * Read-only view over Tink's seekable decrypting channel that never spins: a read that makes
     * no progress before the end is an error, and end-of-file is reported only once the final
     * segment has been authenticated.
     */
    private class VerifyingChannel(
        private val inner: SeekableByteChannel,
        private val verifyEmpty: () -> Unit,
    ) : SeekableByteChannel {
        private var endVerified = false

        override fun read(dst: ByteBuffer): Int {
            if (!dst.hasRemaining()) return 0
            val size = inner.size()
            if (inner.position() >= size) {
                verifyEnd(size)
                return -1
            }
            val n = inner.read(dst)
            if (n <= 0) throw IOException("Vault file is truncated or corrupt")
            return n
        }

        private fun verifyEnd(size: Long) {
            if (endVerified) return
            if (size == 0L) {
                verifyEmpty()
            } else {
                // Reading the last byte loads and authenticates the final segment
                // (cached if it is already the current one).
                val pos = inner.position()
                inner.position(size - 1)
                val one = ByteBuffer.allocate(1)
                if (inner.read(one) != 1) throw IOException("Vault file is truncated or corrupt")
                inner.position(pos)
            }
            endVerified = true
        }

        override fun position(): Long = inner.position()
        override fun position(newPosition: Long): SeekableByteChannel = apply { inner.position(newPosition) }
        override fun size(): Long = inner.size()
        override fun isOpen(): Boolean = inner.isOpen
        override fun close() = inner.close()
        override fun write(src: ByteBuffer): Int = throw NonWritableChannelException()
        override fun truncate(size: Long): SeekableByteChannel = throw NonWritableChannelException()
    }

    private class NonClosingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        override fun write(b: ByteArray, off: Int, len: Int) = out.write(b, off, len)
        override fun close() = flush()
    }
}
