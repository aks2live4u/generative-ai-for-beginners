package com.aichiefofstaff

import android.content.Context
import com.aichiefofstaff.data.db.AppDatabase
import com.aichiefofstaff.data.network.NetworkModule
import com.aichiefofstaff.data.prefs.SecurePrefs
import com.aichiefofstaff.data.prefs.SettingsDataStore
import com.aichiefofstaff.data.repository.ChatRepository
import com.aichiefofstaff.data.repository.InboxRepository
import com.aichiefofstaff.data.repository.ProjectRepository
import com.aichiefofstaff.data.repository.TaskRepository

class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

    private val openAiApi = NetworkModule.buildOpenAiApi()

    val securePrefs = SecurePrefs(context)
    val settingsDataStore = SettingsDataStore(context)

    val taskRepository = TaskRepository(database.taskDao())
    val projectRepository = ProjectRepository(database.projectDao())
    val inboxRepository = InboxRepository(database.inboxDao())
    val chatRepository = ChatRepository(
        conversationDao = database.conversationDao(),
        messageDao = database.messageDao(),
        openAiApi = openAiApi,
        securePrefs = securePrefs
    )

    suspend fun wipeAllData() {
        database.clearAllTables()
        securePrefs.clearAll()
    }
}
