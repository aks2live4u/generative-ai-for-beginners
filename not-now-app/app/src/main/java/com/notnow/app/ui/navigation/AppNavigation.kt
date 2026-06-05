package com.notnow.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.notnow.app.NotNowApplication
import com.notnow.app.ui.screen.dashboard.WeeklyDashboardScreen
import com.notnow.app.ui.screen.home.HomeScreen
import com.notnow.app.ui.screen.messages.FutureMessagesScreen
import com.notnow.app.ui.screen.setup.SetupScreen
import com.notnow.app.ui.screen.vault.ShoppingVaultScreen

object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val VAULT = "vault"
    const val MESSAGES = "messages"
    const val DASHBOARD = "dashboard"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    app: NotNowApplication
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                app = app,
                onNavigateToVault = { navController.navigate(Routes.VAULT) },
                onNavigateToMessages = { navController.navigate(Routes.MESSAGES) },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) }
            )
        }

        composable(Routes.VAULT) {
            ShoppingVaultScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MESSAGES) {
            FutureMessagesScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DASHBOARD) {
            WeeklyDashboardScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
