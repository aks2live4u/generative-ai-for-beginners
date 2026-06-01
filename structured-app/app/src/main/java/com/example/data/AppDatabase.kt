package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TimelineTask::class,
        BrainDump::class,
        MemoryItem::class,
        Habit::class,
        MoodCheckin::class,
        TimeBlock::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun timelineTaskDao(): TimelineTaskDao
    abstract fun brainDumpDao(): BrainDumpDao
    abstract fun memoryItemDao(): MemoryItemDao
    abstract fun habitDao(): HabitDao
    abstract fun moodCheckinDao(): MoodCheckinDao
    abstract fun timeBlockDao(): TimeBlockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "structured_plus_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
