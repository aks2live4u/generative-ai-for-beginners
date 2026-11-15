package com.aks.gallerysorter.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

private var activeScreens = 0

/** Blocks screenshots and recent-apps previews while [content] is composed (ref-counted). */
@Composable
fun SecureScreen(content: @Composable () -> Unit) {
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window) {
        if (window != null) {
            if (activeScreens++ == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (window != null && --activeScreens == 0) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
