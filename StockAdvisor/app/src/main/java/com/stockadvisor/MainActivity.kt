package com.stockadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.stockadvisor.ui.navigation.StockAdvisorNavGraph
import com.stockadvisor.ui.theme.StockAdvisorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockAdvisorTheme {
                StockAdvisorNavGraph()
            }
        }
    }
}
