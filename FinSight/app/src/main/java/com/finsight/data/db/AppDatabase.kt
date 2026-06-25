package com.finsight.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
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
                .build()
        }
    }
}
