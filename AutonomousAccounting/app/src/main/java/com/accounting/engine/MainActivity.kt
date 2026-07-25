package com.accounting.engine

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.accounting.engine.security.BiometricGate
import com.accounting.engine.ui.AccountingViewModelFactory
import com.accounting.engine.ui.navigation.AccountingNavGraph
import com.accounting.engine.ui.theme.AutonomousAccountingTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots/recents-preview and screen recording of financial data.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val app = application as AccountingApplication
        val viewModelFactory = AccountingViewModelFactory(app.repository)

        setContent {
            AutonomousAccountingTheme {
                var isUnlocked by remember { mutableStateOf(false) }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (isUnlocked) {
                        AccountingNavGraph(viewModelFactory)
                    } else {
                        LockScreen(
                            onUnlockRequested = {
                                BiometricGate.authenticate(
                                    activity = this,
                                    onSuccess = { isUnlocked = true },
                                    onFailure = { /* stays locked; user can retry via the button */ }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockScreen(onUnlockRequested: () -> Unit) {
    LaunchedEffect(Unit) { onUnlockRequested() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            Text("Unlock to view your ledger")
            Button(onClick = onUnlockRequested) { Text("Unlock") }
        }
    }
}
