package com.aks.gallerysorter.vault

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.aks.gallerysorter.core.VaultTier
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.TinkProtoKeysetFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.FileChannel
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/** The PIN/passcode did not decrypt the private key (or the key file was altered). */
class WrongSecretException : Exception("Wrong secret")

/** The biometric Keystore key is gone or was invalidated (e.g. a new fingerprint was enrolled). */
class BiometricInvalidatedException(cause: Throwable? = null) :
    Exception("Biometric key invalidated", cause)

/**
 * `private.pin.enc` exists but `public.keyset` does not (e.g. a crash during [VaultKeys.create],
 * or the public file vanished from shared storage), and the secret given does not unlock it.
 * The UI should ask for the vault's existing PIN/passcode; the private key is never overwritten.
 */
class OrphanedVaultException : Exception("Existing vault key needs its original secret")

/** The secret is not valid text (e.g. an unpaired surrogate) and cannot be encoded as UTF-8. */
class InvalidSecretException : IllegalArgumentException("Secret contains invalid characters")

/** Argon2id cost parameters; written into each key file so they can change later. */
data class Argon2Params(val iterations: Int, val memoryKiB: Int, val parallelism: Int) {
    companion object {
        val PRODUCTION = Argon2Params(iterations = 3, memoryKiB = 65536, parallelism = 1)
    }
}

/**
 * Standard vault: 6+ ASCII digits. High-security vault: 10+ characters of any kind.
 * Does not zero [s]; the caller passes it on to [VaultKeys], which does.
 */
fun validateSecret(tier: VaultTier, s: CharArray): Boolean = when (tier) {
    VaultTier.STANDARD -> s.size >= 6 && s.all { it in '0'..'9' }
    VaultTier.HIGH_SECURITY -> s.size >= 10 && utf8Length(s) >= 0
}

/** Exact UTF-8 byte length of [s], or -1 if it contains an unpaired surrogate. */
internal fun utf8Length(s: CharArray): Int {
    var n = 0
    var i = 0
    while (i < s.size) {
        val c = s[i]
        when {
            c.code < 0x80 -> n += 1
            c.code < 0x800 -> n += 2
            c.isHighSurrogate() -> {
                if (i + 1 >= s.size || !s[i + 1].isLowSurrogate()) return -1
                n += 4
                i++
            }
            c.isLowSurrogate() -> return -1
            else -> n += 3
        }
        i++
    }
    return n
}

/**
 * One vault tier's key pair, stored in [dir] (`<vault root>/keys`, see [keysDir]):
 * - `public.keyset`: the Tink hybrid public keyset, in plain form (encrypting needs only this).
 * - `private.pin.enc`: the private keyset, AES-256-GCM under an Argon2id key from the PIN/passcode.
 * - `private.bio.enc`: optional; the private keyset under the biometric Keystore key.
 *
 * Every function taking a secret zeroes that CharArray before returning.
 * Key files are replaced atomically (temp file, fsync, rename) and [create] never overwrites.
 */
class VaultKeys internal constructor(
    private val dir: File,
    private val keystore: KeyWrapper,
    private val argon2: Argon2Params,
) {
    constructor(dir: File, keystore: KeyWrapper) : this(dir, keystore, Argon2Params.PRODUCTION)

    private val publicFile = File(dir, PUBLIC_FILE)
    private val pinFile = File(dir, PIN_FILE)
    private val bioFile = File(dir, BIO_FILE)

    init {
        // Never write a key file that the bounded reader would later refuse to open.
        require(
            argon2.iterations in 1..MAX_ITERATIONS && argon2.parallelism in 1..MAX_PARALLELISM &&
                argon2.memoryKiB in 8 * argon2.parallelism..MAX_MEMORY_KIB,
        ) { "Argon2 parameters out of range" }
        VaultCrypto.ensureRegistered()
    }

    fun isCreated(): Boolean = publicFile.exists()

    /**
     * Generates the key pair. Throws IllegalStateException if `public.keyset` already exists.
     * If only `private.pin.enc` exists (orphaned private key), it is never overwritten: when
     * [secret] unlocks it, `public.keyset` is rebuilt from it (lossless repair) and this returns
     * normally; otherwise [OrphanedVaultException] is thrown.
     */
    fun create(secret: CharArray) {
        try {
            require(secret.isNotEmpty()) { "Empty secret" }
            check(!publicFile.exists()) { "Vault keys already exist" }
            if (pinFile.exists()) {
                repairPublicKey(secret.copyOf())
                return
            }
            // A biometric wrap with no PIN file may still be the only copy of a private key.
            check(!bioFile.exists()) { "Vault keys already exist" }
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create key directory")
            val priv = KeysetHandle.generateNew(KeyTemplates.get(VaultCrypto.HYBRID_TEMPLATE))
            // Private first: a crash in between leaves a recoverable private key, never a public
            // key that new files would be encrypted to with no way to decrypt them.
            writePinFile(priv, secret)
            writeAtomically(
                publicFile,
                TinkProtoKeysetFormat.serializeKeysetWithoutSecret(priv.publicKeysetHandle),
            )
        } finally {
            secret.fill('\u0000')
        }
    }

    private fun repairPublicKey(secret: CharArray) {
        val priv = try {
            unlockWithSecret(secret)
        } catch (e: WrongSecretException) {
            throw OrphanedVaultException()
        }
        writeAtomically(
            publicFile,
            TinkProtoKeysetFormat.serializeKeysetWithoutSecret(priv.keyset.publicKeysetHandle),
        )
    }

    /**
     * The raw `private.pin.enc` bytes, after checking they parse as a key file (no secret needed).
     * Throws IOException if the file is missing or malformed.
     */
    internal fun pinFileBytes(): ByteArray = pinFile.readBytes().also { PinFile.parse(it) }

    fun publicHandle(): PublicHandle =
        PublicHandle(TinkProtoKeysetFormat.parseKeysetWithoutSecret(publicFile.readBytes()))

    /** Throws [WrongSecretException] on a wrong secret, IOException on a missing/malformed file. */
    fun unlockWithSecret(secret: CharArray): PrivateHandle {
        try {
            val f = PinFile.parse(pinFile.readBytes())
            val key = deriveKey(secret, f.salt, f.params)
            val plain = try {
                Cipher.getInstance(AES_GCM).run {
                    init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, f.iv))
                    updateAAD(f.header)
                    doFinal(f.ciphertext)
                }
            } catch (e: AEADBadTagException) {
                throw WrongSecretException()
            } finally {
                key.fill(0)
            }
            return parsePrivate(plain)
        } finally {
            secret.fill('\u0000')
        }
    }

    /**
     * Re-encrypts `private.pin.enc` under [newSecret] (fresh salt and IV). `private.bio.enc`
     * wraps the same private key and is left as it is.
     */
    fun changeSecret(handle: PrivateHandle, newSecret: CharArray) {
        try {
            require(newSecret.isNotEmpty()) { "Empty secret" }
            requireBelongsToThisVault(handle)
            writePinFile(handle.keyset, newSecret)
        } finally {
            newSecret.fill('\u0000')
        }
    }

    // ---- Biometric ----------------------------------------------------------------------

    fun isBiometricEnabled(): Boolean = bioFile.exists()

    /** Encrypt cipher to authenticate with BiometricPrompt, then pass to [enableBiometric]. */
    fun biometricEncryptCipher(): Cipher = translatingInvalidation { keystore.encryptCipher() }

    /** Writes `private.bio.enc` with an authenticated cipher from [biometricEncryptCipher]. */
    fun enableBiometric(handle: PrivateHandle, cipher: Cipher) {
        requireBelongsToThisVault(handle)
        val iv = cipher.iv
        val header = ByteBuffer.allocate(BIO_MAGIC.size + 2 + iv.size)
            .put(BIO_MAGIC).put(BIO_VERSION).put(iv.size.toByte()).put(iv).array()
        val plain = serializePrivate(handle.keyset)
        val ct = try {
            translatingInvalidation {
                cipher.updateAAD(header)
                cipher.doFinal(plain)
            }
        } finally {
            plain.fill(0)
        }
        writeAtomically(bioFile, header + ct)
    }

    fun disableBiometric() {
        if (bioFile.exists() && !bioFile.delete()) throw IOException("Cannot delete biometric key file")
        keystore.delete()
    }

    /**
     * Decrypt cipher to authenticate with BiometricPrompt, then pass to [unlockWithBiometric].
     * Throws [BiometricInvalidatedException] if the Keystore key was invalidated; the PIN still works.
     */
    fun biometricCipher(): Cipher {
        check(isBiometricEnabled()) { "Biometric unlock is not enabled" }
        val f = readBioFile()
        return translatingInvalidation { keystore.decryptCipher(f.iv) }
    }

    /**
     * Throws [BiometricInvalidatedException] if `private.bio.enc` cannot be decrypted or parsed
     * (tampered, corrupt, or wrapped by an older Keystore key), so the UI falls back to the PIN.
     */
    fun unlockWithBiometric(cipher: Cipher): PrivateHandle {
        val f = readBioFile()
        val plain = try {
            translatingInvalidation {
                cipher.updateAAD(f.header)
                cipher.doFinal(f.ciphertext)
            }
        } catch (e: GeneralSecurityException) {
            throw BiometricInvalidatedException(e)
        }
        return try {
            parsePrivate(plain)
        } catch (e: GeneralSecurityException) {
            throw BiometricInvalidatedException(e)
        }
    }

    private fun readBioFile(): BioFile = try {
        BioFile.parse(bioFile.readBytes())
    } catch (e: CorruptKeyFileException) {
        throw BiometricInvalidatedException(e)
    }

    // ---- Internals ----------------------------------------------------------------------

    private fun writePinFile(priv: KeysetHandle, secret: CharArray) {
        val salt = ByteArray(SALT_LEN).also(random::nextBytes)
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val header = PinFile.header(salt, argon2, iv)
        val key = deriveKey(secret, salt, argon2)
        val plain = serializePrivate(priv)
        val ct = try {
            Cipher.getInstance(AES_GCM).run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
                updateAAD(header)
                doFinal(plain)
            }
        } finally {
            key.fill(0)
            plain.fill(0)
        }
        writeAtomically(pinFile, header + ct)
    }

    /** Refuses a private key whose public half is not this vault's `public.keyset`. */
    private fun requireBelongsToThisVault(handle: PrivateHandle) {
        val probe = ByteArray(32).also(random::nextBytes)
        val matches = try {
            val ct = publicHandle().keyset.getPrimitive(RegistryConfiguration.get(), HybridEncrypt::class.java)
                .encrypt(probe, PROBE_CONTEXT)
            handle.keyset.getPrimitive(RegistryConfiguration.get(), HybridDecrypt::class.java)
                .decrypt(ct, PROBE_CONTEXT)
                .contentEquals(probe)
        } catch (e: GeneralSecurityException) {
            false
        }
        require(matches) { "Private key does not belong to this vault" }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val tmp = File(dir, target.name + ".tmp")
        try {
            FileOutputStream(tmp).use {
                it.write(bytes)
                it.fd.sync()
            }
            Files.move(
                tmp.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (t: Throwable) {
            tmp.delete()
            throw t
        }
        try {
            FileChannel.open(dir.toPath(), StandardOpenOption.READ).use { it.force(true) }
        } catch (_: IOException) {
            // Directory fsync is best effort; not every filesystem allows opening a directory.
        }
    }

    private class PinFile(
        val header: ByteArray,
        val salt: ByteArray,
        val params: Argon2Params,
        val iv: ByteArray,
        val ciphertext: ByteArray,
    ) {
        companion object {
            const val HEADER_LEN = 4 + 1 + SALT_LEN + 3 * 4 + IV_LEN

            fun header(salt: ByteArray, p: Argon2Params, iv: ByteArray): ByteArray =
                ByteBuffer.allocate(HEADER_LEN)
                    .put(PIN_MAGIC).put(PIN_VERSION).put(salt)
                    .putInt(p.iterations).putInt(p.memoryKiB).putInt(p.parallelism)
                    .put(iv).array()

            fun parse(bytes: ByteArray): PinFile {
                if (bytes.size < HEADER_LEN + TAG_BITS / 8) throw CorruptKeyFileException("Key file truncated")
                val b = ByteBuffer.wrap(bytes)
                val magic = ByteArray(4).also { b.get(it) }
                if (!magic.contentEquals(PIN_MAGIC)) throw CorruptKeyFileException("Not a vault key file")
                val version = b.get()
                if (version != PIN_VERSION) throw CorruptKeyFileException("Unsupported key file version $version")
                val salt = ByteArray(SALT_LEN).also { b.get(it) }
                val params = Argon2Params(b.int, b.int, b.int)
                // Bound the costs so a corrupted file cannot make us allocate gigabytes or spin.
                if (params.iterations !in 1..MAX_ITERATIONS ||
                    params.parallelism !in 1..MAX_PARALLELISM ||
                    params.memoryKiB !in 8 * params.parallelism..MAX_MEMORY_KIB
                ) {
                    throw CorruptKeyFileException("Key file has invalid parameters")
                }
                val iv = ByteArray(IV_LEN).also { b.get(it) }
                return PinFile(
                    header = bytes.copyOfRange(0, HEADER_LEN),
                    salt = salt,
                    params = params,
                    iv = iv,
                    ciphertext = bytes.copyOfRange(HEADER_LEN, bytes.size),
                )
            }
        }
    }

    /** A key file that is malformed (as opposed to unreadable from disk). */
    private class CorruptKeyFileException(message: String) : IOException(message)

    private class BioFile(val header: ByteArray, val iv: ByteArray, val ciphertext: ByteArray) {
        companion object {
            fun parse(bytes: ByteArray): BioFile {
                val fixed = BIO_MAGIC.size + 2
                if (bytes.size < fixed) throw CorruptKeyFileException("Key file truncated")
                if (!bytes.copyOfRange(0, BIO_MAGIC.size).contentEquals(BIO_MAGIC)) {
                    throw CorruptKeyFileException("Not a vault key file")
                }
                val version = bytes[BIO_MAGIC.size]
                if (version != BIO_VERSION) {
                    throw CorruptKeyFileException("Unsupported key file version $version")
                }
                val ivLen = bytes[BIO_MAGIC.size + 1].toInt() and 0xff
                val headerLen = fixed + ivLen
                if (ivLen == 0 || bytes.size < headerLen + TAG_BITS / 8) {
                    throw CorruptKeyFileException("Key file truncated")
                }
                return BioFile(
                    header = bytes.copyOfRange(0, headerLen),
                    iv = bytes.copyOfRange(fixed, headerLen),
                    ciphertext = bytes.copyOfRange(headerLen, bytes.size),
                )
            }
        }
    }

    companion object {
        const val PUBLIC_FILE = "public.keyset"
        const val PIN_FILE = "private.pin.enc"
        const val BIO_FILE = "private.bio.enc"

        /** Key directory inside a vault root (`.vault-gs/` or `filesDir/hsvault/`). */
        fun keysDir(vaultRoot: File): File = File(vaultRoot, "keys")

        private val PIN_MAGIC = "GSV1".toByteArray(Charsets.US_ASCII)
        private const val PIN_VERSION: Byte = 1
        private val BIO_MAGIC = "GSB1".toByteArray(Charsets.US_ASCII)
        private const val BIO_VERSION: Byte = 1
        private const val SALT_LEN = 16
        private const val IV_LEN = 12
        private const val TAG_BITS = 128
        private const val KEY_LEN = 32
        private const val AES_GCM = "AES/GCM/NoPadding"
        // Ceilings on parameters read from a key file (2x production memory): an edited header
        // must not be able to make unlock allocate or spin without bound before the GCM check.
        private const val MAX_ITERATIONS = 10
        private const val MAX_PARALLELISM = 4
        private const val MAX_MEMORY_KIB = 131072
        private val PROBE_CONTEXT = "gs-vault-probe".toByteArray(Charsets.US_ASCII)

        private val random = SecureRandom()

        private fun deriveKey(secret: CharArray, salt: ByteArray, p: Argon2Params): ByteArray {
            val password = encodeSecret(secret)
            try {
                val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withIterations(p.iterations)
                    .withMemoryAsKB(p.memoryKiB)
                    .withParallelism(p.parallelism)
                    .withSalt(salt)
                    .build()
                val out = ByteArray(KEY_LEN)
                Argon2BytesGenerator().apply { init(params) }.generateBytes(password, out)
                return out
            } finally {
                password.fill(0)
            }
        }

        /**
         * UTF-8 bytes of [secret] in one exactly-sized buffer (the caller zeroes it). Unpaired
         * surrogates are rejected with [InvalidSecretException] rather than replaced by '?'.
         */
        private fun encodeSecret(secret: CharArray): ByteArray {
            val len = utf8Length(secret)
            if (len < 0) throw InvalidSecretException()
            val out = ByteArray(len)
            val dst = ByteBuffer.wrap(out)
            val encoder = Charsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val ok = !encoder.encode(CharBuffer.wrap(secret), dst, true).isError &&
                !encoder.flush(dst).isError && !dst.hasRemaining()
            if (!ok) {
                out.fill(0)
                throw InvalidSecretException()
            }
            return out
        }

        private fun serializePrivate(keyset: KeysetHandle): ByteArray =
            TinkProtoKeysetFormat.serializeKeyset(keyset, InsecureSecretKeyAccess.get())

        private fun parsePrivate(plain: ByteArray): PrivateHandle = try {
            PrivateHandle(TinkProtoKeysetFormat.parseKeyset(plain, InsecureSecretKeyAccess.get()))
        } finally {
            plain.fill(0)
        }

        private inline fun <T> translatingInvalidation(block: () -> T): T = try {
            block()
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw BiometricInvalidatedException(e)
        }
    }
}
