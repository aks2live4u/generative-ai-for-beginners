package com.aks.gallerysorter.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * A biometric-bound AES-256-GCM key that wraps a vault's private keyset (`private.bio.enc`).
 * The ciphers it returns are only usable after the user authenticates them through
 * BiometricPrompt (as a CryptoObject).
 */
interface KeyWrapper {
    /** An encrypt cipher (fresh IV); creates the key, or replaces an invalidated one. */
    fun encryptCipher(): Cipher

    /**
     * A decrypt cipher for [iv]. Throws [BiometricInvalidatedException] (or the platform's
     * KeyPermanentlyInvalidatedException) if the key is gone or invalidated.
     */
    fun decryptCipher(iv: ByteArray): Cipher

    /** Deletes the key. */
    fun delete()
}

/**
 * Android Keystore implementation: user authentication required for every use, Class 3
 * (BIOMETRIC_STRONG) only, and invalidated when a new fingerprint is enrolled.
 * One alias per vault tier. Not unit-tested (needs a real Keystore).
 */
class KeystoreKeyWrapper(private val alias: String) : KeyWrapper {

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(alias, null) as SecretKey?

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            // Timeout 0 = every use must be authorised through a BiometricPrompt CryptoObject.
            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    override fun encryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: generateKey())
        } catch (e: KeyPermanentlyInvalidatedException) {
            delete()
            cipher.init(Cipher.ENCRYPT_MODE, generateKey())
        }
        return cipher
    }

    override fun decryptCipher(iv: ByteArray): Cipher {
        val key = existingKey() ?: throw BiometricInvalidatedException()
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
    }

    override fun delete() {
        val ks = keyStore()
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
