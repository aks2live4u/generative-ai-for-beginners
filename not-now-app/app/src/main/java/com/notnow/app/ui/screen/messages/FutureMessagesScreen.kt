package com.notnow.app.ui.screen.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.FutureMessage
import com.notnow.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureMessagesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NotNowApplication
    val vm: FutureMessagesViewModel = viewModel(factory = FutureMessagesViewModel.Factory(app.futureMessageRepository))

    val messages by vm.messages.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepNavy,
        topBar = {
            TopAppBar(
                title = { Text("Future Self Messages", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = AccentAmber)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = CardDark) {
                    Text(
                        "Write messages to yourself during moments of clarity. They appear on the countdown screen when you try to open a restricted app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("💬", style = MaterialTheme.typography.headlineLarge)
                        Text("No messages yet", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("Write something to your future self.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                        Button(onClick = { showAddDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)) {
                            Text("Write First Message", color = DeepNavy)
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                MessageCard(msg = msg, onDelete = { vm.delete(msg) })
            }
        }
    }

    if (showAddDialog) {
        AddMessageDialog(
            onDismiss = { showAddDialog = false },
            onSave = { text ->
                vm.addMessage(text)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MessageCard(msg: FutureMessage, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = CardDark) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Text("\"", style = MaterialTheme.typography.headlineMedium, color = AccentAmber, modifier = Modifier.padding(end = 8.dp, top = 2.dp))
            Text(msg.message, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AddMessageDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Write to Future Self", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Your message…") },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentAmber, unfocusedBorderColor = BorderDark,
                    focusedLabelColor = AccentAmber, unfocusedLabelColor = TextSecondary
                ),
                placeholder = { Text("e.g. \"Finish the chapter first.\"", color = TextSecondary) }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }, enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)) {
                Text("Save", color = DeepNavy)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
