package com.accounting.engine.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

/**
 * Thin wrapper around [BiometricPrompt] that gates app content behind the device's configured
 * biometric/device-credential lock. If the device has no biometric or PIN/pattern enrolled,
 * access is allowed through - we don't force security hardware the user doesn't have.
 */
object BiometricGate {

    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        if (!isAvailable(activity)) {
            onSuccess()
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure()
                }

                override fun onAuthenticationFailed() {
                    // Let the user retry; BiometricPrompt keeps its own UI open.
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Autonomous Accounting")
            .setSubtitle("Your ledger is encrypted at rest")
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        prompt.authenticate(info)
    }
}
