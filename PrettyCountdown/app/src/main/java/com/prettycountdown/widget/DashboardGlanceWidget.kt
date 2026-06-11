package com.prettycountdown.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prettycountdown.MainActivity
import com.prettycountdown.data.EventRepository
import com.prettycountdown.data.model.ColorPalettes
import com.prettycountdown.data.model.Event
import com.prettycountdown.util.CountdownMath

/** The home dashboard widget: a scrollable list of every upcoming countdown. */
class DashboardGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(WidgetSizes.MEDIUM, WidgetSizes.LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EventRepository.getInstance(context)
        val now = System.currentTimeMillis()
        val events = repository.getAllEventsOnce()
            .filter { it.targetDateTime >= now }
            .sortedBy { it.targetDateTime }
            .take(10)

        provideContent {
            GlanceTheme {
                DashboardContent(events)
            }
        }
    }
}

@Composable
private fun DashboardContent(events: List<Event>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(12.dp),
    ) {
        Text(
            "Upcoming",
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (events.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No upcoming events. Tap to add one!",
                    style = TextStyle(color = GlanceTheme.colors.onBackground, textAlign = TextAlign.Center),
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(events, itemId = { it.id }) { event ->
                    DashboardRow(event)
                    Spacer(GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardRow(event: Event) {
    val palette = ColorPalettes.byKey(event.colorPaletteKey)
    val display = computeDisplay(event, event.countdownFormat)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(Color(palette.background)))
            .cornerRadius(12.dp)
            .padding(10.dp)
            .clickable(openEventAction(event.id)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(event.category.emoji, style = TextStyle(fontSize = 20.sp))
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                event.name,
                style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Text(
                CountdownMath.formatDate(event.targetDateTime, event.hasTime),
                style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(display.big, style = TextStyle(color = ColorProvider(Color(palette.primary)), fontSize = 18.sp, fontWeight = FontWeight.Bold))
            Text(display.unit, style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 10.sp), maxLines = 1)
        }
    }
}
