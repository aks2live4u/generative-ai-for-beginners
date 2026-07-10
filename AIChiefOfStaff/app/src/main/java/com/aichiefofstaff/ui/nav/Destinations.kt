package com.aichiefofstaff.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Destination("home", "Home", Icons.Filled.Home)
    data object Inbox : Destination("inbox", "Inbox", Icons.Filled.Inbox)
    data object Tasks : Destination("tasks", "Tasks", Icons.Filled.CheckCircle)
    data object Projects : Destination("projects", "Projects", Icons.Filled.Folder)
    data object Assistant : Destination("assistant", "Assistant", Icons.Filled.Assistant)
    data object Search : Destination("search", "Search", Icons.Filled.Search)
    data object Settings : Destination("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomBarItems = listOf(Home, Inbox, Tasks, Projects, Assistant)
    }
}

object Routes {
    const val PROJECT_DETAIL = "project_detail/{projectId}"
    const val ASSISTANT_CONVERSATION = "assistant/{conversationId}"

    fun projectDetail(projectId: Long) = "project_detail/$projectId"
    fun assistantConversation(conversationId: Long) = "assistant/$conversationId"
}
