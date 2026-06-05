package com.notnow.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notnow.app.worker.NightLockdownScheduler
import com.notnow.app.worker.ShoppingReminderWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            NightLockdownScheduler.scheduleActivation(context)
            ShoppingReminderWorker.schedulePeriodic(context)
        }
    }
}
