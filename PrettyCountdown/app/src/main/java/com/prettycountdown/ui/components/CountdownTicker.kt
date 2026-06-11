package com.prettycountdown.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/**
 * A [State] holding the current time in millis, refreshed every [periodMillis].
 * Lets countdown screens feel "alive" without recomputing on every frame.
 * Defaults to a 1-second tick so every visible countdown reads like a real clock.
 */
@Composable
fun rememberNowState(periodMillis: Long = 1_000L): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMillis) {
        while (true) {
            delay(periodMillis)
            now.longValue = System.currentTimeMillis()
        }
    }
    return now
}
