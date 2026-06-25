package com.personalfinanceai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Home", Icons.Filled.Home),
    TRANSACTIONS("transactions", "Transactions", Icons.Filled.Receipt),
    INSIGHTS("insights", "Insights", Icons.Filled.Insights),
    CHAT("chat", "Assistant", Icons.Filled.AutoAwesome)
}

@Composable
fun FinanceBottomNavBar(currentTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    NavigationBar {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
