package com.aks.gallerysorter.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.SetupResult
import com.aks.gallerysorter.ui.fragmentActivity
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.HIGH_SECURITY_UNINSTALL_WARNING
import com.aks.gallerysorter.ui.NEW_VAULT_WARNING
import com.aks.gallerysorter.ui.grid.ConfirmDialog
import com.aks.gallerysorter.ui.startNewStandardVault
import com.aks.gallerysorter.ui.label
import com.aks.gallerysorter.ui.onboarding.ToggleRow
import com.aks.gallerysorter.ui.secretNoun
import com.aks.gallerysorter.ui.setUpVault
import com.aks.gallerysorter.ui.settings.SettingsViewModel.Companion.LONG_PIN
import com.aks.gallerysorter.ui.settings.SettingsViewModel.Companion.SHORT_PIN
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onPastPhones: () -> Unit, onAbout: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val vm = graphViewModel { g ->
        SettingsViewModel({ g.db }) {
            VaultTier.entries.associateWith {
                VaultStatus(g.guard.isSetUp(it), g.guard.keys(it).isBiometricEnabled(), existing = g.guard.hasUnpinnedKeys(it))
            }
        }
    }
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var confirmNew by remember { mutableStateOf(false) }
    fun report(r: SetupResult) = when (r) {
        is SetupResult.Failed -> graph.notify(r.message)
        is SetupResult.Done -> graph.notify(if (r.fromEarlierInstall) "Vault restored; unlock it to recover its items" else "Vault set up")
        SetupResult.Cancelled -> Unit
    }
    fun act(block: suspend () -> Unit) = scope.launch { block(); vm.refresh() }
    fun minFor(tier: VaultTier) = when {
        tier == VaultTier.HIGH_SECURITY -> 10
        state.longStandardPin -> LONG_PIN
        else -> SHORT_PIN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Section("Vaults")
            for (tier in VaultTier.entries) {
                val v = state.vaults[tier]
                val noun = tier.secretNoun()
                Text(tier.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                if (tier == VaultTier.HIGH_SECURITY) Text(HIGH_SECURITY_UNINSTALL_WARNING, style = MaterialTheme.typography.bodySmall)
                if (v?.created == true) {
                    TextButton(onClick = { act { changeSecret(context, tier, minFor(tier)) } }) { Text("Change $noun") }
                    ToggleRow("Unlock with fingerprint", v.fingerprint) { on -> act { setFingerprint(context, tier, on) } }
                } else if (v != null) {
                    // Keys from an earlier install: the same flow as onboarding (existing PIN, or archive and start anew).
                    TextButton(onClick = {
                        act {
                            val activity = context.fragmentActivity() ?: return@act
                            report(setUpVault(activity, graph, tier, minFor(tier), fingerprint = false))
                        }
                    }) { Text(if (v.existing) "Enter existing $noun" else "Set up") }
                    if (v.existing && tier == VaultTier.STANDARD) TextButton(onClick = { confirmNew = true }) { Text("Forgot it? Start a new vault") }
                }
            }
            ToggleRow("Use a longer standard passcode ($LONG_PIN+ digits)", state.longStandardPin) { on ->
                when {
                    !on -> vm.setLongStandardPin(false)
                    // Recorded only once the longer passcode is actually in place.
                    state.vaults[VaultTier.STANDARD]?.created == true -> act {
                        if (changeSecret(context, VaultTier.STANDARD, LONG_PIN)) vm.setLongStandardPin(true).join()
                    }
                    else -> vm.setLongStandardPin(true) // applies when the vault is set up
                }
            }

            Section("Default vault per category")
            for ((category, tier) in state.tiers) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(category.label, Modifier.weight(1f))
                    VaultTier.entries.forEach { t ->
                        FilterChip(selected = tier == t, onClick = { vm.setTier(category, t) }, label = { Text(t.label) }, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            Section("Scanning")
            ToggleRow("Automatically hide new Sensitive items", state.autoHide, vm::setAutoHide)
            ToggleRow("First scan only while charging", state.onlyWhileCharging, vm::setOnlyWhileCharging)

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text("My past phones") },
                supportingContent = { Text("Photos from these cameras count as taken on your camera") },
                modifier = Modifier.clickable(onClick = onPastPhones),
            )
            ListItem(headlineContent = { Text("About") }, modifier = Modifier.clickable(onClick = onAbout))
        }
    }

    if (confirmNew) {
        ConfirmDialog(
            title = "Start a new vault?",
            text = NEW_VAULT_WARNING,
            confirm = "Start new vault",
            onConfirm = {
                confirmNew = false
                act {
                    val activity = context.fragmentActivity() ?: return@act
                    report(startNewStandardVault(activity, graph, minFor(VaultTier.STANDARD), fingerprint = false))
                }
            },
            onDismiss = { confirmNew = false },
        )
    }
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}
