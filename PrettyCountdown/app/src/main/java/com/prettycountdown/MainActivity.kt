package com.prettycountdown

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prettycountdown.data.SettingsRepository
import com.prettycountdown.data.ThemeMode
import com.prettycountdown.ui.navigation.AppNavHost
import com.prettycountdown.ui.theme.PrettyCountdownTheme
import com.prettycountdown.widget.WidgetActionParams

/** Single-activity host: theme, notification permission, daily-open streak and widget deep links. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startEventId = intent?.extras?.getLong(WidgetActionParams.EVENT_ID.name, -1L)?.takeIf { it > 0 }

        setContent {
            val settings = remember { SettingsRepository.getInstance(applicationContext) }
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

            LaunchedEffect(Unit) {
                settings.recordAppOpen()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            PrettyCountdownTheme(themeMode = themeMode) {
                AppNavHost(startEventId = startEventId)
            }
        }
    }
}
