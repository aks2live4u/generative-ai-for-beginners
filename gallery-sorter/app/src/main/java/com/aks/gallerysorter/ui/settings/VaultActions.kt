package com.aks.gallerysorter.ui.settings

import android.content.Context
import android.util.Log
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.askNewSecret
import com.aks.gallerysorter.ui.enrollFingerprint
import com.aks.gallerysorter.ui.fragmentActivity
import com.aks.gallerysorter.ui.secretNoun
import com.aks.gallerysorter.vault.PrivateHandle
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The tier's handle from the lock session, else an unlock through the BiometricGate. Main thread. */
private suspend fun handleFor(context: Context, tier: VaultTier): PrivateHandle? {
    val graph = context.graph
    graph.lockState.privateHandle(tier)?.let { return it }
    val activity = context.fragmentActivity() ?: return null
    val gate = withContext(Dispatchers.IO) { graph.biometricGate }
    return gate.prompt(activity, tier)
}

/** Change PIN/passcode, only through VaultKeyGuard.changeSecret (validates and re-pins). True if changed. Main thread. */
suspend fun changeSecret(context: Context, tier: VaultTier, minLength: Int): Boolean {
    val graph = context.graph
    val activity = context.fragmentActivity() ?: return false
    val handle = handleFor(context, tier) ?: return false
    val noun = tier.secretNoun()
    val secret = askNewSecret(activity, tier, minLength, "New $noun") ?: return false
    return try {
        withContext(Dispatchers.Default) { graph.guard.changeSecret(tier, handle, secret) } // zeroes secret
        graph.notify("${noun.replaceFirstChar { it.uppercase() }} changed")
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "change secret failed: ${e.javaClass.name}")
        graph.notify("Couldn't change the $noun")
        false
    } finally {
        secret.fill('\u0000')
    }
}

/** Fingerprint unlock on (enrolled through the guard) or off. Main thread. */
suspend fun setFingerprint(context: Context, tier: VaultTier, on: Boolean) {
    val graph = context.graph
    if (on) {
        val activity = context.fragmentActivity() ?: return
        val handle = handleFor(context, tier) ?: return
        enrollFingerprint(activity, graph, tier, handle)
        return
    }
    try {
        withContext(Dispatchers.IO) { graph.guard.disableBiometric(tier) }
        graph.notify("Fingerprint unlock is off; use your ${tier.secretNoun()}")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "disable fingerprint failed: ${e.javaClass.name}")
        graph.notify("Couldn't turn off fingerprint unlock")
    }
}

private const val TAG = "VaultActions"
