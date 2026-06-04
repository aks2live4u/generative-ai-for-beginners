package com.stockadvisor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stockadvisor.ui.screens.DecisionScreen
import com.stockadvisor.ui.screens.ResultScreen
import com.stockadvisor.ui.screens.SearchScreen
import com.stockadvisor.viewmodel.StockViewModel

object Routes {
    const val SEARCH = "search"
    const val DECISION = "decision/{symbol}"
    const val RESULT = "result/{symbol}/{decision}"

    fun decisionRoute(symbol: String) = "decision/$symbol"
    fun resultRoute(symbol: String, decision: String) = "result/$symbol/$decision"
}

@Composable
fun StockAdvisorNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: StockViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = Routes.SEARCH) {
        composable(Routes.SEARCH) {
            SearchScreen(
                onContinue = { symbol ->
                    navController.navigate(Routes.decisionRoute(symbol))
                }
            )
        }
        composable(Routes.DECISION) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
            DecisionScreen(
                symbol = symbol,
                onDecision = { decision ->
                    viewModel.analyzeStock(symbol, decision)
                    navController.navigate(Routes.resultRoute(symbol, decision)) {
                        popUpTo(Routes.SEARCH)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.RESULT) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
            val decision = backStackEntry.arguments?.getString("decision") ?: ""
            ResultScreen(
                symbol = symbol,
                decision = decision,
                viewModel = viewModel,
                onAnalyzeAnother = {
                    viewModel.reset()
                    navController.navigate(Routes.SEARCH) {
                        popUpTo(Routes.SEARCH) { inclusive = true }
                    }
                }
            )
        }
    }
}
