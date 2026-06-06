package com.notnow.app.ui.screen.websites

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.BlockedWebsite
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.ui.theme.*

@Composable
fun BlockedWebsitesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NotNowApplication
    val vm: BlockedWebsitesViewModel = viewModel(
        factory = BlockedWebsitesViewModel.Factory(app.blockedWebsiteRepository)
    )

    val sites by vm.sites.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepNavy,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                }
                Text("Block Websites", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentAmber,
                contentColor = DeepNavy
            ) {
                Icon(Icons.Default.Add, "Add website")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(10.dp), color = CardDark) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Info, null, tint = AccentAmber, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Text(
                            "Works in Chrome. Enter just the domain — e.g. pornhub.com, reddit.com.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (sites.isEmpty()) {
                item {
                    Text(
                        "No blocked sites yet.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            }

            items(sites, key = { it.id }) { site ->
                SiteRow(
                    site = site,
                    onToggle = { vm.toggleSite(site.id, it) },
                    onDelete = { vm.deleteSite(site) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddSiteDialog(
            onAdd = { domain, label, level ->
                vm.addSite(domain, label, level)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun SiteRow(site: BlockedWebsite, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(shape = RoundedCornerShape(10.dp), color = if (site.isEnabled) SurfaceDark else SurfaceDark.copy(alpha = 0.5f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(site.label, style = MaterialTheme.typography.titleMedium, color = if (site.isEnabled) TextPrimary else TextSecondary)
                Text(site.domain, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(site.frictionLevel.label(), style = MaterialTheme.typography.bodyMedium, color = frictionColor(site.frictionLevel))
            }
            Switch(
                checked = site.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentAmber, checkedTrackColor = AccentAmber.copy(0.3f),
                    uncheckedThumbColor = TextSecondary, uncheckedTrackColor = BorderDark
                )
            )
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = CardDark,
            title = { Text("Remove block?", color = TextPrimary) },
            text = { Text("Remove the block on ${site.domain}?", color = TextSecondary) },
            confirmButton = {
                Button(onClick = { onDelete(); confirmDelete = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) {
                    Text("Remove", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun AddSiteDialog(onAdd: (String, String, FrictionLevel) -> Unit, onDismiss: () -> Unit) {
    var domain by remember { mutableStateOf("") }
    var label  by remember { mutableStateOf("") }
    var level  by remember { mutableStateOf(FrictionLevel.LEVEL_1_MINOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Block a Website", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain  (e.g. pornhub.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentAmber, unfocusedBorderColor = BorderDark,
                        focusedLabelColor = AccentAmber, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label  (e.g. PornHub)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentAmber, unfocusedBorderColor = BorderDark,
                        focusedLabelColor = AccentAmber, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
                Text("Delay before the site loads:", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(FrictionLevel.LEVEL_1_MINOR, FrictionLevel.LEVEL_2_ATTENTION, FrictionLevel.LEVEL_3_SPENDING)
                        .forEach { l ->
                            FilterChip(
                                selected = level == l,
                                onClick = { level = l },
                                label = { Text(l.shortLabel(), style = MaterialTheme.typography.labelLarge) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentAmber,
                                    selectedLabelColor = DeepNavy,
                                    containerColor = SurfaceDark,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(domain, label, level) },
                enabled = domain.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
            ) { Text("Block It", color = DeepNavy) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

private fun FrictionLevel.label() = when (this) {
    FrictionLevel.LEVEL_1_MINOR     -> "30 sec delay"
    FrictionLevel.LEVEL_2_ATTENTION -> "10 min delay"
    FrictionLevel.LEVEL_3_SPENDING  -> "60 min delay"
    FrictionLevel.LEVEL_4_BLOCKED   -> "Always blocked"
}

private fun FrictionLevel.shortLabel() = when (this) {
    FrictionLevel.LEVEL_1_MINOR     -> "30s"
    FrictionLevel.LEVEL_2_ATTENTION -> "10m"
    FrictionLevel.LEVEL_3_SPENDING  -> "1h"
    FrictionLevel.LEVEL_4_BLOCKED   -> "Block"
}

@Composable
private fun frictionColor(level: FrictionLevel) = when (level) {
    FrictionLevel.LEVEL_1_MINOR     -> AccentGreen
    FrictionLevel.LEVEL_2_ATTENTION -> AccentAmber
    FrictionLevel.LEVEL_3_SPENDING  -> AccentRed
    FrictionLevel.LEVEL_4_BLOCKED   -> AccentRed
}
