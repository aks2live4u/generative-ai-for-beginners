package com.prettycountdown.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Call after any change that could affect what a home-screen widget shows. */
object WidgetUpdater {
    suspend fun refreshAll(context: Context) {
        CountdownGlanceWidget().updateAll(context)
        DashboardGlanceWidget().updateAll(context)
    }
}
