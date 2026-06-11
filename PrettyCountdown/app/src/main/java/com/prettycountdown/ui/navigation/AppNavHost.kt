package com.prettycountdown.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prettycountdown.ui.collections.CollectionDetailScreen
import com.prettycountdown.ui.collections.CollectionsScreen
import com.prettycountdown.ui.createevent.CreateEventScreen
import com.prettycountdown.ui.eventdetail.EventDetailScreen
import com.prettycountdown.ui.home.HomeScreen
import com.prettycountdown.ui.notifications.NotificationsScreen
import com.prettycountdown.ui.settings.SettingsScreen
import com.prettycountdown.ui.widgetgallery.WidgetGalleryScreen

/** Hosts every screen behind the bottom navigation bar, plus the detail/edit screens above it. */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController(), startEventId: Long? = null) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
    }

    LaunchedEffect(startEventId) {
        if (startEventId != null && startEventId > 0) {
            navController.navigate(Destination.EventDetail.route(startEventId))
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onEventClick = { navController.navigate(Destination.EventDetail.route(it)) },
                    onCreateClick = { navController.navigate(Destination.CreateEvent.route()) },
                )
            }
            composable(Destination.WidgetGallery.route) { WidgetGalleryScreen() }
            composable(Destination.Notifications.route) { NotificationsScreen() }
            composable(Destination.Collections.route) {
                CollectionsScreen(onCollectionClick = { navController.navigate(Destination.CollectionDetail.route(it)) })
            }
            composable(Destination.Settings.route) { SettingsScreen() }
            composable(
                Destination.CreateEvent.route,
                arguments = listOf(navArgument(Destination.CreateEvent.ARG_EVENT_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { entry ->
                val eventId = entry.arguments?.getLong(Destination.CreateEvent.ARG_EVENT_ID) ?: -1L
                CreateEventScreen(
                    eventId = eventId,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Destination.EventDetail.route,
                arguments = listOf(navArgument(Destination.EventDetail.ARG_EVENT_ID) { type = NavType.LongType })
            ) { entry ->
                val eventId = entry.arguments?.getLong(Destination.EventDetail.ARG_EVENT_ID) ?: 0L
                EventDetailScreen(
                    eventId = eventId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Destination.CreateEvent.route(it)) },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                Destination.CollectionDetail.route,
                arguments = listOf(navArgument(Destination.CollectionDetail.ARG_COLLECTION_ID) { type = NavType.LongType })
            ) { entry ->
                val collectionId = entry.arguments?.getLong(Destination.CollectionDetail.ARG_COLLECTION_ID) ?: 0L
                CollectionDetailScreen(
                    collectionId = collectionId,
                    onEventClick = { navController.navigate(Destination.EventDetail.route(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
