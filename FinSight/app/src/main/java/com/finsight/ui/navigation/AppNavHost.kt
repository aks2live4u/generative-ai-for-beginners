package com.finsight.ui.navigation

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.finsight.data.AppContainer
import com.finsight.security.BiometricAuthManager
import com.finsight.ui.chat.ChatMessage
import com.finsight.ui.chat.ChatScreen
import com.finsight.ui.dashboard.DashboardScreen
import com.finsight.ui.insights.InsightsScreen
import com.finsight.ui.lock.AppLockScreen
import com.finsight.ui.onboarding.DefaultPermissionIcons
import com.finsight.ui.onboarding.InitialScanScreen
import com.finsight.ui.onboarding.PermissionItem
import com.finsight.ui.onboarding.PermissionKeys
import com.finsight.ui.onboarding.PermissionsSetupScreen
import com.finsight.ui.onboarding.SecuritySetupScreen
import com.finsight.ui.onboarding.WelcomeScreen
import com.finsight.ui.state.buildDashboardState
import com.finsight.ui.state.buildInsightsState
import com.finsight.ui.state.buildTransactionsState
import com.finsight.ui.transactions.TransactionsScreen
import kotlinx.coroutines.launch

private object Routes {
    const val WELCOME = "welcome"
    const val SECURITY_SETUP = "security_setup"
    const val PERMISSIONS_SETUP = "permissions_setup"
    const val INITIAL_SCAN = "initial_scan"
    const val LOCK = "lock"
    const val MAIN = "main"
}

@Composable
fun AppNavHost(container: AppContainer, activity: FragmentActivity) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val biometricAuthManager = remember { BiometricAuthManager(activity) }

    val startDestination = remember {
        when {
            !container.onboardingPrefs.isOnboardingComplete -> Routes.WELCOME
            container.pinManager.isPinSet() -> Routes.LOCK
            else -> Routes.MAIN
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.WELCOME) {
            WelcomeScreen(onGetStarted = { navController.navigate(Routes.SECURITY_SETUP) })
        }

        composable(Routes.SECURITY_SETUP) {
            var biometricEnabled by remember { mutableStateOf(container.onboardingPrefs.isBiometricEnabled) }
            SecuritySetupScreen(
                biometricAvailable = biometricAuthManager.canAuthenticateWithBiometrics(),
                onEnableBiometric = {
                    biometricAuthManager.authenticate(
                        onSuccess = {
                            biometricEnabled = true
                            container.onboardingPrefs.isBiometricEnabled = true
                        },
                        onFailedOrError = { /* user can retry or rely on PIN */ }
                    )
                },
                biometricEnabled = biometricEnabled,
                onPinConfirmed = { pin -> container.pinManager.setPin(pin) },
                pinConfirmedAlready = container.pinManager.isPinSet(),
                onContinue = { navController.navigate(Routes.PERMISSIONS_SETUP) }
            )
        }

        composable(Routes.PERMISSIONS_SETUP) {
            var refreshTick by remember { mutableIntStateOf(0) }

            // Notification access is granted via a system Settings screen (no ActivityResult
            // callback), and returning from any permission dialog/sign-in also resumes this
            // activity, so re-check all three grants whenever the activity comes back to the
            // foreground - not just from the launcher callbacks below.
            DisposableEffect(activity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        refreshTick++
                    }
                }
                activity.lifecycle.addObserver(observer)
                onDispose { activity.lifecycle.removeObserver(observer) }
            }

            val smsPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { refreshTick++ }

            val gmailSignInLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
                refreshTick++
            }

            // Reading refreshTick as a remember() key is what makes these re-evaluate on
            // recomposition - writing refreshTick alone does nothing if its value is never read.
            val smsGranted = remember(refreshTick) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            val notificationsGranted = remember(refreshTick) {
                NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            }
            val gmailGranted = remember(refreshTick) {
                container.gmailAuthManager.getSignedInAccount() != null
            }

            val permissions = listOf(
                PermissionItem(
                    key = PermissionKeys.SMS,
                    title = "SMS Access",
                    description = "Read-only access to detect bank/UPI transaction SMS. Never sends messages.",
                    icon = DefaultPermissionIcons[PermissionKeys.SMS]!!,
                    granted = smsGranted
                ),
                PermissionItem(
                    key = PermissionKeys.NOTIFICATIONS,
                    title = "Notification Access",
                    description = "Read-only access to Swiggy, Zomato, Uber, Ola, Blinkit and Amazon notifications only.",
                    icon = DefaultPermissionIcons[PermissionKeys.NOTIFICATIONS]!!,
                    granted = notificationsGranted
                ),
                PermissionItem(
                    key = PermissionKeys.GMAIL,
                    title = "Gmail",
                    description = "Read-only scope to detect transactions from order/payment emails.",
                    icon = DefaultPermissionIcons[PermissionKeys.GMAIL]!!,
                    granted = gmailGranted,
                    optional = true
                )
            )

            PermissionsSetupScreen(
                permissions = permissions,
                onRequestPermission = { key ->
                    when (key) {
                        PermissionKeys.SMS -> smsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                        )
                        PermissionKeys.NOTIFICATIONS -> context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                        PermissionKeys.GMAIL -> gmailSignInLauncher.launch(container.gmailAuthManager.signInIntent())
                    }
                },
                onContinue = { navController.navigate(Routes.INITIAL_SCAN) }
            )
        }

        composable(Routes.INITIAL_SCAN) {
            var scanned by remember { mutableIntStateOf(0) }
            var imported by remember { mutableIntStateOf(0) }
            var isComplete by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                container.smsHistoryScanner.scanHistory { s, i ->
                    scanned = s
                    imported = i
                }
                isComplete = true
            }

            InitialScanScreen(
                scanned = scanned,
                imported = imported,
                isComplete = isComplete,
                onContinue = {
                    container.onboardingPrefs.isOnboardingComplete = true
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOCK) {
            var errorMessage by remember { mutableStateOf<String?>(null) }
            AppLockScreen(
                biometricAvailable = container.onboardingPrefs.isBiometricEnabled && biometricAuthManager.canAuthenticateWithBiometrics(),
                onBiometricRequested = {
                    biometricAuthManager.authenticate(
                        onSuccess = {
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.LOCK) { inclusive = true }
                            }
                        },
                        onFailedOrError = { message -> errorMessage = message }
                    )
                },
                onVerifyPin = { pin -> container.pinManager.verifyPin(pin) },
                onUnlocked = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOCK) { inclusive = true }
                    }
                },
                errorMessage = errorMessage
            )
        }

        composable(Routes.MAIN) {
            MainScaffold(container = container)
        }
    }
}

@Composable
private fun MainScaffold(container: AppContainer) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    val transactions by container.transactionRepository.observeAll().collectAsState(initial = emptyList())
    val subscriptions by container.subscriptionRepository.observeAll().collectAsState(initial = emptyList())
    var transactionsSearchQuery by remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateOf(listOf<ChatMessage>()) }
    var chatInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = { FinanceBottomNavBar(currentTab = currentTab, onTabSelected = { currentTab = it }) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    state = buildDashboardState(transactions, subscriptions, userName = "there"),
                    onOpenChat = { currentTab = AppTab.CHAT },
                    onOpenInsights = { currentTab = AppTab.INSIGHTS },
                    onOpenNotifications = {}
                )
                AppTab.TRANSACTIONS -> TransactionsScreen(
                    state = buildTransactionsState(transactions, subscriptions, transactionsSearchQuery),
                    onSearchQueryChange = { transactionsSearchQuery = it },
                    onTransactionClick = {}
                )
                AppTab.INSIGHTS -> InsightsScreen(
                    state = buildInsightsState(transactions, subscriptions),
                    onOpenChat = { currentTab = AppTab.CHAT },
                    onBackupNow = { container.backupManager.createLocalBackup() != null }
                )
                AppTab.CHAT -> ChatScreen(
                    messages = chatMessages.value,
                    inputText = chatInput,
                    onInputChange = { chatInput = it },
                    onSend = {
                        val question = chatInput
                        if (question.isNotBlank()) {
                            chatMessages.value = chatMessages.value + ChatMessage(question, isUser = true)
                            chatInput = ""
                            coroutineScope.launch {
                                val answer = com.finsight.core.ai.ChatAssistantEngine.answer(question, container.financeDataProvider)
                                chatMessages.value = chatMessages.value + ChatMessage(answer, isUser = false)
                            }
                        }
                    }
                )
            }
        }
    }
}
