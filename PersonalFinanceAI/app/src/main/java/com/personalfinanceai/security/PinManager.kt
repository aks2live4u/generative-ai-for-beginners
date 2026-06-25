package com.personalfinanceai.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * PIN fallback for when biometric auth is unavailable/fails. The PIN is never stored in plain
 * text - only a salted SHA-256 hash is persisted (inside Keystore-backed EncryptedSharedPreferences
 * for defense in depth).
 */
class PinManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isPinSet(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_PIN_HASH, Base64.getEncoder().encodeToString(hash))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltEncoded = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHashEncoded = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.getDecoder().decode(saltEncoded)
        val candidateHash = hash(pin, salt)
        return MessageDigest.isEqual(candidateHash, Base64.getDecoder().decode(storedHashEncoded))
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val PREFS_FILE_NAME = "personalfinanceai_pin_prefs"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
