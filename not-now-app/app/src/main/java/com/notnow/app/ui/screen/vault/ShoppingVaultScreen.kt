package com.notnow.app.ui.screen.vault

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.ShoppingVaultItem

@Composable
fun ShoppingVaultScreen(app: NotNowApplication, onBack: () -> Unit) {
    val vm: ShoppingVaultViewModel = viewModel(factory = ShoppingVaultViewModelFactory(app.shoppingVaultRepository))
    val items by vm.items.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        AddItemDialog(onAdd = { title, price, url -> vm.add(title, price, url); showAdd = false }, onDismiss = { showAdd = false })
    }

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
            Text("Shopping Vault", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFFE85D04))
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No items saved yet.\n\nNext time a shopping app opens,\nchoose 'Save For Later'.",
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    VaultItemCard(item = item, ageLabel = vm.ageLabel(item.savedAt), onBuy = { vm.markPurchased(item.id) }, onRemove = { vm.remove(item.id) })
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun VaultItemCard(item: ShoppingVaultItem, ageLabel: String, onBuy: () -> Unit, onRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (item.price.isNotBlank()) Text(item.price, color = Color(0xFFE85D04), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Saved $ageLabel · ${item.sourceApp.ifBlank { "manually" }}", color = Color(0xFF666666), fontSize = 12.sp)
            }
        }

        val dayOld = System.currentTimeMillis() - item.savedAt > 86_400_000L
        if (dayOld) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2000))
                    .padding(10.dp)
            ) {
                Text("You saved this $ageLabel. Do you still want it?", color = Color(0xFFFFCC44), fontSize = 13.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onRemove,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888))
            ) { Text("Remove") }
            Button(
                onClick = onBuy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE85D04))
            ) { Text("Bought It") }
        }
    }
}

@Composable
private fun AddItemDialog(onAdd: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Add to Vault", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("What is it?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE85D04), focusedLabelColor = Color(0xFFE85D04), unfocusedBorderColor = Color(0xFF333333), focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedLabelColor = Color(0xFF888888), cursorColor = Color(0xFFE85D04))
                )
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Price (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE85D04), focusedLabelColor = Color(0xFFE85D04), unfocusedBorderColor = Color(0xFF333333), focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedLabelColor = Color(0xFF888888), cursorColor = Color(0xFFE85D04))
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onAdd(title, price, "") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE85D04))) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF888888)) } }
    )
}
