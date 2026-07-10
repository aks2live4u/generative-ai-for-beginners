package com.aichiefofstaff.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aichiefofstaff.data.db.dao.ConversationDao
import com.aichiefofstaff.data.db.dao.InboxDao
import com.aichiefofstaff.data.db.dao.MessageDao
import com.aichiefofstaff.data.db.dao.ProjectDao
import com.aichiefofstaff.data.db.dao.TaskDao
import com.aichiefofstaff.data.db.entity.ConversationEntity
import com.aichiefofstaff.data.db.entity.InboxItemEntity
import com.aichiefofstaff.data.db.entity.MessageEntity
import com.aichiefofstaff.data.db.entity.ProjectEntity
import com.aichiefofstaff.data.db.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        ProjectEntity::class,
        InboxItemEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun inboxDao(): InboxDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_chief_of_staff.db"
                ).build().also { instance = it }
            }
    }
}
