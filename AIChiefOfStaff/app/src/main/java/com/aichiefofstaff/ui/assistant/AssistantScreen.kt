package com.aichiefofstaff.ui.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aichiefofstaff.ui.components.DepthCard
import com.aichiefofstaff.ui.util.LocalAppContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssistantScreen(onOpenConversation: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: AssistantViewModel = viewModel(factory = viewModelFactory {
        initializer { AssistantViewModel(container.chatRepository) }
    })
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createConversation("New conversation", onOpenConversation) },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New conversation")
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Text(
                "Ask your AI Chief of Staff anything — priorities, decisions, drafts.",
                modifier = Modifier.padding(padding).padding(24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    DepthCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenConversation(conversation.id) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(conversation.title, fontWeight = FontWeight.Medium)
                            Text(
                                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(conversation.updatedAt)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
