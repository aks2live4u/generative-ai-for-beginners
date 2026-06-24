package com.dosemate.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dosemate.ui.screens.AddMedicineScreen
import com.dosemate.ui.screens.AnalyticsScreen
import com.dosemate.ui.screens.DashboardScreen
import com.dosemate.ui.screens.HistoryScreen
import com.dosemate.ui.screens.PermissionsScreen
import com.dosemate.ui.screens.WelcomeScreen
import com.dosemate.ui.theme.LocalDoseMateColors

private object Routes {
    const val WELCOME = "welcome"
    const val PERMISSIONS = "permissions"
    const val DASHBOARD = "dashboard"
    const val ADD_MEDICINE = "add_medicine"
    const val HISTORY = "history"
    const val ANALYTICS = "analytics"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.DASHBOARD, "Home", Icons.Filled.Home),
    BottomTab(Routes.ADD_MEDICINE, "Add", Icons.Filled.Add),
    BottomTab(Routes.HISTORY, "History", Icons.Filled.History),
    BottomTab(Routes.ANALYTICS, "Analytics", Icons.Filled.BarChart)
)

@Composable
fun DoseMateNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }
    val colors = LocalDoseMateColors.current

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.background(colors.glassSoft),
                    containerColor = colors.glassStrong
                ) {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.DASHBOARD)
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.WELCOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.WELCOME) {
                WelcomeScreen(onGetStarted = { navController.navigate(Routes.PERMISSIONS) })
            }
            composable(Routes.PERMISSIONS) {
                PermissionsScreen(onContinue = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                })
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onAddMedicine = { navController.navigate(Routes.ADD_MEDICINE) },
                    onViewHistory = { navController.navigate(Routes.HISTORY) },
                    onViewAnalytics = { navController.navigate(Routes.ANALYTICS) }
                )
            }
            composable(Routes.ADD_MEDICINE) {
                AddMedicineScreen(onSaved = { navController.popBackStack(Routes.DASHBOARD, inclusive = false) })
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.ANALYTICS) { AnalyticsScreen() }
        }
    }
}
