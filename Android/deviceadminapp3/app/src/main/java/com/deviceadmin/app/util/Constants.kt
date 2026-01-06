package com.deviceadmin.app.util

/**
 * Application-wide constants.
 */
object Constants {
    
    // Master PIN for admin access
    const val MASTER_PIN = "1133"
    
    // Break duration in minutes
    const val BREAK_DURATION_MINUTES = 2
    
    // Work Manager
    const val STATUS_WORK_NAME = "StatusSyncWork"
    const val STATUS_WORK_INTERVAL_MINUTES = 15L
    
    // Intent extras
    const val EXTRA_SHOW_ADMIN_PANEL = "show_admin_panel"
    
    // Broadcast actions
    const val ACTION_UNLOCK_DEVICE = "com.deviceadmin.app.UNLOCK_DEVICE"
    
    // Secret dial code
    const val SECRET_DIAL_CODE = "1133"
}
