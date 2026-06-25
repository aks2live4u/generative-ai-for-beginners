package com.finsight

import android.app.Application
import com.finsight.backup.BackupWorker
import com.finsight.data.AppContainer

class FinSightApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BackupWorker.schedule(this)
    }
}
