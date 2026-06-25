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

    // BiometricPrompt throws IllegalStateException if authenticate() is called while a session
    // is already in flight - e.g. the lock screen's auto-trigger on entry racing a user tap on
    // the fingerprint icon. That exception was previously uncaught, which froze the lock screen
    // with no visible error and no way forward except killing the app.
    @Volatile
    private var authenticating = false

    fun canAuthenticateWithBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(onSuccess: () -> Unit, onFailedOrError: (String) -> Unit) {
        if (authenticating) return
        authenticating = true

        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authenticating = false
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authenticating = false
                onFailedOrError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                // Wrong fingerprint/face - the system keeps the same session open for a retry, so
                // don't clear `authenticating` here or a stray tap on the icon would try to start
                // a second concurrent session and hit the same IllegalStateException as above.
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

        try {
            prompt.authenticate(promptInfo)
        } catch (e: IllegalStateException) {
            authenticating = false
            onFailedOrError("Couldn't start biometric authentication - please use your PIN")
        }
    }
}
