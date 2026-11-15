package com.aks.gallerysorter.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/** FLAG_SECURE is per window, so count per window (a recreated activity has a new one). */
private val activeScreens = java.util.WeakHashMap<Window, Int>()

/** Blocks screenshots and recent-apps previews while [content] is composed (ref-counted). */
@Composable
fun SecureScreen(content: @Composable () -> Unit) {
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window) {
        if (window != null) {
            val n = activeScreens[window] ?: 0
            activeScreens[window] = n + 1
            if (n == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (window != null) {
                val n = (activeScreens[window] ?: 1) - 1
                if (n <= 0) {
                    activeScreens.remove(window)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    activeScreens[window] = n
                }
            }
        }
    }
    content()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
