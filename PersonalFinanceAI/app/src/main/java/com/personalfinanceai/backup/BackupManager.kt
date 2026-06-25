package com.personalfinanceai.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports/imports the already-SQLCipher-encrypted database file as-is. Because the source file
 * is encrypted at rest, the exported backup file is just as encrypted as the live database - no
 * additional encryption layer is needed, and a stolen backup file is useless without the
 * Keystore-protected passphrase that lives only on the original device.
 */
class BackupManager(private val context: Context) {

    private val dbFile: File
        get() = context.getDatabasePath(DB_NAME)

    /** Copies the encrypted database to [destination]. Returns true on success. */
    suspend fun exportTo(destination: File): Boolean = withContext(Dispatchers.IO) {
        if (!dbFile.exists()) return@withContext false
        dbFile.copyTo(destination, overwrite = true)
        true
    }

    /** Creates a timestamped backup file in the app's private backups directory (manual or scheduled). */
    suspend fun createLocalBackup(): File? = withContext(Dispatchers.IO) {
        if (!dbFile.exists()) return@withContext null
        val backupsDir = File(context.filesDir, "backups").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val destination = File(backupsDir, "personalfinanceai_backup_$timestamp.db")
        dbFile.copyTo(destination, overwrite = true)
        destination
    }

    /**
     * Restores the database from a previously exported backup file. The app must be restarted
     * (and the database connection re-opened) after calling this.
     */
    suspend fun restoreFrom(source: File): Boolean = withContext(Dispatchers.IO) {
        if (!source.exists()) return@withContext false
        source.copyTo(dbFile, overwrite = true)
        true
    }

    /**
     * Google Drive backup upload: uploads the same encrypted backup file produced by
     * [createLocalBackup] to a Drive app-data folder using the Drive REST API. Wiring this in
     * requires the same Google Cloud OAuth client used for Gmail (with the
     * drive.appdata scope added) - see README.md "Google Drive Backup" section. The interface
     * below is ready for that implementation; only raw, already-encrypted bytes are ever
     * uploaded, never decrypted financial data.
     */
    fun uploadToDriveAppData(@Suppress("UNUSED_PARAMETER") backupFile: File) {
        throw NotImplementedError(
            "Wire up Drive upload once a Google Cloud OAuth client with the " +
                "drive.appdata scope is configured. See README.md."
        )
    }

    companion object {
        private const val DB_NAME = "personalfinanceai.db"
    }
}
