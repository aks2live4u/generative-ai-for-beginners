package com.personalfinanceai

import android.app.Application
import com.personalfinanceai.backup.BackupWorker
import com.personalfinanceai.data.AppContainer

class PersonalFinanceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BackupWorker.schedule(this)
    }
}
