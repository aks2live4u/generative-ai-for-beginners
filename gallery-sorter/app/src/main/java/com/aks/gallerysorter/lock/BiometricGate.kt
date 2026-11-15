package com.aks.gallerysorter.lock

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.vault.BiometricInvalidatedException
import com.aks.gallerysorter.vault.InvalidSecretException
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.VaultKeyGuard
import com.aks.gallerysorter.vault.WrongSecretException
import java.io.IOException
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Unlocks a vault tier: fingerprint first when enabled (BIOMETRIC_STRONG with a CryptoObject),
 * otherwise, or on "Use PIN" / an invalidated key, an in-app passcode dialog (5 tries).
 * Call [prompt] from the main thread. On success the handle goes into [lockState] and
 * [onUnlocked] runs (Task 14 wires it to HideManager.verifyPending + rebuildIndex).
 */
class BiometricGate(
    private val guard: VaultKeyGuard,
    private val lockState: LockState,
    private val onUnlocked: suspend (VaultTier, PrivateHandle) -> Unit,
) {
    suspend fun prompt(activity: FragmentActivity, tier: VaultTier): PrivateHandle? {
        lockState.privateHandle(tier)?.let { return it }
        val keys = guard.keys(tier)
        val handle = if (keys.isBiometricEnabled() && canUseBiometric(activity)) {
            when (val r = biometricStep(activity, tier)) {
                is Step.Done -> r.handle
                Step.Cancelled -> return null
                Step.UsePasscode -> passcodeStep(activity, tier)
            }
        } else {
            passcodeStep(activity, tier)
        } ?: return null
        lockState.unlock(tier, handle)
        onUnlocked(tier, handle)
        return handle
    }

    private sealed interface Step {
        class Done(val handle: PrivateHandle) : Step
        data object UsePasscode : Step
        data object Cancelled : Step
    }

    private sealed interface Auth {
        class Ok(val cipher: Cipher) : Auth
        data object UsePin : Auth
        data object Cancelled : Auth
    }

    private fun canUseBiometric(activity: Activity): Boolean =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    private suspend fun biometricStep(activity: FragmentActivity, tier: VaultTier): Step {
        val keys = guard.keys(tier)
        val cipher = try {
            keys.biometricCipher()
        } catch (e: BiometricInvalidatedException) {
            return Step.UsePasscode
        }
        val authed: Auth = suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val c = result.cryptoObject?.cipher
                        if (cont.isActive) cont.resume(if (c != null) Auth.Ok(c) else Auth.UsePin)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (!cont.isActive) return
                        // Negative button and hard failures (lockout, no hardware) go to the PIN.
                        cont.resume(if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) Auth.Cancelled else Auth.UsePin)
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock")
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setNegativeButtonText("Use PIN")
                .build()
            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
        }
        val ok = when (authed) {
            is Auth.Ok -> authed
            Auth.Cancelled -> return Step.Cancelled
            Auth.UsePin -> return Step.UsePasscode
        }
        return try {
            Step.Done(withContext(Dispatchers.Default) { keys.unlockWithBiometric(ok.cipher) })
        } catch (e: BiometricInvalidatedException) {
            Step.UsePasscode
        }
    }

    private suspend fun passcodeStep(activity: Activity, tier: VaultTier): PrivateHandle? {
        var error: String? = null
        repeat(MAX_TRIES) {
            val secret = askSecret(activity, tier, error) ?: return null
            // unlockWithSecret zeroes the secret.
            try {
                return withContext(Dispatchers.Default) { guard.keys(tier).unlockWithSecret(secret) }
            } catch (e: WrongSecretException) {
                error = "Wrong ${noun(tier)}"
            } catch (e: InvalidSecretException) {
                error = "Wrong ${noun(tier)}"
            } catch (e: IOException) {
                error = "Could not read the vault keys"
            }
        }
        return null
    }

    private fun noun(tier: VaultTier) = if (tier == VaultTier.STANDARD) "PIN" else "passcode"

    /** Shows the passcode dialog; the entry is copied to a CharArray and the field cleared at once. */
    private suspend fun askSecret(activity: Activity, tier: VaultTier, error: String?): CharArray? =
        suspendCancellableCoroutine { cont ->
            val field = EditText(activity).apply {
                inputType = if (tier == VaultTier.STANDARD) {
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                } else {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }
            fun submit(): CharArray {
                val text = field.text
                val chars = CharArray(text.length).also { text.getChars(0, text.length, it, 0) }
                text.clear()
                return chars
            }
            val dialog = AlertDialog.Builder(activity)
                .setTitle(if (tier == VaultTier.STANDARD) "Enter PIN" else "Enter passcode")
                .setMessage(error)
                .setView(field)
                .setCancelable(true)
                .setPositiveButton("Unlock") { _, _ -> if (cont.isActive) cont.resume(submit()) }
                .setNegativeButton("Cancel") { _, _ -> if (cont.isActive) cont.resume(null) }
                .setOnCancelListener { if (cont.isActive) cont.resume(null) }
                .create()
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            cont.invokeOnCancellation { dialog.dismiss() }
            dialog.show()
        }

    private companion object {
        const val MAX_TRIES = 5
    }
}
