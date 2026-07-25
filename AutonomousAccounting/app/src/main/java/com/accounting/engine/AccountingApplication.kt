package com.accounting.engine

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.accounting.engine.data.AppDatabase
import com.accounting.engine.data.UserPreferences
import com.accounting.engine.domain.AccountingEngine
import com.accounting.engine.repository.AccountingRepository
import com.accounting.engine.worker.ContingencyReserveWorker
import java.util.concurrent.TimeUnit

class AccountingApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: AccountingRepository
        private set
    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        userPreferences = UserPreferences(this)
        repository = AccountingRepository(database, AccountingEngine(database))
        scheduleContingencyReserveAutomation()
    }

    /** Phase 4: periodic monthly auto-allocation into the Contingency Reserve. */
    private fun scheduleContingencyReserveAutomation() {
        val request = PeriodicWorkRequestBuilder<ContingencyReserveWorker>(30, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ContingencyReserveWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
