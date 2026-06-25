package com.personalfinanceai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.personalfinanceai.core.model.CategoryGroup
import com.personalfinanceai.ui.theme.CategoryAccents

fun iconForGroup(group: CategoryGroup): ImageVector = when (group) {
    CategoryGroup.INCOME -> Icons.Filled.AccountBalanceWallet
    CategoryGroup.FOOD -> Icons.Filled.Restaurant
    CategoryGroup.SHOPPING -> Icons.Filled.ShoppingBag
    CategoryGroup.TRANSPORTATION -> Icons.Filled.DirectionsCar
    CategoryGroup.UTILITIES -> Icons.Filled.Bolt
    CategoryGroup.ENTERTAINMENT -> Icons.Filled.Movie
    CategoryGroup.HEALTH -> Icons.Filled.LocalHospital
    CategoryGroup.EDUCATION -> Icons.Filled.School
    CategoryGroup.FINANCIAL -> Icons.Filled.CreditCard
    CategoryGroup.MISCELLANEOUS -> Icons.Filled.Receipt
}

fun colorForGroup(group: CategoryGroup) = when (group) {
    CategoryGroup.INCOME -> CategoryAccents.Teal
    CategoryGroup.FOOD -> CategoryAccents.Orange
    CategoryGroup.SHOPPING -> CategoryAccents.Purple
    CategoryGroup.TRANSPORTATION -> CategoryAccents.Blue
    CategoryGroup.UTILITIES -> CategoryAccents.Pink
    CategoryGroup.ENTERTAINMENT -> CategoryAccents.Red
    CategoryGroup.HEALTH -> CategoryAccents.Teal
    CategoryGroup.EDUCATION -> CategoryAccents.Blue
    CategoryGroup.FINANCIAL -> CategoryAccents.Orange
    CategoryGroup.MISCELLANEOUS -> CategoryAccents.Purple
}
