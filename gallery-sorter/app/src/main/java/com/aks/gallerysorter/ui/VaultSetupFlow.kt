package com.aks.gallerysorter.ui

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.vault.OrphanedVaultException
import com.aks.gallerysorter.vault.VaultKeyMismatchException
import com.aks.gallerysorter.vault.WrongSecretException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** NEW: no keys. EXISTING: keys from an earlier install need their original secret. READY: set up. */
enum class VaultSetup { NEW, EXISTING, READY }

sealed interface SetupResult {
    /** [fromEarlierInstall]: existing keys were re-pinned; their hidden items can be recovered by unlocking. */
    data class Done(val fromEarlierInstall: Boolean) : SetupResult
    data class Failed(val message: String) : SetupResult
    data object Cancelled : SetupResult
}

suspend fun vaultSetup(graph: AppGraph, tier: VaultTier): VaultSetup = withContext(Dispatchers.IO) {
    when {
        graph.guard.isSetUp(tier) -> VaultSetup.READY
        graph.guard.hasUnpinnedKeys(tier) -> VaultSetup.EXISTING
        else -> VaultSetup.NEW
    }
}

/**
 * Sets up [tier] through VaultKeyGuard.createVault only (it validates the secret): a new secret
 * asked twice, or, when keys from an earlier install exist, "Enter your existing PIN" (createVault
 * then re-pins those keys; OrphanedVaultException / a wrong secret ask again). With [fingerprint],
 * fingerprint unlock is enrolled right after with a one-off handle that is not kept. Main thread.
 */
suspend fun setUpVault(activity: FragmentActivity, graph: AppGraph, tier: VaultTier, minLength: Int, fingerprint: Boolean): SetupResult {
    val setup = vaultSetup(graph, tier)
    if (setup == VaultSetup.READY) return SetupResult.Done(fromEarlierInstall = false)
    val noun = tier.secretNoun()
    val secret = if (setup == VaultSetup.EXISTING) {
        askExistingSecret(activity, tier, "Enter your existing $noun", "This phone already has this vault from an earlier install. Enter its $noun to use it again.")
    } else {
        askNewSecret(activity, tier, minLength, "Choose a $noun")
    } ?: return SetupResult.Cancelled
    val copy = secret.copyOf()
    try {
        withContext(Dispatchers.IO) { graph.guard.createVault(tier, secret) } // zeroes secret
        if (fingerprint && canUseFingerprint(activity)) {
            val handle = withContext(Dispatchers.Default) { graph.guard.keys(tier).unlockWithSecret(copy.copyOf()) }
            enrollFingerprint(activity, graph, tier, handle)
        }
        return SetupResult.Done(fromEarlierInstall = setup == VaultSetup.EXISTING)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("VaultSetup", "vault setup failed: ${e.javaClass.name}")
        return SetupResult.Failed(
            when (e) {
                is WrongSecretException, is OrphanedVaultException -> "That isn't this vault's $noun. Enter the $noun you used before."
                is VaultKeyMismatchException -> "This vault's key files don't match each other, so it can't be used."
                else -> "Couldn't set up the vault. Try again."
            },
        )
    } finally {
        copy.fill('\u0000')
        secret.fill('\u0000')
    }
}
