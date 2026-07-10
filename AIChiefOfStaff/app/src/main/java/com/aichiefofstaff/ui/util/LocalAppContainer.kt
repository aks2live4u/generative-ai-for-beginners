package com.aichiefofstaff.ui.util

import androidx.compose.runtime.staticCompositionLocalOf
import com.aichiefofstaff.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
