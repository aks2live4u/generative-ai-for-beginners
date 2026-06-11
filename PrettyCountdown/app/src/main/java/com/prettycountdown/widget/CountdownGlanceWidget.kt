package com.prettycountdown.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.material3.GlanceTheme
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.prettycountdown.data.EventRepository
import com.prettycountdown.data.SettingsRepository
import com.prettycountdown.data.model.CountdownFormat
import com.prettycountdown.data.model.WidgetStyle
import kotlinx.coroutines.flow.first

/** The single-event countdown widget - the heart of Pretty Countdown. */
class CountdownGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(WidgetSizes.responsive)
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EventRepository.getInstance(context)
        val settings = SettingsRepository.getInstance(context)

        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val configuredEventId = prefs[WidgetPrefKeys.EVENT_ID]

        val event = configuredEventId?.let { repository.getEvent(it) }
            ?: settings.lastViewedEventId.first()?.let { repository.getEvent(it) }
            ?: repository.getAllEventsOnce().minByOrNull { it.targetDateTime }

        if (configuredEventId == null && event != null) {
            updateAppWidgetState(context, id) { it[WidgetPrefKeys.EVENT_ID] = event.id }
        }

        val style = prefs[WidgetPrefKeys.STYLE_OVERRIDE]
            ?.let { runCatching { WidgetStyle.valueOf(it) }.getOrNull() }
            ?: event?.widgetStyle ?: WidgetStyle.default

        val format = prefs[WidgetPrefKeys.FORMAT_OVERRIDE]
            ?.let { runCatching { CountdownFormat.valueOf(it) }.getOrNull() }
            ?: event?.countdownFormat ?: CountdownFormat.default

        val photoBitmap = if (style == WidgetStyle.PHOTO && event?.photoUri != null) {
            loadBitmap(context, event.photoUri)
        } else {
            null
        }

        provideContent {
            GlanceTheme {
                if (event == null) {
                    EmptyStateWidget()
                } else {
                    val sizeBucket = bucketFor(LocalSize.current)
                    WidgetContent(
                        event = event,
                        style = style,
                        format = format,
                        photoBitmap = photoBitmap,
                        sizeBucket = sizeBucket,
                    )
                }
            }
        }
    }

    private fun loadBitmap(context: Context, uriString: String): Bitmap? = runCatching {
        context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}
