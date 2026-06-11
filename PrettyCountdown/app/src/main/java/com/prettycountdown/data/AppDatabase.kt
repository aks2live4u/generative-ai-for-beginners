package com.prettycountdown.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prettycountdown.data.model.ChecklistItem
import com.prettycountdown.data.model.Event
import com.prettycountdown.data.model.EventCollection
import com.prettycountdown.data.model.EventCollectionCrossRef

@Database(
    entities = [Event::class, ChecklistItem::class, EventCollection::class, EventCollectionCrossRef::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pretty_countdown.db"
                ).build().also { instance = it }
            }
    }
}
