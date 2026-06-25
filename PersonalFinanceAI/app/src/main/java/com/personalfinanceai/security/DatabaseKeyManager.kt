package com.personalfinanceai.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.Base64

/**
 * Generates and stores the SQLCipher database passphrase. The passphrase itself is random
 * (never derived from anything guessable) and is stored in [EncryptedSharedPreferences], which
 * wraps it with a key that lives in the Android Keystore (hardware-backed where available) -
 * the passphrase bytes never touch disk unencrypted.
 */
class DatabaseKeyManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Returns the existing database passphrase, generating and persisting one on first run. */
    fun getOrCreatePassphrase(): CharArray {
        val existing = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) return existing.toCharArray()

        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        val generated = Base64.getEncoder().encodeToString(random)
        encryptedPrefs.edit().putString(KEY_DB_PASSPHRASE, generated).apply()
        return generated.toCharArray()
    }

    companion object {
        private const val PREFS_FILE_NAME = "personalfinanceai_secure_prefs"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
    }
}
