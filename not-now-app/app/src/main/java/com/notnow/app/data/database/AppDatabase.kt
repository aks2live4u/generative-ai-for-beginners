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
    @TypeConverter fun fromFrictionLevel(v: FrictionLevel): String = v.name
    @TypeConverter fun toFrictionLevel(v: String): FrictionLevel = FrictionLevel.valueOf(v)

    @TypeConverter fun fromAppCategory(v: AppCategory): String = v.name
    @TypeConverter fun toAppCategory(v: String): AppCategory = AppCategory.valueOf(v)

    @TypeConverter fun fromInteractionType(v: InteractionType): String = v.name
    @TypeConverter fun toInteractionType(v: String): InteractionType = InteractionType.valueOf(v)
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
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notnow.db"
                ).build().also { instance = it }
            }
    }
}
