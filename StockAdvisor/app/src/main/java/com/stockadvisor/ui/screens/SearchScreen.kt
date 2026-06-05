package com.stockadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockadvisor.R
import com.stockadvisor.ui.theme.*

// Market options — encoded as a prefix the resolver understands
private data class Market(
    val label: String,
    val flag: String,
    val code: String,          // prefix sent to YahooFinanceApi
    val hint: String           // placeholder hint text
)

private val MARKETS = listOf(
    Market("India",  "🇮🇳", "IN",   "RELIANCE, ITC, HDFC, ETERNAL"),
    Market("US",     "🇺🇸", "US",   "AAPL, TSLA, MSFT, GOOGL"),
    Market("Global", "🌍", "AUTO", "BTC-USD, ^GSPC, ETF symbols"),
    Market("Auto",   "🔍", "AUTO", "Any name or ticker"),
)

@Composable
fun SearchScreen(
    onContinue: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var ticker by remember { mutableStateOf("") }
    var selectedMarket by remember { mutableStateOf(MARKETS[0]) }   // default: India
    var showError by remember { mutableStateOf(false) }

    fun submit() {
        val sym = ticker.trim().uppercase()
        if (sym.isEmpty()) { showError = true; return }
        // Encode market prefix so the resolver knows which exchange to target
        val encoded = when (selectedMarket.code) {
            "IN"   -> "IN:$sym"
            "US"   -> "US:$sym"
            else   -> sym          // AUTO — let resolver decide
        }
        onContinue(encoded)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = OnSurfaceVariant)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_indistock_logo),
                contentDescription = "IndiStock Advisor",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "IndiStock Advisor",
                style = MaterialTheme.typography.headlineMedium,
                color = GoldAccent,
                fontWeight = FontWeight.Bold
            )
            Text(
                "AI-powered investment research",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // ── Market / country selector ───────────────────────────────────
            Text(
                "Select market",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MARKETS.forEach { market ->
                    val selected = market == selectedMarket
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) GoldAccent.copy(alpha = 0.15f) else Surface)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) GoldAccent else SurfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedMarket = market; ticker = "" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(market.flag, fontSize = 18.sp)
                            Text(
                                market.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) GoldAccent else OnSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Search field ────────────────────────────────────────────────
            OutlinedTextField(
                value = ticker,
                onValueChange = { ticker = it.uppercase(); showError = false },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        selectedMarket.hint,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = TextStyle(textAlign = TextAlign.Center, color = OnSurface),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = if (ticker.isNotEmpty()) GoldAccent else OnSurfaceVariant
                    )
                },
                isError = showError,
                supportingText = if (showError) {
                    { Text("Please enter a company name or ticker", color = RedAccent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    cursorColor = GoldAccent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(10.dp))

            // Quick-fill chips — change based on selected market
            val chips = when (selectedMarket.code) {
                "IN"   -> listOf("RELIANCE", "ITC", "HDFC", "TCS")
                "US"   -> listOf("AAPL", "TSLA", "MSFT", "AMZN")
                else   -> listOf("BTC-USD", "^GSPC", "^NSEI", "GLD")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                chips.forEach { chip ->
                    SuggestionChip(
                        onClick = { ticker = chip; showError = false },
                        label = { Text(chip, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = SurfaceVariant,
                            labelColor = OnSurfaceVariant
                        ),
                        border = null,
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = OnPrimary)
            ) {
                Text("Continue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "For educational purposes only. Not financial advice.",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
