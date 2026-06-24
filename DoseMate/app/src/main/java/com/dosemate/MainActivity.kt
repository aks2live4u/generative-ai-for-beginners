package com.dosemate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dosemate.data.ThemePrefs
import com.dosemate.scheduling.AlarmScheduler
import com.dosemate.scheduling.NotificationDeepLink
import com.dosemate.scheduling.NotificationHelper
import com.dosemate.ui.navigation.DoseMateNavHost
import com.dosemate.ui.theme.DoseMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePrefs.init(this)
        consumeNotificationIntent(intent)
        setContent {
            DoseMateTheme {
                DoseMateNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeNotificationIntent(intent)
    }

    private fun consumeNotificationIntent(intent: Intent) {
        val logId = intent.getLongExtra(AlarmScheduler.EXTRA_LOG_ID, -1L)
        if (logId == -1L) return
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME)
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE)
        NotificationDeepLink.set(logId, medicineName, dosage)
        intent.removeExtra(AlarmScheduler.EXTRA_LOG_ID)
    }
}
