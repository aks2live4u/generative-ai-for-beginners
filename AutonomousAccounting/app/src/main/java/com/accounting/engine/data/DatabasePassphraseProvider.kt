package com.accounting.engine.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Generates (once) and retrieves a random SQLCipher passphrase, itself protected at
 * rest by an AES-256-GCM key held in the Android Keystore via [EncryptedSharedPreferences].
 * The raw passphrase never leaves the device and is never logged.
 */
object DatabasePassphraseProvider {

    private const val PREFS_FILE_NAME = "accounting_engine_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_BYTE_LENGTH = 32

    fun getOrCreatePassphrase(context: Context): CharArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            .build()

        val securePrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        securePrefs.getString(KEY_DB_PASSPHRASE, null)?.let { return it.toCharArray() }

        val randomBytes = ByteArray(PASSPHRASE_BYTE_LENGTH).also { SecureRandom().nextBytes(it) }
        val passphrase = randomBytes.joinToString("") { "%02x".format(it) }
        securePrefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
        return passphrase.toCharArray()
    }
}
