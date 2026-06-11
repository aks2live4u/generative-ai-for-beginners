package com.prettycountdown.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigable destinations in the app. */
sealed class Destination(val route: String) {
    object Home : Destination("home")
    object WidgetGallery : Destination("widgets")
    object Notifications : Destination("notifications")
    object Collections : Destination("collections")
    object Settings : Destination("settings")

    object CreateEvent : Destination("create_event?eventId={eventId}") {
        const val ARG_EVENT_ID = "eventId"
        fun route(eventId: Long? = null) = "create_event?eventId=${eventId ?: -1L}"
    }

    object EventDetail : Destination("event_detail/{eventId}") {
        const val ARG_EVENT_ID = "eventId"
        fun route(eventId: Long) = "event_detail/$eventId"
    }

    object CollectionDetail : Destination("collection_detail/{collectionId}") {
        const val ARG_COLLECTION_ID = "collectionId"
        fun route(collectionId: Long) = "collection_detail/$collectionId"
    }
}

data class BottomNavItem(val destination: Destination, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Destination.Home, "Home", Icons.Filled.Home),
    BottomNavItem(Destination.WidgetGallery, "Widgets", Icons.Filled.Widgets),
    BottomNavItem(Destination.Notifications, "Alerts", Icons.Filled.Notifications),
    BottomNavItem(Destination.Collections, "Collections", Icons.Filled.CalendarMonth),
    BottomNavItem(Destination.Settings, "Settings", Icons.Filled.Settings),
)
