package com.personalfinanceai

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.personalfinanceai.ui.navigation.AppNavHost
import com.personalfinanceai.ui.theme.PersonalFinanceAITheme

/**
 * Extends [FragmentActivity] (not the lighter ComponentActivity) because [androidx.biometric.BiometricPrompt]
 * requires a FragmentActivity host.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PersonalFinanceApp).container
        setContent {
            PersonalFinanceAITheme {
                AppNavHost(container = container, activity = this)
            }
        }
    }
}
