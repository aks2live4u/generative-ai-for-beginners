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
import com.finsight.ui.settings.SettingsScreen
import com.finsight.ui.settings.SettingsUiState
import com.finsight.ui.state.TimePeriod
import com.finsight.ui.state.buildDashboardState
import com.finsight.ui.state.buildInsightsState
import com.finsight.ui.state.buildTransactionsState
import com.finsight.ui.transactions.TransactionFilter
import com.finsight.ui.transactions.TransactionsScreen
import com.finsight.core.ai.llm.FinanceContextBuilder
import com.finsight.llm.GeminiResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    val transactions by container.transactionRepository.observeAll().collectAsState(initial = emptyList())
    val subscriptions by container.subscriptionRepository.observeAll().collectAsState(initial = emptyList())
    var transactionsSearchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTH) }
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    val chatMessages = remember { mutableStateOf(listOf<ChatMessage>()) }
    var chatInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var settingsVersion by remember { mutableIntStateOf(0) }
    val settingsState = remember(settingsVersion) {
        SettingsUiState(
            aiFeaturesEnabled = container.geminiSettingsManager.aiFeaturesEnabled,
            hasApiKey = !container.geminiSettingsManager.apiKey.isNullOrBlank(),
            lastCrashLog = com.finsight.CrashHandler.lastCrash(context)
        )
    }

    // Re-discover recurring payments (EMI/SIP/insurance/subscriptions/rent) whenever the
    // transaction list changes, with zero manual setup. Subscription.serviceName is the Room
    // primary key, so upsert() naturally keeps one row per merchant instead of growing duplicates.
    LaunchedEffect(transactions) {
        // Merchant clustering is O(n^2) in comparisons and can take a real while against a device's
        // full transaction history, so it must run off the main thread - otherwise it blocks
        // Compose's UI dispatcher and the app appears frozen (or gets killed as unresponsive).
        val detections = withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.finsight.core.ai.RecurringPaymentDetector.detect(transactions)
        }
        detections.forEach { detection ->
            container.subscriptionRepository.upsert(
                com.finsight.core.model.Subscription(
                    serviceName = detection.merchantLabel,
                    renewalDate = detection.nextExpectedDate,
                    monthlyCost = detection.monthlyCost
                )
            )
        }
    }

    Scaffold(
        bottomBar = { FinanceBottomNavBar(currentTab = currentTab, onTabSelected = { currentTab = it }) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    state = buildDashboardState(transactions, subscriptions, userName = "there", period = selectedPeriod),
                    onOpenChat = { currentTab = AppTab.CHAT },
                    onOpenInsights = { currentTab = AppTab.INSIGHTS },
                    onOpenNotifications = {},
                    onPeriodSelected = { selectedPeriod = it },
                    onReclassifyCategory = { from, to ->
                        coroutineScope.launch { container.transactionRepository.reclassifyCategory(from, to) }
                    }
                )
                AppTab.TRANSACTIONS -> TransactionsScreen(
                    state = buildTransactionsState(
                        transactions,
                        subscriptions,
                        transactionsSearchQuery,
                        period = selectedPeriod,
                        filter = selectedFilter
                    ),
                    onSearchQueryChange = { transactionsSearchQuery = it },
                    onTransactionClick = {},
                    onReclassifyTransaction = { tx, category ->
                        coroutineScope.launch {
                            tx.id?.let { container.transactionRepository.reclassify(it, category) }
                        }
                    },
                    onReclassifyCategory = { from, to ->
                        coroutineScope.launch { container.transactionRepository.reclassifyCategory(from, to) }
                    },
                    onPeriodSelected = { selectedPeriod = it },
                    onFilterSelected = { selectedFilter = it }
                )
                AppTab.INSIGHTS -> {
                    val insightsState = buildInsightsState(transactions, subscriptions)
                    InsightsScreen(
                        state = insightsState,
                        onOpenChat = { currentTab = AppTab.CHAT },
                        onBackupNow = { container.backupManager.createLocalBackup() != null },
                        aiFeaturesEnabled = settingsState.aiFeaturesEnabled && settingsState.hasApiKey,
                        onExplainHealthScore = insightsState.healthScore?.let { score ->
                            {
                                askGeminiOrFail(container) { apiKey ->
                                    val context = FinanceContextBuilder.buildHealthScoreContext(score)
                                    container.geminiClient.generateContent(
                                        apiKey = apiKey,
                                        systemInstruction = EXPLAIN_SYSTEM_INSTRUCTION,
                                        prompt = "Financial health score breakdown:\n$context\n\n" +
                                            "Explain this score and what's driving it up or down, in plain language."
                                    )
                                }
                            }
                        },
                        onExplainHiddenExpenses = if (insightsState.savingsOpportunities.isNotEmpty()) {
                            {
                                askGeminiOrFail(container) { apiKey ->
                                    val context = FinanceContextBuilder.buildSavingsOpportunitiesContext(insightsState.savingsOpportunities)
                                    container.geminiClient.generateContent(
                                        apiKey = apiKey,
                                        systemInstruction = EXPLAIN_SYSTEM_INSTRUCTION,
                                        prompt = "Hidden expense findings:\n$context\n\n" +
                                            "Explain these findings, and flag anything that looks wrong or unrealistic."
                                    )
                                }
                            }
                        } else null,
                        onRunSmartScan = {
                            askGeminiOrFail(container) { apiKey ->
                                val context = FinanceContextBuilder.buildSmartScanContext(transactions)
                                container.geminiClient.generateContent(
                                    apiKey = apiKey,
                                    systemInstruction = SMART_SCAN_SYSTEM_INSTRUCTION,
                                    prompt = "Transactions:\n$context"
                                )
                            }
                        }
                    )
                }
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
                                val answer = answerWithAiIfEnabled(container, question)
                                chatMessages.value = chatMessages.value + ChatMessage(answer, isUser = false)
                            }
                        }
                    }
                )
                AppTab.SETTINGS -> SettingsScreen(
                    state = settingsState,
                    onSaveApiKey = { key ->
                        container.geminiSettingsManager.apiKey = key
                        settingsVersion++
                    },
                    onClearApiKey = {
                        container.geminiSettingsManager.clearApiKey()
                        settingsVersion++
                    },
                    onToggleAiFeatures = { enabled ->
                        container.geminiSettingsManager.aiFeaturesEnabled = enabled
                        settingsVersion++
                    },
                    onTestConnection = {
                        val apiKey = container.geminiSettingsManager.apiKey
                        if (apiKey.isNullOrBlank()) {
                            Result.failure(IllegalStateException("No API key saved"))
                        } else {
                            when (val result = container.geminiClient.generateContent(
                                apiKey = apiKey,
                                systemInstruction = "Reply with a short one-sentence greeting confirming the connection works.",
                                prompt = "Say hello."
                            )) {
                                is GeminiResult.Success -> Result.success(result.text.trim())
                                is GeminiResult.Failure -> Result.failure(Exception(result.message))
                            }
                        }
                    },
                    onCopyCrashLog = { log ->
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("FinSight crash log", log))
                    },
                    onClearCrashLog = {
                        com.finsight.CrashHandler.clear(context)
                        settingsVersion++
                    }
                )
            }
        }
    }
}

private const val EXPLAIN_SYSTEM_INSTRUCTION =
    "You are FinSight's personal finance assistant. Explain the already-computed figures given " +
        "to you in plain, concise language. Don't recompute or second-guess the numbers - just " +
        "interpret them for a non-technical user, and point out anything that looks off."

private suspend fun askGeminiOrFail(
    container: AppContainer,
    call: suspend (apiKey: String) -> GeminiResult
): Result<String> {
    val apiKey = container.geminiSettingsManager.apiKey
    if (apiKey.isNullOrBlank()) {
        return Result.failure(IllegalStateException("No API key saved"))
    }
    return when (val result = call(apiKey)) {
        is GeminiResult.Success -> Result.success(result.text.trim())
        is GeminiResult.Failure -> Result.failure(Exception(result.message))
    }
}

private const val SMART_SCAN_SYSTEM_INSTRUCTION =
    "You are reviewing a personal finance app's imported transactions (from SMS, email and " +
        "notifications, already deduplicated for exact matches). Look across them and report in " +
        "plain text, in three short sections: (1) likely cross-source duplicates (same purchase " +
        "logged twice from different sources/wording), (2) anomalies that look fraud-like (unusual " +
        "amount/merchant/timing patterns), (3) any insurance policies you can identify from the " +
        "merchant/category. If a section has nothing to report, say so briefly."

private const val CHAT_SYSTEM_INSTRUCTION =
    "You are FinSight's personal finance assistant. Answer the user's question using only the " +
        "financial context provided below. Be concise and specific with numbers. If the context " +
        "doesn't contain enough information to answer, say so rather than guessing."

private suspend fun answerWithAiIfEnabled(container: AppContainer, question: String): String {
    val taughtRule = com.finsight.core.parser.MerchantRuleParser.parse(question)
    if (taughtRule != null) {
        val taughtPurpose = com.finsight.core.model.Purpose.fromDisplayName(taughtRule.label)
        if (taughtPurpose != null) {
            container.purposeRuleManager.addRule(taughtRule.merchantKey, taughtPurpose)
            return "Got it - I'll tag \"${taughtRule.merchantKey}\" as ${taughtPurpose.displayName.lowercase()} spending from now on."
        }
        container.merchantRuleManager.addRule(taughtRule.merchantKey, taughtRule.label)
        container.transactionRepository.relabelPastTransactions(taughtRule.merchantKey, taughtRule.label)
        return "Got it - I'll label \"${taughtRule.merchantKey}\" as \"${taughtRule.label}\" from now on, " +
            "and I've updated your past transactions too."
    }

    val settings = container.geminiSettingsManager
    val apiKey = settings.apiKey
    if (!settings.isConfigured() || apiKey.isNullOrBlank()) {
        return com.finsight.core.ai.ChatAssistantEngine.answer(question, container.financeDataProvider)
    }
    val context = FinanceContextBuilder.buildChatContext(container.financeDataProvider)
    val prompt = "Financial context:\n$context\n\nUser question: $question"
    return when (val result = container.geminiClient.generateContent(apiKey, CHAT_SYSTEM_INSTRUCTION, prompt)) {
        is GeminiResult.Success -> result.text.trim()
        is GeminiResult.Failure -> com.finsight.core.ai.ChatAssistantEngine.answer(question, container.financeDataProvider)
    }
}
