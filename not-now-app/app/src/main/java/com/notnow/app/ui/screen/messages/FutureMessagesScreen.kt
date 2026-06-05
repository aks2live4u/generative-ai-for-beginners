package com.notnow.app.ui.screen.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.FutureMessage

@Composable
fun FutureMessagesScreen(app: NotNowApplication, onBack: () -> Unit) {
    val vm: FutureMessagesViewModel = viewModel(factory = FutureMessagesViewModelFactory(app.futureMessageRepository))
    val messages by vm.messages.collectAsStateWithLifecycle()
    var newMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D)).statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Future Self Messages", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Shown during unlock requests", color = Color(0xFF666666), fontSize = 12.sp)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                placeholder = { Text("Write something during a clear-headed moment…", color = Color(0xFF555555)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE85D04),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFE85D04)
                ),
                maxLines = 3,
                trailingIcon = {
                    if (newMessage.isNotBlank()) {
                        IconButton(onClick = { vm.add(newMessage); newMessage = "" }) {
                            Icon(Icons.Default.Send, contentDescription = "Add", tint = Color(0xFFE85D04))
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No messages yet.\n\nWrite something honest\nto your future self.",
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageCard(message = message, onDelete = { vm.delete(message.id) })
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun MessageCard(message: FutureMessage, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "\"${message.message}\"",
            color = Color(0xFFDDDDDD),
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFF444444), modifier = Modifier.size(18.dp))
        }
    }
}
