package com.aks.gallerysorter.vault

import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.dao.SettingsDao
import com.aks.gallerysorter.index.entities.SettingRow
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.TinkProtoKeysetFormat
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher

/**
 * The tier's `public.keyset` is missing, has no pin, or is not the one pinned when the vault was
 * created (e.g. replaced on shared storage). Nothing was encrypted to it.
 */
class VaultKeyMismatchException(val tier: VaultTier) : Exception("Vault public key does not match its pin")

/**
 * The tier's `private.pin.enc` is missing, malformed or not the pinned one, so files encrypted
 * now might never be decryptable. No original is deleted while this holds.
 */
class VaultPrivateKeyMissingException(val tier: VaultTier) : Exception("Vault private key file is missing or altered")

/** The PIN/passcode does not meet its tier's rules ([validateSecret]). */
class WeakSecretException : IllegalArgumentException("Secret does not meet the vault's rules")

/**
 * The only way to get a [PublicHandle] for encrypting. Pins, in the encrypted index, the SHA-256
 * of the tier's `public.keyset` ([pinKey]) and of its `private.pin.enc` ([privPinKey]).
 * [publicHandle] re-reads the public file, compares it with the pin and parses those same bytes;
 * [requirePrivateKey] confirms the private key file is still there before anything is deleted.
 * Secret changes and biometric enrolment must go through this class so the pins stay current.
 */
class VaultKeyGuard(
    private val store: VaultStore,
    private val settings: SettingsDao,
    newKeys: (dir: File) -> VaultKeys,
) {
    private val keys: Map<VaultTier, VaultKeys> =
        VaultTier.entries.associateWith { newKeys(VaultKeys.keysDir(store.root(it))) }

    init {
        VaultCrypto.ensureRegistered()
    }

    /** The tier's key files (for unlocking). Change keys only through this guard. */
    fun keys(tier: VaultTier): VaultKeys = keys.getValue(tier)

    /**
     * Validates [secret] for the tier, creates the key pair (or repairs an orphaned private key,
     * see [VaultKeys.create]) and records both pins. If the keys already exist but no public pin
     * was recorded (crash between the two steps, or a lost index), the pins are recorded only
     * once [secret] unlocks the private key and that key matches `public.keyset`. Zeroes [secret].
     */
    suspend fun createVault(tier: VaultTier, secret: CharArray) {
        try {
            if (!validateSecret(tier, secret)) throw WeakSecretException()
            val k = keys(tier)
            if (k.isCreated()) {
                check(storedPin(pinKey(tier)) == null) { "Vault keys already exist" }
                val priv = k.unlockWithSecret(secret.copyOf())
                val bytes = publicFile(tier).readBytes()
                if (!matches(bytes, priv)) throw VaultKeyMismatchException(tier)
                recordPins(tier, bytes)
            } else {
                k.create(secret.copyOf())
                recordPins(tier, publicFile(tier).readBytes())
            }
        } finally {
            secret.fill('\u0000')
        }
    }

    /** [VaultKeys.changeSecret], then re-pins `private.pin.enc`. Zeroes [newSecret]. */
    suspend fun changeSecret(tier: VaultTier, handle: PrivateHandle, newSecret: CharArray) {
        try {
            if (!validateSecret(tier, newSecret)) throw WeakSecretException()
            keys(tier).changeSecret(handle, newSecret.copyOf())
            recordPrivatePin(tier)
        } finally {
            newSecret.fill('\u0000')
        }
    }

    /** [VaultKeys.enableBiometric], then re-pins `private.pin.enc`. */
    suspend fun enableBiometric(tier: VaultTier, handle: PrivateHandle, cipher: Cipher) {
        keys(tier).enableBiometric(handle, cipher)
        recordPrivatePin(tier)
    }

    /** The pinned public key; throws [VaultKeyMismatchException] when it cannot be verified. */
    suspend fun publicHandle(tier: VaultTier): PublicHandle {
        val pin = storedPin(pinKey(tier)) ?: throw VaultKeyMismatchException(tier)
        val bytes = try {
            publicFile(tier).readBytes()
        } catch (e: IOException) {
            throw VaultKeyMismatchException(tier)
        }
        if (!sameHash(bytes, pin)) throw VaultKeyMismatchException(tier)
        return try {
            PublicHandle(TinkProtoKeysetFormat.parseKeysetWithoutSecret(bytes))
        } catch (e: GeneralSecurityException) {
            throw VaultKeyMismatchException(tier)
        }
    }

    /**
     * Throws [VaultPrivateKeyMissingException] unless `private.pin.enc` exists, parses as a key
     * file and matches its pin. Checked before any original or old copy is deleted.
     */
    suspend fun requirePrivateKey(tier: VaultTier) {
        val pin = storedPin(privPinKey(tier)) ?: throw VaultPrivateKeyMissingException(tier)
        val bytes = try {
            keys(tier).pinFileBytes()
        } catch (e: IOException) {
            throw VaultPrivateKeyMissingException(tier)
        }
        if (!sameHash(bytes, pin)) throw VaultPrivateKeyMissingException(tier)
    }

    private fun sameHash(bytes: ByteArray, pin: String) =
        MessageDigest.isEqual(VaultIo.sha256(bytes).toByteArray(), pin.toByteArray())

    private fun publicFile(tier: VaultTier) = File(VaultKeys.keysDir(store.root(tier)), VaultKeys.PUBLIC_FILE)

    private suspend fun storedPin(key: String): String? = settings.get(key)?.takeIf { it.isNotEmpty() }

    private suspend fun recordPins(tier: VaultTier, publicKeyset: ByteArray) {
        recordPrivatePin(tier)
        settings.put(SettingRow(pinKey(tier), VaultIo.sha256(publicKeyset)))
    }

    private suspend fun recordPrivatePin(tier: VaultTier) =
        settings.put(SettingRow(privPinKey(tier), VaultIo.sha256(keys(tier).pinFileBytes())))

    /** True if [publicKeyset] is the public half of [priv] (encrypt a random probe, decrypt it). */
    private fun matches(publicKeyset: ByteArray, priv: PrivateHandle): Boolean = try {
        val probe = ByteArray(32).also(SecureRandom()::nextBytes)
        val ct = TinkProtoKeysetFormat.parseKeysetWithoutSecret(publicKeyset)
            .getPrimitive(RegistryConfiguration.get(), HybridEncrypt::class.java).encrypt(probe, PROBE)
        priv.keyset.getPrimitive(RegistryConfiguration.get(), HybridDecrypt::class.java)
            .decrypt(ct, PROBE).contentEquals(probe)
    } catch (e: GeneralSecurityException) {
        false
    }

    companion object {
        private val PROBE = "gs-vault-pin-probe".toByteArray(Charsets.US_ASCII)

        fun pinKey(tier: VaultTier) = "vault.${tier.name.lowercase()}.pubhash"
        fun privPinKey(tier: VaultTier) = "vault.${tier.name.lowercase()}.privhash"
    }
}
