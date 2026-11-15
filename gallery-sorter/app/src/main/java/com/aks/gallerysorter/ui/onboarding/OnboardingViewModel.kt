package com.aks.gallerysorter.ui.onboarding

import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.dao.SettingsDao
import com.aks.gallerysorter.index.entities.SettingRow
import com.aks.gallerysorter.ui.SetupResult
import com.aks.gallerysorter.ui.VaultSetup
import com.aks.gallerysorter.ui.setUpVault
import com.aks.gallerysorter.ui.vaultSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The first-run steps, in the brief's order. */
enum class Step { PRIVACY, MEDIA, ALL_FILES, BACKGROUND, STANDARD_VAULT, HIGH_VAULT, BACKUP, AUTO_HIDE, SCAN }

data class OnboardingState(
    val step: Step = Step.PRIVACY,
    val vaults: Map<VaultTier, VaultSetup> = emptyMap(),
    /** Tiers set up again from an earlier install's keys: offer to recover their hidden items. */
    val recoverable: Set<VaultTier> = emptySet(),
    val error: String? = null,
    val busy: Boolean = false,
    val backupApps: List<String> = emptyList(),
    val autoHide: Boolean = false,
    val onlyWhileCharging: Boolean = true,
)

/**
 * The first-run wizard. Vaults are created only through VaultKeyGuard.createVault (which
 * validates the secret); keys left by an earlier install are re-pinned with their existing secret.
 */
class OnboardingViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val vaults = VaultTier.entries.associateWith { vaultSetup(graph, it) }
            val scan = withContext(Dispatchers.IO) { graph.scanSettings.autoHideSensitive() to graph.scanSettings.onlyWhileCharging() }
            _state.update { it.copy(vaults = vaults, backupApps = installedBackupApps(), autoHide = scan.first, onlyWhileCharging = scan.second) }
        }
    }

    fun next() = _state.update { s ->
        var i = s.step.ordinal + 1
        if (Step.entries.getOrNull(i) == Step.BACKUP && s.backupApps.isEmpty()) i++
        s.copy(step = Step.entries.getOrElse(i) { Step.SCAN }, error = null)
    }

    fun back() = _state.update { s ->
        var i = s.step.ordinal - 1
        if (Step.entries.getOrNull(i) == Step.BACKUP && s.backupApps.isEmpty()) i--
        s.copy(step = Step.entries.getOrElse(i) { Step.PRIVACY }, error = null)
    }

    /** Sets up [tier] (see [setUpVault]); keys from an earlier install then offer recovery. Main thread. */
    suspend fun setUp(activity: FragmentActivity, tier: VaultTier, minLength: Int, fingerprint: Boolean) {
        _state.update { it.copy(busy = true, error = null) }
        try {
            when (val r = setUpVault(activity, graph, tier, minLength, fingerprint)) {
                is SetupResult.Done -> _state.update {
                    it.copy(vaults = it.vaults + (tier to VaultSetup.READY), recoverable = if (r.fromEarlierInstall) it.recoverable + tier else it.recoverable)
                }
                is SetupResult.Failed -> _state.update { it.copy(error = r.message, vaults = it.vaults + (tier to vaultSetup(graph, tier))) }
                SetupResult.Cancelled -> Unit
            }
        } finally {
            _state.update { it.copy(busy = false) }
        }
    }

    fun setAutoHide(on: Boolean) {
        _state.update { it.copy(autoHide = on) }
        viewModelScope.launch(Dispatchers.IO) { graph.scanSettings.setAutoHideSensitive(on) }
    }

    fun setOnlyWhileCharging(on: Boolean) {
        _state.update { it.copy(onlyWhileCharging = on) }
        viewModelScope.launch(Dispatchers.IO) { graph.scanSettings.setOnlyWhileCharging(on) }
    }

    /** Records that the wizard is done (MainActivity then opens Home); call before leaving it. */
    suspend fun finish() = withContext(Dispatchers.IO) {
        markDone(graph.db.settingsDao())
    }

    private fun installedBackupApps(): List<String> {
        val pm = graph.appContext.packageManager
        return BACKUP_PACKAGES.filter { (pkg, _) ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }.map { it.second }.distinct()
    }

    companion object {
        private const val DONE = "onboarding.done"

        /** Declared in the manifest's <queries> (no QUERY_ALL_PACKAGES). */
        val BACKUP_PACKAGES = listOf(
            "com.google.android.apps.photos" to "Google Photos",
            "com.vivo.cloud" to "vivo Cloud",
            "com.bbk.cloud" to "vivo Cloud",
        )

        suspend fun isDone(dao: SettingsDao): Boolean = dao.get(DONE)?.toBoolean() ?: false
        suspend fun markDone(dao: SettingsDao) = dao.put(SettingRow(DONE, "true"))
    }
}
