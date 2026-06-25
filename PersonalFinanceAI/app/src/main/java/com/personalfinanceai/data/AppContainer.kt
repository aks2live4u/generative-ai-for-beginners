package com.personalfinanceai.data

import android.content.Context
import com.personalfinanceai.backup.BackupManager
import com.personalfinanceai.data.db.AppDatabase
import com.personalfinanceai.data.repository.GoalRepository
import com.personalfinanceai.data.repository.RoomFinanceDataProvider
import com.personalfinanceai.data.repository.SubscriptionRepository
import com.personalfinanceai.data.repository.TransactionRepository
import com.personalfinanceai.gmail.GmailAuthManager
import com.personalfinanceai.gmail.GmailScanner
import com.personalfinanceai.security.OnboardingPrefs
import com.personalfinanceai.security.PinManager
import com.personalfinanceai.sms.SmsHistoryScanner

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
}
