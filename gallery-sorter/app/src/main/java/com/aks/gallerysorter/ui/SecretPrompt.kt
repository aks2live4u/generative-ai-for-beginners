package com.aks.gallerysorter.ui

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.validateSecret
import javax.crypto.Cipher
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** "PIN" for the standard tier, "passcode" for high-security. */
fun VaultTier.secretNoun(): String = if (this == VaultTier.STANDARD) "PIN" else "passcode"

/** The rule shown to the user; [minLength] may be longer than the tier's minimum (the longer standard passcode). */
fun VaultTier.secretRule(minLength: Int): String =
    if (this == VaultTier.STANDARD) "$minLength or more digits" else "$minLength or more characters, letters allowed"

/**
 * Asks for a new PIN/passcode twice in a FLAG_SECURE dialog. The entry is copied to a CharArray
 * and the fields are cleared; it is never turned into a String. Returns null if cancelled.
 * Accepted only if both entries match, have [minLength] characters and pass [validateSecret].
 */
suspend fun askNewSecret(activity: Activity, tier: VaultTier, minLength: Int, title: String): CharArray? =
    askSecretDialog(activity, tier, title, "Use ${tier.secretRule(minLength)}.", confirm = true) { first, second ->
        when {
            second != null && !first.contentEquals(second) -> "The two entries don't match"
            first.size < minLength || !validateSecret(tier, first) -> "Use ${tier.secretRule(minLength)}"
            else -> null
        }
    }

/** Asks for an existing PIN/passcode once (e.g. a vault left from an earlier install). */
suspend fun askExistingSecret(activity: Activity, tier: VaultTier, title: String, message: String): CharArray? =
    askSecretDialog(activity, tier, title, message, confirm = false) { _, _ -> null }

/** [check] returns an error to show (the dialog stays open) or null to accept. The second entry is zeroed here. */
@OptIn(ExperimentalCoroutinesApi::class) // resume(value, onCancellation) zeroes an undelivered secret
private suspend fun askSecretDialog(
    activity: Activity,
    tier: VaultTier,
    title: String,
    message: String,
    confirm: Boolean,
    check: (CharArray, CharArray?) -> String?,
): CharArray? = suspendCancellableCoroutine { cont ->
    fun field(hint: String) = EditText(activity).apply {
        this.hint = hint
        inputType = if (tier == VaultTier.STANDARD) {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }
    val first = field(tier.secretNoun().replaceFirstChar { it.uppercase() })
    val second = if (confirm) field("Repeat") else null
    val layout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        val pad = (20 * activity.resources.displayMetrics.density).toInt()
        setPadding(pad, 0, pad, 0)
        addView(first)
        second?.let(::addView)
    }
    fun chars(f: EditText) = f.text.let { t -> CharArray(t.length).also { t.getChars(0, t.length, it, 0) } }
    fun clear() {
        first.text.clear()
        second?.text?.clear()
    }
    val dialog = AlertDialog.Builder(activity)
        .setTitle(title)
        .setMessage(message)
        .setView(layout)
        .setCancelable(true)
        .setPositiveButton("OK", null)
        .setNegativeButton("Cancel") { _, _ -> if (cont.isActive) cont.resume(null) }
        .setOnDismissListener { clear(); if (cont.isActive) cont.resume(null) }
        .create()
    dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val a = chars(first)
            val b = second?.let(::chars)
            val error = check(a, b)
            b?.fill('\u0000')
            if (error != null) {
                a.fill('\u0000')
                clear()
                dialog.setMessage(error)
                return@setOnClickListener
            }
            if (cont.isActive) cont.resume(a) { a.fill('\u0000') } else a.fill('\u0000')
            dialog.dismiss()
        }
    }
    cont.invokeOnCancellation { dialog.dismiss() }
    dialog.show()
}

/** True if this phone can do Class 3 (strong) fingerprint authentication now. */
fun canUseFingerprint(activity: Activity): Boolean =
    BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * Turns on fingerprint unlock for [tier]: authenticates the Keystore encrypt cipher with
 * BiometricPrompt, then wraps the private key through [com.aks.gallerysorter.vault.VaultKeyGuard.enableBiometric]
 * (which re-pins the key file). Returns true on success; failures give a notice without details.
 */
suspend fun enrollFingerprint(activity: FragmentActivity, graph: AppGraph, tier: VaultTier, handle: PrivateHandle): Boolean {
    if (!canUseFingerprint(activity)) {
        graph.notify("Add a fingerprint in the phone's settings first")
        return false
    }
    return try {
        val cipher = withContext(Dispatchers.IO) { graph.guard.keys(tier).biometricEncryptCipher() }
        val authed = authenticate(activity, cipher) ?: return false
        withContext(Dispatchers.IO) { graph.guard.enableBiometric(tier, handle, authed) }
        graph.notify("Fingerprint unlock is on")
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        android.util.Log.w("SecretPrompt", "fingerprint enrolment failed: ${e.javaClass.name}")
        graph.notify("Couldn't turn on fingerprint unlock")
        false
    }
}

private suspend fun authenticate(activity: FragmentActivity, cipher: Cipher): Cipher? = suspendCancellableCoroutine { cont ->
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(result.cryptoObject?.cipher)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (cont.isActive) cont.resume(null)
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Turn on fingerprint unlock")
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .setNegativeButtonText("Cancel")
        .build()
    prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    cont.invokeOnCancellation { prompt.cancelAuthentication() }
}
