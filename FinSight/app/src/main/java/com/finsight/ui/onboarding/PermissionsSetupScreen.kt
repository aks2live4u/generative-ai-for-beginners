package com.finsight.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.finsight.ui.components.PrimaryButton

data class PermissionItem(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val granted: Boolean,
    val optional: Boolean = false
)

/**
 * SMS and Notification access are required to auto-detect transactions; Gmail sign-in is optional
 * since not every user banks via email statements. Every permission description below states the
 * read-only guarantee explicitly so users understand exactly what's being granted.
 */
@Composable
fun PermissionsSetupScreen(
    permissions: List<PermissionItem>,
    onRequestPermission: (String) -> Unit,
    onContinue: () -> Unit
) {
    val requiredGranted = permissions.filter { !it.optional }.all { it.granted }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text(
                text = "Grant read-only access",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "These permissions let the app detect transactions automatically. Nothing is ever sent, modified, or shared.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            permissions.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp, end = 12.dp)
                        ) {
                            Text(
                                text = item.title + if (item.optional) " (optional)" else "",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.granted) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Granted",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            OutlinedButton(onClick = { onRequestPermission(item.key) }) {
                                Text("Grant")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "Continue",
                enabled = requiredGranted,
                onClick = onContinue
            )
        }
    }
}

object PermissionKeys {
    const val SMS = "sms"
    const val NOTIFICATIONS = "notifications"
    const val GMAIL = "gmail"
}

val DefaultPermissionIcons = mapOf(
    PermissionKeys.SMS to Icons.Filled.Sms,
    PermissionKeys.NOTIFICATIONS to Icons.Filled.Notifications,
    PermissionKeys.GMAIL to Icons.Filled.Email
)
