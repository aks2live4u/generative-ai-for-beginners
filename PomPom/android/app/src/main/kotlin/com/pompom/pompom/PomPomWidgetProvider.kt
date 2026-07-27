package com.pompom.pompom

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetBackgroundIntent
import es.antonborri.home_widget.HomeWidgetLaunchIntent
import es.antonborri.home_widget.HomeWidgetProvider

/// Home screen widget showing the live Pomodoro countdown with a single
/// context-sensitive action button (Start when idle, Pause/Resume while a
/// session is running).
class PomPomWidgetProvider : HomeWidgetProvider() {

  override fun onUpdate(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetIds: IntArray,
      widgetData: SharedPreferences,
  ) {
    appWidgetIds.forEach { widgetId ->
      val timeText = widgetData.getString("time_text", "25:00")
      val phaseLabel = widgetData.getString("phase_label", "PomPom")
      val progress = widgetData.getInt("progress_percent", 0)
      val isRunning = widgetData.getBoolean("is_running", false)
      val isActive = widgetData.getBoolean("is_active", false)

      val views =
          RemoteViews(context.packageName, R.layout.pompom_widget_layout).apply {
            setTextViewText(R.id.widget_phase_label, phaseLabel)
            setTextViewText(R.id.widget_time_text, timeText)
            setProgressBar(R.id.widget_progress, 100, progress, false)

            val openAppIntent =
                HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java,
                    Uri.parse("pompom://open"),
                )
            setOnClickPendingIntent(R.id.widget_container, openAppIntent)

            when {
              !isActive -> {
                setTextViewText(R.id.widget_action_button, "Start")
                setOnClickPendingIntent(
                    R.id.widget_action_button,
                    HomeWidgetLaunchIntent.getActivity(
                        context,
                        MainActivity::class.java,
                        Uri.parse("pompom://start"),
                    ),
                )
              }
              isRunning -> {
                setTextViewText(R.id.widget_action_button, "Pause")
                setOnClickPendingIntent(
                    R.id.widget_action_button,
                    HomeWidgetBackgroundIntent.getBroadcast(context, Uri.parse("pompom://pause")),
                )
              }
              else -> {
                setTextViewText(R.id.widget_action_button, "Resume")
                setOnClickPendingIntent(
                    R.id.widget_action_button,
                    HomeWidgetBackgroundIntent.getBroadcast(context, Uri.parse("pompom://resume")),
                )
              }
            }
          }

      appWidgetManager.updateAppWidget(widgetId, views)
    }
  }
}
