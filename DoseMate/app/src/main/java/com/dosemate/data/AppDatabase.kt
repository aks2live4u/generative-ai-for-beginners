package com.dosemate.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE medicines ADD COLUMN timesCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE medicines ADD COLUMN quantityAvailable INTEGER")
        db.execSQL("ALTER TABLE medicines ADD COLUMN dosesPerIntake INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(entities = [Medicine::class, MedicineLog::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun medicineLogDao(): MedicineLogDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dosemate.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
