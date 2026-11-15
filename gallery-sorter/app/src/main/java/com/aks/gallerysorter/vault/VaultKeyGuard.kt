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

/**
 * The tier's `public.keyset` is missing, has no pin, or is not the one pinned when the vault was
 * created (e.g. replaced on shared storage). Nothing was encrypted to it.
 */
class VaultKeyMismatchException(val tier: VaultTier) : Exception("Vault public key does not match its pin")

/** The PIN/passcode does not meet its tier's rules ([validateSecret]). */
class WeakSecretException : IllegalArgumentException("Secret does not meet the vault's rules")

/**
 * The only way to get a [PublicHandle] for encrypting. At creation it pins the SHA-256 of the
 * tier's serialized public keyset in the encrypted index (settings key [pinKey]); every
 * [publicHandle] re-reads the file, compares it with the pin and parses those same bytes.
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

    /** The tier's key files (for unlocking and biometric setup). */
    fun keys(tier: VaultTier): VaultKeys = keys.getValue(tier)

    /**
     * Validates [secret] for the tier, creates the key pair (or repairs an orphaned private key,
     * see [VaultKeys.create]) and records the pin. If the keys already exist but no pin was
     * recorded (crash between the two steps), the pin is recorded only once [secret] unlocks the
     * private key and that key matches `public.keyset`. Zeroes [secret].
     */
    suspend fun createVault(tier: VaultTier, secret: CharArray) {
        try {
            if (!validateSecret(tier, secret)) throw WeakSecretException()
            val k = keys(tier)
            if (k.isCreated()) {
                check(storedPin(tier) == null) { "Vault keys already exist" }
                val priv = k.unlockWithSecret(secret.copyOf())
                val bytes = publicFile(tier).readBytes()
                if (!matches(bytes, priv)) throw VaultKeyMismatchException(tier)
                recordPin(tier, bytes)
            } else {
                k.create(secret.copyOf())
                recordPin(tier, publicFile(tier).readBytes())
            }
        } finally {
            secret.fill('\u0000')
        }
    }

    /** The pinned public key; throws [VaultKeyMismatchException] when it cannot be verified. */
    suspend fun publicHandle(tier: VaultTier): PublicHandle {
        val pin = storedPin(tier) ?: throw VaultKeyMismatchException(tier)
        val bytes = try {
            publicFile(tier).readBytes()
        } catch (e: IOException) {
            throw VaultKeyMismatchException(tier)
        }
        val actual = VaultIo.sha256(bytes)
        if (!MessageDigest.isEqual(actual.toByteArray(), pin.toByteArray())) throw VaultKeyMismatchException(tier)
        return try {
            PublicHandle(TinkProtoKeysetFormat.parseKeysetWithoutSecret(bytes))
        } catch (e: GeneralSecurityException) {
            throw VaultKeyMismatchException(tier)
        }
    }

    private fun publicFile(tier: VaultTier) = File(VaultKeys.keysDir(store.root(tier)), VaultKeys.PUBLIC_FILE)

    private suspend fun storedPin(tier: VaultTier): String? = settings.get(pinKey(tier))?.takeIf { it.isNotEmpty() }

    private suspend fun recordPin(tier: VaultTier, publicKeyset: ByteArray) =
        settings.put(SettingRow(pinKey(tier), VaultIo.sha256(publicKeyset)))

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
    }
}
