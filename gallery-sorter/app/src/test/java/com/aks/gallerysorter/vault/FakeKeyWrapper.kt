package com.aks.gallerysorter.vault

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * In-memory stand-in for the Keystore wrapper: a plain AES-256 key, no user authentication.
 * [invalidate] simulates a new fingerprint being enrolled ([onInvalidated] decides what the
 * "Keystore" throws, so a Robolectric test can throw the real KeyPermanentlyInvalidatedException).
 */
class FakeKeyWrapper : KeyWrapper {
    private var key: SecretKey? = null
    var onInvalidated: (() -> Nothing)? = null

    private fun key(): SecretKey =
        key ?: KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also { key = it }

    override fun encryptCipher(): Cipher {
        if (onInvalidated != null) { onInvalidated = null; key = null }
        return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
    }

    override fun decryptCipher(iv: ByteArray): Cipher {
        onInvalidated?.invoke()
        val k = key ?: throw BiometricInvalidatedException()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(128, iv))
        }
    }

    override fun delete() { key = null }

    fun invalidate(thrower: () -> Nothing) { onInvalidated = thrower }
}
