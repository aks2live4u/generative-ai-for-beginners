package com.notnow.app.ui.screen.vault

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
import com.notnow.app.data.entity.ShoppingVaultItem
import com.notnow.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingVaultScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NotNowApplication
    val vm: ShoppingVaultViewModel = viewModel(factory = ShoppingVaultViewModel.Factory(app.shoppingVaultRepository))

    val activeItems by vm.activeItems.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepNavy,
        topBar = {
            TopAppBar(
                title = { Text("Shopping Vault", color = TextPrimary) },
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
            if (activeItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🛍️", style = MaterialTheme.typography.headlineLarge)
                        Text("Vault is empty", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("Items saved for later appear here.\nAfter 24 hours, we'll ask if you still want them.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }

            items(activeItems, key = { it.id }) { item ->
                VaultItemCard(
                    item = item,
                    onMarkPurchased = { vm.markPurchased(item.id) },
                    onDelete = { vm.delete(item) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, url, price ->
                vm.addItem(title, url, price)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun VaultItemCard(item: ShoppingVaultItem, onMarkPurchased: () -> Unit, onDelete: () -> Unit) {
    val savedDate = remember(item.savedAt) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.savedAt))
    }
    val isOld = System.currentTimeMillis() - item.savedAt > 24 * 60 * 60 * 1000L

    Surface(shape = RoundedCornerShape(12.dp), color = if (isOld) AccentAmber.copy(alpha = 0.08f) else CardDark) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (item.price.isNotBlank()) Text(item.price, style = MaterialTheme.typography.bodyMedium, color = AccentAmber)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            if (isOld) {
                Surface(shape = RoundedCornerShape(8.dp), color = AccentAmber.copy(alpha = 0.15f)) {
                    Text(
                        "You saved this yesterday — do you still want it?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentAmber,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Text("Saved $savedDate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f),
                    border = ButtonDefaults.outlinedButtonBorder.copy()) {
                    Text("Not Anymore", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                }
                Button(onClick = onMarkPurchased, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)) {
                    Text("Purchased ✓", color = DeepNavy, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var url   by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Save to Vault", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item name") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentAmber, unfocusedBorderColor = BorderDark, focusedLabelColor = AccentAmber, unfocusedLabelColor = TextSecondary))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentAmber, unfocusedBorderColor = BorderDark, focusedLabelColor = AccentAmber, unfocusedLabelColor = TextSecondary))
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Link (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentAmber, unfocusedBorderColor = BorderDark, focusedLabelColor = AccentAmber, unfocusedLabelColor = TextSecondary))
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onSave(title, url, price) }, enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)) {
                Text("Save", color = DeepNavy)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
