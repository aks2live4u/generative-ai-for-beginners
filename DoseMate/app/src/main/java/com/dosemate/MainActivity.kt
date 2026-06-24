package com.dosemate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dosemate.ui.navigation.DoseMateNavHost
import com.dosemate.ui.theme.DoseMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoseMateTheme {
                DoseMateNavHost()
            }
        }
    }
}
