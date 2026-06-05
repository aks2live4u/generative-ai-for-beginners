package com.notnow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.notnow.app.receiver.BootReceiver
import com.notnow.app.ui.navigation.AppNavigation
import com.notnow.app.ui.navigation.Route
import com.notnow.app.ui.theme.DeepNavy
import com.notnow.app.ui.theme.NotNowTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NotNowApplication

        // Seed default rules and schedule workers on first launch
        lifecycleScope.launch {
            if (app.preferences.isFirstLaunch.first()) {
                app.appRuleRepository.seedDefaults()
                app.preferences.setFirstLaunchDone()
                BootReceiver.scheduleWorkers(applicationContext)
            }
        }

        lifecycleScope.launch {
            val isFirst = app.preferences.isFirstLaunch.first()
            val startDest = if (isFirst) Route.Setup.path else Route.Home.path

            setContent {
                NotNowTheme {
                    val navController = rememberNavController()
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize().background(DeepNavy)
                    ) {
                        AppNavigation(
                            navController = navController,
                            startDestination = startDest
                        )
                    }
                }
            }
        }
    }
}
