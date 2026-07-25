package com.accounting.engine.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.accounting.engine.ui.AccountingViewModelFactory
import com.accounting.engine.ui.balancesheet.BalanceSheetScreen
import com.accounting.engine.ui.pnl.ProfitAndLossScreen
import com.accounting.engine.ui.quickinput.QuickInputScreen

private sealed class Destination(val route: String, val label: String) {
    object QuickInput : Destination("quick_input", "Quick Input")
    object ProfitAndLoss : Destination("profit_and_loss", "P&L")
    object BalanceSheet : Destination("balance_sheet", "Balance Sheet")
}

private val bottomNavDestinations = listOf(Destination.QuickInput, Destination.ProfitAndLoss, Destination.BalanceSheet)

@Composable
fun AccountingNavGraph(viewModelFactory: AccountingViewModelFactory) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination

            NavigationBar {
                bottomNavDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconFor(destination), contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.QuickInput.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Destination.QuickInput.route) { QuickInputScreen(viewModelFactory) }
            composable(Destination.ProfitAndLoss.route) { ProfitAndLossScreen(viewModelFactory) }
            composable(Destination.BalanceSheet.route) { BalanceSheetScreen(viewModelFactory) }
        }
    }
}

private fun iconFor(destination: Destination) = when (destination) {
    Destination.QuickInput -> Icons.Filled.Add
    Destination.ProfitAndLoss -> Icons.Filled.ShowChart
    Destination.BalanceSheet -> Icons.Filled.AccountBalance
}
