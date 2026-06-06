package com.notnow.app.ui.screen.customrules

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.ui.theme.*

@Composable
fun CustomRulesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NotNowApplication
    val vm: CustomRulesViewModel = viewModel(
        factory = CustomRulesViewModel.Factory(app.appRuleRepository, context)
    )

    val rules by vm.allRules.collectAsStateWithLifecycle()
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
                Text("Block Apps", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentAmber,
                contentColor = DeepNavy
            ) {
                Icon(Icons.Default.Add, "Add app")
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
                Text(
                    "Tap + to pick any installed app and set a delay before it opens.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (rules.isEmpty()) {
                item {
                    Text(
                        "No rules yet — tap + to add an app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            }

            items(rules, key = { it.packageName }) { rule ->
                RuleRow(
                    rule = rule,
                    onToggle = { vm.toggleRule(rule.packageName, it) },
                    onDelete = { vm.deleteRule(rule) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddAppDialog(
            vm = vm,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun RuleRow(rule: AppRule, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(shape = RoundedCornerShape(10.dp), color = if (rule.isEnabled) SurfaceDark else SurfaceDark.copy(alpha = 0.5f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(rule.appName, style = MaterialTheme.typography.titleMedium, color = if (rule.isEnabled) TextPrimary else TextSecondary)
                Text(rule.frictionLevel.label(), style = MaterialTheme.typography.bodyMedium, color = frictionColor(rule.frictionLevel))
            }
            Switch(
                checked = rule.isEnabled,
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
            title = { Text("Remove rule?", color = TextPrimary) },
            text = { Text("Remove the block on ${rule.appName}?", color = TextSecondary) },
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
private fun AddAppDialog(vm: CustomRulesViewModel, onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var selectedLevel by remember { mutableStateOf(FrictionLevel.LEVEL_1_MINOR) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        installedApps = vm.getInstalledApps()
        loading = false
    }

    val filtered = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    if (selectedApp == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = CardDark,
            title = { Text("Pick an App", color = TextPrimary) },
            text = {
                Column(Modifier.height(400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search apps…", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentAmber, unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) }
                    )
                    if (loading) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentAmber)
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(filtered, key = { it.packageName }) { app ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceDark,
                                    onClick = { selectedApp = app }
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column {
                                            Text(app.appName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                            Text(app.packageName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = CardDark,
            title = { Text("Set Delay for\n${selectedApp!!.appName}", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How much friction before opening?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    FrictionLevel.entries.filter { it != FrictionLevel.LEVEL_4_BLOCKED }.forEach { level ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedLevel == level) AccentAmber.copy(alpha = 0.15f) else SurfaceDark,
                            onClick = { selectedLevel = level },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedLevel == level,
                                    onClick = { selectedLevel = level },
                                    colors = RadioButtonDefaults.colors(selectedColor = AccentAmber)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(level.label(), color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.addRule(selectedApp!!, selectedLevel); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                ) { Text("Add Rule", color = DeepNavy) }
            },
            dismissButton = {
                TextButton(onClick = { selectedApp = null }) { Text("Back", color = TextSecondary) }
            }
        )
    }
}

private fun FrictionLevel.label() = when (this) {
    FrictionLevel.LEVEL_1_MINOR     -> "30 sec delay"
    FrictionLevel.LEVEL_2_ATTENTION -> "10 min delay"
    FrictionLevel.LEVEL_3_SPENDING  -> "60 min delay"
    FrictionLevel.LEVEL_4_BLOCKED   -> "Always blocked"
}

@Composable
private fun frictionColor(level: FrictionLevel) = when (level) {
    FrictionLevel.LEVEL_1_MINOR     -> AccentGreen
    FrictionLevel.LEVEL_2_ATTENTION -> AccentAmber
    FrictionLevel.LEVEL_3_SPENDING  -> AccentRed
    FrictionLevel.LEVEL_4_BLOCKED   -> AccentRed
}
