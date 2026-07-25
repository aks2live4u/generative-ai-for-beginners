package com.accounting.engine.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.accounting.engine.data.entity.AccountEntity
import com.accounting.engine.data.entity.AccountType
import com.accounting.engine.data.entity.JournalEntryEntity
import com.accounting.engine.data.entity.LineItemEntity
import com.accounting.engine.data.entity.TransactionEntity
import net.sqlcipher.database.SQLiteDatabase as CipherSQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.util.UUID

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        JournalEntryEntity::class,
        LineItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountingDao(): AccountingDao

    companion object {
        private const val DATABASE_NAME = "autonomous_accounting.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            CipherSQLiteDatabase.loadLibs(context)
            val passphrase = DatabasePassphraseProvider.getOrCreatePassphrase(context)
            val factory = SupportFactory(CipherSQLiteDatabase.getBytes(passphrase))

            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .openHelperFactory(factory)
                .addCallback(SeedChartOfAccountsCallback)
                .build()
        }
    }

    /**
     * Seeds the five master root ledger accounts the moment the encrypted database file is
     * created, via a raw insert against the just-created `accounts` table. This runs
     * synchronously inside Room's own onCreate callback rather than through the Dao/coroutine
     * path, so it doesn't race the not-yet-assigned [instance] reference.
     */
    private object SeedChartOfAccountsCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val now = System.currentTimeMillis()
            AccountType.entries.forEach { type ->
                val values = ContentValues().apply {
                    put("id", UUID.randomUUID().toString())
                    put("name", AccountType.rootAccountName(type))
                    put("type", type.name)
                    putNull("parentAccountId")
                    put("isSystemAccount", 1)
                    put("createdAt", now)
                }
                db.insert("accounts", SQLiteDatabase.CONFLICT_IGNORE, values)
            }
        }
    }
}
