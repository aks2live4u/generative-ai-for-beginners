package com.stockadvisor.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stockadvisor.R
import com.stockadvisor.ui.theme.*

@Composable
fun SearchScreen(
    onContinue: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
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
            .statusBarsPadding()
    ) {
        // Settings gear — top right corner
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "API Key Settings",
                tint = OnSurfaceVariant
            )
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
                    .size(160.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "IndiStock Advisor",
                style = MaterialTheme.typography.headlineMedium,
                color = GoldAccent,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "AI-powered investment research",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = ticker,
                onValueChange = {
                    ticker = it.uppercase()
                    showError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "e.g. RELIANCE, ITC, AAPL, ^NSEI",
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = TextStyle(textAlign = TextAlign.Center, color = OnSurface),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("RELIANCE", "ITC", "AAPL", "^NSEI").forEach { example ->
                    SuggestionChip(
                        onClick = { ticker = example; showError = false },
                        label = { Text(example, style = MaterialTheme.typography.labelMedium) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = SurfaceVariant,
                            labelColor = OnSurfaceVariant
                        ),
                        border = null,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Type any company name or ticker symbol.\nWe'll find it on Yahoo Finance automatically.",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
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
