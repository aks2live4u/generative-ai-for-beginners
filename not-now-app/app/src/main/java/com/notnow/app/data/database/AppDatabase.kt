package com.notnow.app.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notnow.app.data.dao.*
import com.notnow.app.data.entity.*

class Converters {
    @TypeConverter fun fromFriction(v: FrictionLevel) = v.name
    @TypeConverter fun toFriction(v: String) = FrictionLevel.valueOf(v)
    @TypeConverter fun fromCategory(v: AppCategory) = v.name
    @TypeConverter fun toCategory(v: String) = AppCategory.valueOf(v)
    @TypeConverter fun fromOutcome(v: AccessOutcome) = v.name
    @TypeConverter fun toOutcome(v: String) = AccessOutcome.valueOf(v)
}

@Database(
    entities = [AppRule::class, ShoppingVaultItem::class, FutureMessage::class, UsageRecord::class, BlockedWebsite::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appRuleDao(): AppRuleDao
    abstract fun shoppingVaultDao(): ShoppingVaultDao
    abstract fun futureMessageDao(): FutureMessageDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun blockedWebsiteDao(): BlockedWebsiteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS blocked_websites (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        domain TEXT NOT NULL,
                        label TEXT NOT NULL,
                        frictionLevel TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notnow.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
    }
}
