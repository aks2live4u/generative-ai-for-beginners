package com.finsight

import android.app.Application
import androidx.work.Configuration
import com.finsight.backup.BackupWorker
import com.finsight.data.AppContainer

/**
 * Implements Configuration.Provider so WorkManager initializes on demand. The default
 * androidx.startup-based auto-init is disabled in AndroidManifest.xml because other AARs
 * (Room, SQLCipher, Play Services Auth) merge into the same startup provider entry and were
 * dropping the WorkManagerInitializer meta-data, crashing the app on launch.
 */
class FinSightApp : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        container = AppContainer(this)
        BackupWorker.schedule(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
