package com.notnow.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.notnow.app.data.database.AppDatabase
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.data.repository.*
import com.notnow.app.worker.NightLockdownScheduler
import com.notnow.app.worker.ShoppingReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotNowApplication : Application() {

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val database by lazy { AppDatabase.getInstance(this) }

    val appPreferences by lazy { AppPreferences.getInstance(this) }
    val appRuleRepository by lazy { AppRuleRepository(database.appRuleDao()) }
    val shoppingVaultRepository by lazy { ShoppingVaultRepository(database.shoppingVaultDao()) }
    val futureMessageRepository by lazy { FutureMessageRepository(database.futureMessageDao()) }
    val usageRecordRepository by lazy { UsageRecordRepository(database.usageRecordDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        appScope.launch {
            appRuleRepository.seedDefaultRules()
            futureMessageRepository.seedDefaults()
            NightLockdownScheduler.scheduleActivation(applicationContext)
            ShoppingReminderWorker.schedulePeriodic(applicationContext)
            usageRecordRepository.pruneOldRecords()
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel("guardrail", "Guardrail Active", NotificationManager.IMPORTANCE_LOW),
            NotificationChannel("shopping_reminders", "Shopping Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        ).forEach { nm.createNotificationChannel(it) }
    }
}
