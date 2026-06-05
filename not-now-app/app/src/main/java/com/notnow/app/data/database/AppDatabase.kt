package com.notnow.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
    entities = [AppRule::class, ShoppingVaultItem::class, FutureMessage::class, UsageRecord::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appRuleDao(): AppRuleDao
    abstract fun shoppingVaultDao(): ShoppingVaultDao
    abstract fun futureMessageDao(): FutureMessageDao
    abstract fun usageRecordDao(): UsageRecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notnow.db"
                ).build().also { INSTANCE = it }
            }
    }
}
