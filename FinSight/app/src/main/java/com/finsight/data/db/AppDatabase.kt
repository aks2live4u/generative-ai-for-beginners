package com.finsight.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finsight.data.db.dao.GoalDao
import com.finsight.data.db.dao.MerchantDao
import com.finsight.data.db.dao.SubscriptionDao
import com.finsight.data.db.dao.TransactionDao
import com.finsight.data.db.entity.GoalEntity
import com.finsight.data.db.entity.MerchantEntity
import com.finsight.data.db.entity.SubscriptionEntity
import com.finsight.data.db.entity.TransactionEntity
import com.finsight.security.DatabaseKeyManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [TransactionEntity::class, MerchantEntity::class, SubscriptionEntity::class, GoalEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantDao(): MerchantDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun goalDao(): GoalDao

    companion object {
        private const val DB_NAME = "finsight.db"

        @Volatile private var instance: AppDatabase? = null

        // Existing rows default to 99 (already trusted/displayed) rather than retroactively
        // dumping a user's entire transaction history into the new review queue on upgrade -
        // confidence scoring only applies going forward to newly-parsed messages.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN confidence INTEGER NOT NULL DEFAULT 99")
            }
        }

        /** All financial records live in this single SQLCipher-encrypted, on-device database. */
        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }

        private fun build(context: Context): AppDatabase {
            SQLiteDatabase.loadLibs(context)
            val passphrase = DatabaseKeyManager(context).getOrCreatePassphrase()
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase))
            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
