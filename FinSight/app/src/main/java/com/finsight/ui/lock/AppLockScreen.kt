package com.finsight.ui.lock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finsight.ui.components.PrimaryButton

/**
 * Shown on every app launch (and resume after backgrounding) when a PIN has been configured.
 * Tries biometrics first via [onBiometricRequested]; the PIN field is always available as the
 * mandatory fallback per the product's security flow.
 */
@Composable
fun AppLockScreen(
    biometricAvailable: Boolean,
    onBiometricRequested: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit,
    errorMessage: String?
) {
    var pin by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var localError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) onBiometricRequested()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(96.dp))
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter your PIN to unlock",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
                label = { Text("PIN") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            (errorMessage ?: localError)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = "Unlock",
                enabled = pin.isNotBlank(),
                onClick = {
                    if (onVerifyPin(pin)) {
                        onUnlocked()
                    } else {
                        localError = "Incorrect PIN"
                        pin = ""
                    }
                }
            )
            if (biometricAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(onClick = onBiometricRequested) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = "Use biometrics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
