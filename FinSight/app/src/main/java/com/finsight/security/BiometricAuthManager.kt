package com.finsight.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

/**
 * Wraps [BiometricPrompt] for fingerprint/face unlock. Deliberately does NOT use
 * [BiometricManager.Authenticators.DEVICE_CREDENTIAL] as a fallback - PIN fallback is handled by
 * the app's own [PinManager] screen instead, per the product's security flow (Fingerprint / Face
 * / PIN backup, in that order).
 */
class BiometricAuthManager(private val activity: FragmentActivity) {

    fun canAuthenticateWithBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(onSuccess: () -> Unit, onFailedOrError: (String) -> Unit) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailedOrError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onFailedOrError("Fingerprint/Face not recognized")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FinSight")
            .setSubtitle("Use your fingerprint or face to continue")
            .setNegativeButtonText("Use PIN instead")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo)
    }
}
