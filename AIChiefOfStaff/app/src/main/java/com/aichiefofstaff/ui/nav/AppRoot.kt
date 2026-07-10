package com.aichiefofstaff.ui.nav

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aichiefofstaff.ui.assistant.AssistantChatScreen
import com.aichiefofstaff.ui.assistant.AssistantScreen
import com.aichiefofstaff.ui.components.VoiceCaptureSheet
import com.aichiefofstaff.ui.home.HomeScreen
import com.aichiefofstaff.ui.inbox.InboxScreen
import com.aichiefofstaff.ui.projects.ProjectDetailScreen
import com.aichiefofstaff.ui.projects.ProjectsScreen
import com.aichiefofstaff.ui.search.SearchScreen
import com.aichiefofstaff.ui.settings.SettingsScreen
import com.aichiefofstaff.ui.tasks.TasksScreen
import com.aichiefofstaff.ui.util.LocalAppContainer
import com.aichiefofstaff.ui.voice.VoiceCaptureViewModel
import com.aichiefofstaff.ui.voice.VoiceOutcome
import com.aichiefofstaff.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val voiceCaptureViewModel: VoiceCaptureViewModel = viewModel(factory = viewModelFactory {
        initializer {
            VoiceCaptureViewModel(
                context.applicationContext,
                container.taskRepository,
                container.inboxRepository,
                container.chatRepository
            )
        }
    })
    val voiceState by voiceCaptureViewModel.voiceState.collectAsState()
    var showVoiceSheet by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showVoiceSheet = true }

    LaunchedEffect(voiceState) {
        val current = voiceState
        if (current is VoiceState.FinalResult) {
            val outcome = voiceCaptureViewModel.handleRecognizedText(current.text)
            showVoiceSheet = false
            voiceCaptureViewModel.cancel()
            when (outcome) {
                is VoiceOutcome.TaskCreated -> snackbarHostState.showSnackbar("Task added: ${outcome.title}")
                is VoiceOutcome.NoteCaptured -> snackbarHostState.showSnackbar("Saved to inbox")
                is VoiceOutcome.RoutedToAssistant -> navController.navigate(Routes.assistantConversation(outcome.conversationId))
                is VoiceOutcome.Failed -> snackbarHostState.showSnackbar(outcome.message)
            }
        } else if (current is VoiceState.Error) {
            showVoiceSheet = false
            snackbarHostState.showSnackbar(current.message)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI Chief of Staff") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    IconButton(onClick = { navController.navigate(Destination.Search.route) }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { navController.navigate(Destination.Settings.route) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            if (currentRoute in Destination.bottomBarItems.map { it.route }) {
                NavigationBar {
                    Destination.bottomBarItems.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) showVoiceSheet = true
                    else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp, pressedElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice")
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Home.route) {
                HomeScreen(onOpenConversation = { navController.navigate(Routes.assistantConversation(it)) })
            }
            composable(Destination.Inbox.route) { InboxScreen() }
            composable(Destination.Tasks.route) { TasksScreen() }
            composable(Destination.Projects.route) {
                ProjectsScreen(onOpenProject = { navController.navigate(Routes.projectDetail(it)) })
            }
            composable(Destination.Assistant.route) {
                AssistantScreen(onOpenConversation = { navController.navigate(Routes.assistantConversation(it)) })
            }
            composable(Destination.Search.route) { SearchScreen() }
            composable(Destination.Settings.route) { SettingsScreen() }
            composable(
                Routes.PROJECT_DETAIL,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { entry ->
                ProjectDetailScreen(projectId = entry.arguments?.getLong("projectId") ?: 0L)
            }
            composable(
                Routes.ASSISTANT_CONVERSATION,
                arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
            ) { entry ->
                AssistantChatScreen(conversationId = entry.arguments?.getLong("conversationId") ?: 0L)
            }
        }
    }

    if (showVoiceSheet) {
        VoiceCaptureSheet(
            voiceState = voiceState,
            onStart = { voiceCaptureViewModel.startListening() },
            onDismiss = {
                showVoiceSheet = false
                voiceCaptureViewModel.cancel()
            }
        )
    }
}
