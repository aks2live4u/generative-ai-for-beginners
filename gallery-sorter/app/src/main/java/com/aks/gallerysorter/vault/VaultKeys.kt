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
    VaultTier.HIGH_SECURITY -> s.size >= 10
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
        VaultCrypto.ensureRegistered()
    }

    fun isCreated(): Boolean = publicFile.exists() && pinFile.exists()

    /** Generates the key pair. Throws IllegalStateException if any key file already exists. */
    fun create(secret: CharArray) {
        try {
            require(secret.isNotEmpty()) { "Empty secret" }
            check(!publicFile.exists() && !pinFile.exists() && !bioFile.exists()) {
                "Vault keys already exist"
            }
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
        val f = BioFile.parse(bioFile.readBytes())
        return translatingInvalidation { keystore.decryptCipher(f.iv) }
    }

    fun unlockWithBiometric(cipher: Cipher): PrivateHandle {
        val f = BioFile.parse(bioFile.readBytes())
        val plain = translatingInvalidation {
            cipher.updateAAD(f.header)
            cipher.doFinal(f.ciphertext)
        }
        return parsePrivate(plain)
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
        } catch (e: IOException) {
            tmp.delete()
            throw e
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
                if (bytes.size < HEADER_LEN + TAG_BITS / 8) throw IOException("Key file truncated")
                val b = ByteBuffer.wrap(bytes)
                val magic = ByteArray(4).also { b.get(it) }
                if (!magic.contentEquals(PIN_MAGIC)) throw IOException("Not a vault key file")
                val version = b.get()
                if (version != PIN_VERSION) throw IOException("Unsupported key file version $version")
                val salt = ByteArray(SALT_LEN).also { b.get(it) }
                val params = Argon2Params(b.int, b.int, b.int)
                // Bound the costs so a corrupted file cannot make us allocate gigabytes or spin.
                if (params.iterations !in 1..MAX_ITERATIONS ||
                    params.parallelism !in 1..MAX_PARALLELISM ||
                    params.memoryKiB !in 8 * params.parallelism..MAX_MEMORY_KIB
                ) {
                    throw IOException("Key file has invalid parameters")
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

    private class BioFile(val header: ByteArray, val iv: ByteArray, val ciphertext: ByteArray) {
        companion object {
            fun parse(bytes: ByteArray): BioFile {
                val fixed = BIO_MAGIC.size + 2
                if (bytes.size < fixed) throw IOException("Key file truncated")
                if (!bytes.copyOfRange(0, BIO_MAGIC.size).contentEquals(BIO_MAGIC)) {
                    throw IOException("Not a vault key file")
                }
                val version = bytes[BIO_MAGIC.size]
                if (version != BIO_VERSION) throw IOException("Unsupported key file version $version")
                val ivLen = bytes[BIO_MAGIC.size + 1].toInt() and 0xff
                val headerLen = fixed + ivLen
                if (ivLen == 0 || bytes.size < headerLen + TAG_BITS / 8) throw IOException("Key file truncated")
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
        private const val MAX_ITERATIONS = 64
        private const val MAX_PARALLELISM = 16
        private const val MAX_MEMORY_KIB = 4 * 65536
        private val PROBE_CONTEXT = "gs-vault-probe".toByteArray(Charsets.US_ASCII)

        private val random = SecureRandom()

        private fun deriveKey(secret: CharArray, salt: ByteArray, p: Argon2Params): ByteArray {
            val encoded = Charsets.UTF_8.encode(CharBuffer.wrap(secret))
            val password = ByteArray(encoded.remaining()).also { encoded.get(it) }
            if (encoded.hasArray()) encoded.array().fill(0)
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
