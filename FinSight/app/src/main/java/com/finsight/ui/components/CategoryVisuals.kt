package com.finsight.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finsight.core.model.CategoryGroup
import com.finsight.ui.theme.CategoryAccents

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

/** A recognizable brand color for a known merchant, used for the initial-letter badge on transaction rows. */
private val knownMerchantColors: Map<String, Color> = mapOf(
    "zomato" to Color(0xFFE23744),
    "swiggy" to Color(0xFFFC8019),
    "uber" to Color(0xFF000000),
    "ola" to Color(0xFF2F2F2F),
    "blinkit" to Color(0xFFFFC700),
    "zepto" to Color(0xFF8A2BE2),
    "amazon" to Color(0xFFFF9900),
    "flipkart" to Color(0xFF2874F0),
    "netflix" to Color(0xFFE50914),
    "spotify" to Color(0xFF1DB954),
    "salary" to Color(0xFF1FAA59),
    "irctc" to Color(0xFF1B5E20),
    "myntra" to Color(0xFFFF3F6C),
    "bigbasket" to Color(0xFF84C225)
)

/** Picks the brand color for a known merchant by substring match, else falls back to the category color. */
fun merchantBadgeColor(merchant: String, fallback: Color): Color {
    val lower = merchant.lowercase()
    return knownMerchantColors.entries.firstOrNull { lower.contains(it.key) }?.value ?: fallback
}

/** True when [merchant] matches a recognized brand, so the UI can show its initial instead of a generic category icon. */
fun isKnownMerchant(merchant: String): Boolean {
    val lower = merchant.lowercase()
    return knownMerchantColors.keys.any { lower.contains(it) }
}

fun merchantInitial(merchant: String): String =
    merchant.trim().firstOrNull()?.uppercase() ?: "?"

/**
 * Circular avatar for a transaction/subscription row: a brand-colored initial letter for
 * recognized merchants (Zomato, Uber, Swiggy, Salary, ...), falling back to the generic
 * category icon for everything else (bills, transfers, unrecognized merchants).
 */
@Composable
fun MerchantBadge(merchant: String, group: CategoryGroup, modifier: Modifier = Modifier) {
    val fallbackColor = colorForGroup(group)
    if (isKnownMerchant(merchant)) {
        val color = merchantBadgeColor(merchant, fallbackColor)
        Surface(
            shape = CircleShape,
            color = color,
            modifier = modifier.size(40.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    text = merchantInitial(merchant),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    } else {
        Surface(
            shape = CircleShape,
            color = fallbackColor.copy(alpha = 0.15f),
            modifier = modifier.size(40.dp)
        ) {
            Icon(
                imageVector = iconForGroup(group),
                contentDescription = null,
                tint = fallbackColor,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
