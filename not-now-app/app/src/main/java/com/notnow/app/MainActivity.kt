package com.notnow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.notnow.app.ui.navigation.AppNavHost
import com.notnow.app.ui.navigation.Routes
import com.notnow.app.ui.theme.NotNowTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NotNowApplication

        setContent {
            NotNowTheme {
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val setupComplete = app.appPreferences.setupComplete.first()
                    startDestination = if (setupComplete) Routes.HOME else Routes.SETUP
                }

                startDestination?.let { start ->
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        startDestination = start,
                        app = app
                    )
                }
            }
        }
    }
}
