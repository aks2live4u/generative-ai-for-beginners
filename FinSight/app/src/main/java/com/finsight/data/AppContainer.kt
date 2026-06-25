package com.finsight.data

import android.content.Context
import com.finsight.backup.BackupManager
import com.finsight.data.db.AppDatabase
import com.finsight.data.repository.GoalRepository
import com.finsight.data.repository.RoomFinanceDataProvider
import com.finsight.data.repository.SubscriptionRepository
import com.finsight.data.repository.TransactionRepository
import com.finsight.gmail.GmailAuthManager
import com.finsight.gmail.GmailScanner
import com.finsight.llm.GeminiClient
import com.finsight.llm.GeminiSettingsManager
import com.finsight.security.OnboardingPrefs
import com.finsight.security.PinManager
import com.finsight.sms.SmsHistoryScanner

/**
 * Hand-rolled dependency container (no DI framework needed for this app's size). Holds one
 * instance of the database/repositories for the lifetime of the process.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    val transactionRepository = TransactionRepository(database.transactionDao(), database.merchantDao())
    val subscriptionRepository = SubscriptionRepository(database.subscriptionDao())
    val goalRepository = GoalRepository(database.goalDao())
    val financeDataProvider = RoomFinanceDataProvider(transactionRepository, subscriptionRepository)

    val pinManager = PinManager(context)
    val onboardingPrefs = OnboardingPrefs(context)
    val backupManager = BackupManager(context)
    val gmailAuthManager = GmailAuthManager(context)
    val gmailScanner = GmailScanner(transactionRepository)
    val smsHistoryScanner = SmsHistoryScanner(context, transactionRepository)

    val geminiSettingsManager = GeminiSettingsManager(context)
    val geminiClient = GeminiClient()
}
