package com.nexus.launcher

import android.app.Application
import android.content.Context
import com.nexus.launcher.ai.NexusAI
import com.nexus.launcher.data.AppRepository
import com.nexus.launcher.data.SettingsStore
import com.nexus.launcher.system.FocusController
import com.nexus.launcher.system.Speaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow

class NexusApplication : Application() {
    val graph by lazy { NexusGraph(this) }
}

/** Hand-rolled dependency graph: one instance of each service for the whole launcher process. */
class NexusGraph(context: Context) {
    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = SettingsStore(appContext)
    val apps = AppRepository(appContext, scope)
    val focus = FocusController(appContext, scope)
    val ai = NexusAI(settings)
    val speaker = Speaker(appContext)

    /** Emits when the Home button is pressed while NEXUS is already in front. */
    val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
}
