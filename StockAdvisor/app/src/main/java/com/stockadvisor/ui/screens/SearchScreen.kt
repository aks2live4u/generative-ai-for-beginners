package com.stockadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stockadvisor.ui.theme.*

@Composable
fun SearchScreen(onContinue: (String) -> Unit) {
    var ticker by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    fun submit() {
        if (ticker.trim().isEmpty()) showError = true
        else onContinue(ticker.trim().uppercase())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GreenAccent.copy(alpha = 0.05f), Background)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = GreenAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Stock Advisor",
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "AI-powered investment analysis",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = ticker,
                onValueChange = {
                    ticker = it.uppercase()
                    showError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Enter ticker (e.g. AAPL, VOO, FXAIX)", color = OnSurfaceVariant)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (ticker.isNotEmpty()) GreenAccent else OnSurfaceVariant
                    )
                },
                isError = showError,
                supportingText = if (showError) {
                    { Text("Please enter a ticker symbol", color = RedAccent) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenAccent,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    cursorColor = GreenAccent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("AAPL", "VOO", "BTC-USD", "FXAIX").forEach { example ->
                    SuggestionChip(
                        onClick = { ticker = example; showError = false },
                        label = { Text(example, style = MaterialTheme.typography.labelMedium) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = SurfaceVariant,
                            labelColor = OnSurfaceVariant
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent,
                    contentColor = OnPrimary
                )
            ) {
                Text("Continue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "For educational purposes only. Not financial advice.",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
