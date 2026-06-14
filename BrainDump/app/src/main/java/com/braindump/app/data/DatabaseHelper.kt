package com.braindump.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * All Brain Dump data lives in a local SQLite database inside the app's
 * private storage (/data/data/com.braindump.app/databases). Nothing is
 * ever synced off the device.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE thoughts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL DEFAULT '',
                mood TEXT,
                images TEXT NOT NULL DEFAULT '[]',
                audio_path TEXT,
                audio_duration INTEGER,
                audio_transcript TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_thoughts_created_at ON thoughts(created_at)")

        db.execSQL(
            """
            CREATE TABLE chat_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No previous versions yet.
    }

    companion object {
        private const val DB_NAME = "braindump.db"
        private const val DB_VERSION = 1
    }
}
