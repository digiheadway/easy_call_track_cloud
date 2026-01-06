package com.clicktoearn.linkbox.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Activity.setScreenshotDisabled(disabled: Boolean) {
    if (disabled) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
