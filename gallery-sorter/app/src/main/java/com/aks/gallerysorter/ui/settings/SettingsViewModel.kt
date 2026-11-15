package com.aks.gallerysorter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.dao.SettingsDao
import com.aks.gallerysorter.index.entities.SettingRow
import com.aks.gallerysorter.scan.ScanSettings
import com.aks.gallerysorter.vault.BUILT_IN_TIERS
import com.aks.gallerysorter.vault.TierSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A tier's vault: set up, fingerprint unlock, or [existing] keys from an earlier install that still need their secret. */
data class VaultStatus(val created: Boolean, val fingerprint: Boolean, val existing: Boolean = false)

data class SettingsState(
    val loaded: Boolean = false,
    /** The default tier of each lockable category (user choice, else built-in). */
    val tiers: Map<Category, VaultTier> = emptyMap(),
    val autoHide: Boolean = false,
    val onlyWhileCharging: Boolean = true,
    val longStandardPin: Boolean = false,
    val vaults: Map<VaultTier, VaultStatus> = emptyMap(),
)

/**
 * Settings stored in the index. Default tiers go to `tier.<CATEGORY>` ([TierSettings]), read by
 * the hide dialog and auto-hide. [vaults] reports each tier's status (read on IO by the caller).
 */
class SettingsViewModel(
    private val db: () -> AppDatabase,
    private val vaults: suspend () -> Map<VaultTier, VaultStatus> = { emptyMap() },
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh(): Job = io {
        val dao = db().settingsDao()
        val scan = ScanSettings(dao)
        val overrides = TierSettings(dao).overrides()
        _state.value = SettingsState(
            loaded = true,
            tiers = Category.lockedExcluded.associateWith { overrides[it] ?: BUILT_IN_TIERS[it] ?: VaultTier.STANDARD },
            autoHide = scan.autoHideSensitive(),
            onlyWhileCharging = scan.onlyWhileCharging(),
            longStandardPin = longStandardPin(dao),
            vaults = vaults(),
        )
    }

    fun setTier(category: Category, tier: VaultTier): Job = io {
        TierSettings(db().settingsDao()).set(category, tier)
        _state.value = _state.value.copy(tiers = _state.value.tiers + (category to tier))
    }

    fun setAutoHide(on: Boolean): Job = io {
        ScanSettings(db().settingsDao()).setAutoHideSensitive(on)
        _state.value = _state.value.copy(autoHide = on)
    }

    fun setOnlyWhileCharging(on: Boolean): Job = io {
        ScanSettings(db().settingsDao()).setOnlyWhileCharging(on)
        _state.value = _state.value.copy(onlyWhileCharging = on)
    }

    fun setLongStandardPin(on: Boolean): Job = io {
        db().settingsDao().put(SettingRow(LONG_STANDARD_PIN, on.toString()))
        _state.value = _state.value.copy(longStandardPin = on)
    }

    private fun io(block: suspend () -> Unit): Job = viewModelScope.launch(Dispatchers.IO) { block() }

    companion object {
        private const val LONG_STANDARD_PIN = "lock.standard_long_pin"

        /** Minimum length the UI asks for: a 6-digit PIN, or the longer passcode option. validateSecret still applies. */
        const val SHORT_PIN = 6
        const val LONG_PIN = 10

        suspend fun longStandardPin(dao: SettingsDao): Boolean = dao.get(LONG_STANDARD_PIN)?.toBoolean() ?: false
    }
}
