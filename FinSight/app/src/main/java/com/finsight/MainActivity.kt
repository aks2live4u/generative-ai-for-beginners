package com.finsight

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.finsight.ui.navigation.AppNavHost
import com.finsight.ui.theme.FinSightTheme

/**
 * Extends [FragmentActivity] (not the lighter ComponentActivity) because [androidx.biometric.BiometricPrompt]
 * requires a FragmentActivity host.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FinSightApp).container
        setContent {
            FinSightTheme {
                AppNavHost(container = container, activity = this)
            }
        }
    }
}
