package com.aks.gallerysorter.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.scan.ScanWorker
import com.aks.gallerysorter.ui.canUseFingerprint
import com.aks.gallerysorter.ui.fragmentActivity
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.HIGH_SECURITY_UNINSTALL_WARNING
import com.aks.gallerysorter.ui.NEW_VAULT_WARNING
import com.aks.gallerysorter.ui.grid.ConfirmDialog
import com.aks.gallerysorter.ui.VaultSetup
import com.aks.gallerysorter.ui.secretNoun
import com.aks.gallerysorter.ui.settings.SettingsViewModel
import com.aks.gallerysorter.ui.unlock
import kotlinx.coroutines.launch

/** The first-run wizard; [onDone] runs after completion has been recorded. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val vm = graphViewModel { OnboardingViewModel(it) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()

    fun finish(start: Boolean, anyway: Boolean = false) = scope.launch {
        if (!vm.finish()) return@launch
        if (start) {
            if (!anyway && graph.scanNeedsCharger()) {
                // The charger-constrained worker starts the first scan once plugged in.
                graph.appScope.launch { runCatching { ScanWorker.enqueueOnChange(graph.appContext) } }
                graph.notify("The scan starts when the phone is charging")
            } else {
                graph.startScan()
                graph.notify("Scan started")
            }
        }
        onDone()
    }

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            LinearProgressIndicator(progress = { (state.step.ordinal + 1f) / Step.entries.size }, modifier = Modifier.fillMaxWidth())
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state.step) {
                    Step.PRIVACY -> StepText(
                        "Your gallery, sorted privately",
                        "Gallery Sorter sorts your photos and videos into folders on this phone. It has no internet permission: " +
                            "nothing is uploaded, and every model runs on the device. Sorting never copies or moves your files. " +
                            "Hidden items are encrypted into a vault that only your fingerprint or PIN opens.",
                    )
                    Step.MEDIA -> MediaPermissionStep()
                    Step.ALL_FILES -> AllFilesStep()
                    Step.BACKGROUND -> BackgroundStep()
                    Step.STANDARD_VAULT -> VaultStep(vm, state, VaultTier.STANDARD)
                    Step.HIGH_VAULT -> VaultStep(vm, state, VaultTier.HIGH_SECURITY)
                    Step.BACKUP -> StepText(
                        "Cloud backup",
                        "${state.backupApps.joinToString(" and ")} is installed. Photos it already uploaded stay in the cloud: " +
                            "remove sensitive ones there yourself. Hidden items stop being uploaded, because they leave the gallery.",
                    )
                    Step.AUTO_HIDE -> {
                        StepText("Hide new sensitive items automatically?", "New photos detected as Sensitive go straight into their vault. You can change this in Settings.")
                        ToggleRow("Automatically hide new Sensitive items", state.autoHide, vm::setAutoHide)
                    }
                    Step.SCAN -> {
                        StepText("Start sorting", "The first scan looks at every photo and video once. It can take a while; you can use the phone meanwhile.")
                        ToggleRow("Only while charging", state.onlyWhileCharging, vm::setOnlyWhileCharging)
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (state.step != Step.PRIVACY) TextButton(onClick = vm::back) { Text("Back") }
                Spacer(Modifier.weight(1f))
                when (state.step) {
                    // The standard vault is required, unless an earlier install's vault can't be opened (see VaultStep).
                    Step.STANDARD_VAULT -> Button(
                        enabled = state.vaults[VaultTier.STANDARD] == VaultSetup.READY || state.standardLater,
                        onClick = vm::next,
                    ) { Text("Next") }
                    Step.HIGH_VAULT -> if (state.vaults[VaultTier.HIGH_SECURITY] == VaultSetup.READY) {
                        Button(onClick = vm::next) { Text("Next") }
                    } else {
                        TextButton(onClick = vm::next) { Text("Set up later") }
                    }
                    Step.SCAN -> {
                        TextButton(onClick = { finish(start = false) }) { Text("Later") }
                        if (state.onlyWhileCharging) TextButton(onClick = { finish(start = true, anyway = true) }) { Text("Start now") }
                        Button(onClick = { finish(start = true) }) { Text("Start scan") }
                    }
                    else -> Button(onClick = vm::next) { Text("Next") }
                }
            }
        }
    }
}

@Composable
private fun VaultStep(vm: OnboardingViewModel, state: OnboardingState, tier: VaultTier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val setup = state.vaults[tier]
    val noun = tier.secretNoun()
    var fingerprint by rememberSaveable { mutableStateOf(true) }
    var confirmNew by rememberSaveable { mutableStateOf(false) }
    val fingerprintAvailable = remember { context.fragmentActivity()?.let(::canUseFingerprint) ?: false }
    if (tier == VaultTier.STANDARD) {
        StepText(
            "Your PIN and fingerprint",
            "The standard vault opens with your fingerprint or a PIN of 6 or more digits. It is stored on the phone's storage " +
                "in a hidden folder, so it survives reinstalling the app. If you forget the PIN and lose the fingerprint, " +
                "the hidden items cannot be recovered.",
        )
    } else {
        StepText(
            "High-security vault (optional)",
            "For your most private items: stored inside the app, out of reach of other apps, file managers and USB. " +
                "It has its own passcode of 10 or more characters (letters allowed), plus an optional fingerprint.",
        )
        Text(HIGH_SECURITY_UNINSTALL_WARNING + " There is no way to recover them.", color = MaterialTheme.colorScheme.error)
    }
    when (setup) {
        VaultSetup.READY -> {
            Text("Set up ✓")
            if (tier in state.recoverable) {
                Text("This vault is from an earlier install. Unlock it to bring back its hidden items.")
                OutlinedButton(onClick = { scope.launch { unlock(context, tier) } }) { Text("Recover hidden items") }
            }
        }
        null -> Text("Checking…")
        else -> {
            if (setup == VaultSetup.EXISTING) Text("This phone already has this vault from an earlier install. Enter your existing $noun.")
            if (fingerprintAvailable) ToggleRow("Also unlock with fingerprint", fingerprint) { fingerprint = it }
            Button(enabled = !state.busy, onClick = {
                val activity = context.fragmentActivity() ?: return@Button
                scope.launch {
                    val min = if (tier == VaultTier.STANDARD) SettingsViewModel.SHORT_PIN else 10
                    vm.setUp(activity, tier, min, fingerprint && fingerprintAvailable)
                }
            }) { Text(if (setup == VaultSetup.EXISTING) "Enter existing $noun" else "Set $noun") }
            if (setup == VaultSetup.EXISTING && tier == VaultTier.STANDARD) {
                Text("Forgot it?")
                OutlinedButton(enabled = !state.busy, onClick = { confirmNew = true }) { Text("Start a new vault") }
                TextButton(enabled = !state.busy, onClick = vm::continueWithoutStandard) { Text("Continue without the standard vault for now") }
                Text(
                    "Without it, Sensitive and Documents can't be hidden in the standard vault until you set it up in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    if (confirmNew) {
        ConfirmDialog(
            title = "Start a new vault?",
            text = NEW_VAULT_WARNING,
            confirm = "Start new vault",
            onConfirm = {
                confirmNew = false
                val activity = context.fragmentActivity() ?: return@ConfirmDialog
                scope.launch { vm.startNewStandard(activity, SettingsViewModel.SHORT_PIN, fingerprint && fingerprintAvailable) }
            },
            onDismiss = { confirmNew = false },
        )
    }
}

@Composable
fun StepText(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Text(body, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
