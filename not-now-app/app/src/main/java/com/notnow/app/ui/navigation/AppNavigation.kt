package com.notnow.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.notnow.app.ui.screen.customrules.CustomRulesScreen
import com.notnow.app.ui.screen.dashboard.WeeklyDashboardScreen
import com.notnow.app.ui.screen.home.HomeScreen
import com.notnow.app.ui.screen.messages.FutureMessagesScreen
import com.notnow.app.ui.screen.setup.SetupScreen
import com.notnow.app.ui.screen.vault.ShoppingVaultScreen
import com.notnow.app.ui.screen.websites.BlockedWebsitesScreen

sealed class Route(val path: String) {
    object Setup        : Route("setup")
    object Home         : Route("home")
    object Vault        : Route("vault")
    object Messages     : Route("messages")
    object Dashboard    : Route("dashboard")
    object CustomRules  : Route("custom_rules")
    object Websites     : Route("websites")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.Setup.path) {
            SetupScreen(onSetupComplete = {
                navController.navigate(Route.Home.path) {
                    popUpTo(Route.Setup.path) { inclusive = true }
                }
            })
        }
        composable(Route.Home.path) {
            HomeScreen(
                onOpenVault        = { navController.navigate(Route.Vault.path) },
                onOpenMessages     = { navController.navigate(Route.Messages.path) },
                onOpenDashboard    = { navController.navigate(Route.Dashboard.path) },
                onOpenCustomRules  = { navController.navigate(Route.CustomRules.path) },
                onOpenWebsites     = { navController.navigate(Route.Websites.path) }
            )
        }
        composable(Route.Vault.path) {
            ShoppingVaultScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.Messages.path) {
            FutureMessagesScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.Dashboard.path) {
            WeeklyDashboardScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.CustomRules.path) {
            CustomRulesScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.Websites.path) {
            BlockedWebsitesScreen(onBack = { navController.popBackStack() })
        }
    }
}
