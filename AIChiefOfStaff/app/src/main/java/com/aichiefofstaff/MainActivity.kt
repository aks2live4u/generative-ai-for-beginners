package com.aichiefofstaff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichiefofstaff.data.prefs.ThemeMode
import com.aichiefofstaff.ui.nav.AppRoot
import com.aichiefofstaff.ui.theme.AiChiefOfStaffTheme
import com.aichiefofstaff.ui.util.LocalAppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as AiChiefApp).container

        setContent {
            val themeMode by container.settingsDataStore.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)

            AiChiefOfStaffTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalAppContainer provides container) {
                        AppRoot()
                    }
                }
            }
        }
    }
}
