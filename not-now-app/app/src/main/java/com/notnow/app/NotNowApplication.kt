package com.notnow.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.notnow.app.data.database.AppDatabase
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.data.repository.*

class NotNowApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val preferences by lazy { AppPreferences(this) }

    val appRuleRepository by lazy { AppRuleRepository(database.appRuleDao()) }
    val shoppingVaultRepository by lazy { ShoppingVaultRepository(database.shoppingVaultDao()) }
    val futureMessageRepository by lazy { FutureMessageRepository(database.futureMessageDao()) }
    val usageRepository by lazy { UsageRepository(database.usageRecordDao()) }
    val blockedWebsiteRepository by lazy { BlockedWebsiteRepository(database.blockedWebsiteDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel("guardrail", "Guardrail", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Active guardrail service indicator"
                }
            )
            manager.createNotificationChannel(
                NotificationChannel("reminders", "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Shopping vault and daily reminders"
                }
            )
        }
    }
}
