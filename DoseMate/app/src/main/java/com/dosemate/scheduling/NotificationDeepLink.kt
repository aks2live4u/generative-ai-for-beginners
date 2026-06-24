package com.dosemate.scheduling

import androidx.compose.runtime.mutableStateOf

/** Carries the "tapped notification" event from MainActivity into the Dashboard composable. */
object NotificationDeepLink {
    val pendingLogId = mutableStateOf<Long?>(null)
    val pendingMedicineName = mutableStateOf<String?>(null)
    val pendingDosage = mutableStateOf<String?>(null)

    fun set(logId: Long, medicineName: String?, dosage: String?) {
        pendingLogId.value = logId
        pendingMedicineName.value = medicineName
        pendingDosage.value = dosage
    }

    fun clear() {
        pendingLogId.value = null
        pendingMedicineName.value = null
        pendingDosage.value = null
    }
}
