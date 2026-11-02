package com.nexus.launcher.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nexus.launcher.NexusGraph
import com.nexus.launcher.ai.NexusAIException
import kotlinx.coroutines.launch
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.ui.components.GlassCard
import com.nexus.launcher.ui.components.HudLabel
import com.nexus.launcher.ui.components.Pill
import com.nexus.launcher.ui.components.ScreenHeader
import com.nexus.launcher.ui.components.SettingRow
import com.nexus.launcher.ui.theme.Nx

@Composable
fun SettingsScreen(graph: NexusGraph, settings: NexusSettings, onDone: () -> Unit) {
    val context = LocalContext.current
    var keyVisible by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Nx.Cyan, unfocusedBorderColor = Nx.Line,
        focusedContainerColor = Nx.Surface, unfocusedContainerColor = Nx.Surface, cursorColor = Nx.Cyan,
        focusedLabelColor = Nx.Cyan, unfocusedLabelColor = Nx.Muted,
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader("Settings", "Configure NEXUS", trailing = {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Nx.Muted) }
        })

        GlassCard(Modifier.fillMaxWidth()) {
            HudLabel("Intelligence", color = Nx.Cyan)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = { v -> graph.settings.update { it.copy(apiKey = v) } },
                label = { Text("Gemini API key") },
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (keyVisible) "Hide key" else "Show key", tint = Nx.Muted,
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Stored only on this phone. Get a free key at aistudio.google.com/apikey.",
                style = MaterialTheme.typography.bodyMedium, color = Nx.Muted, modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = settings.model,
                onValueChange = { v -> graph.settings.update { it.copy(model = v) } },
                label = { Text("Gemini model") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = settings.userName,
                onValueChange = { v -> graph.settings.update { it.copy(userName = v) } },
                label = { Text("What should NEXUS call you?") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Spacer(Modifier.height(12.dp))
            Pill(
                if (testing) "Testing connection…" else "Test connection",
                onClick = {
                    if (!testing) {
                        testing = true
                        testResult = null
                        graph.scope.launch {
                            testResult = try {
                                graph.ai.testConnection() to true
                            } catch (e: NexusAIException) {
                                (e.message ?: "Connection failed.") to false
                            } catch (e: Exception) {
                                "Connection failed: ${e.message}" to false
                            }
                            testing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            testResult?.let { (msg, ok) ->
                Text(
                    msg, style = MaterialTheme.typography.bodyMedium, color = if (ok) Nx.Mint else Nx.Amber,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            SettingRow("Live web answers", "Lets NEXUS search the web for weather, news and other live facts.", settings.webSearch) { v ->
                graph.settings.update { it.copy(webSearch = v) }
            }
            SettingRow("Spoken replies", "Read NEXUS's answers aloud.", settings.voiceReplies) { v ->
                graph.settings.update { it.copy(voiceReplies = v) }
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            HudLabel("Comfort", color = Nx.Cyan)
            SettingRow("Reduce motion", "Stops spinning rings, drifting particles and momentum.", settings.reducedMotion) { v ->
                graph.settings.update { it.copy(reducedMotion = v) }
            }
            SettingRow("Classic app grid", "Show apps as a simple alphabetical grid instead of orbits.", settings.classicGrid) { v ->
                graph.settings.update { it.copy(classicGrid = v) }
            }
            SettingRow("Interface sounds", "Soft tones for taps, voice and launches.", settings.sounds) { v ->
                graph.settings.update { it.copy(sounds = v) }
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            HudLabel("System", color = Nx.Cyan)
            Spacer(Modifier.height(10.dp))
            Pill("Make NEXUS the default home app", icon = Icons.Outlined.Home, onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Pill("Notification access", onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Pill("Android settings", onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
        HudLabel("NEXUS · JARVIS Edition · 0.1.0")
        Spacer(Modifier.height(24.dp))
    }
}
