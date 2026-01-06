package com.deviceadmin.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.deviceadmin.app.data.local.PreferencesManager
import com.deviceadmin.app.data.model.PhoneState
import com.deviceadmin.app.ui.lock.LockScreenActivity

/**
 * Accessibility Service for device protection.
 * Monitors and blocks attempts to uninstall or disable the app.
 */
class ProtectionAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ProtectionService"
        
        @Volatile
        var isRunning = false
            private set
        
        // Packages that should be monitored for settings access
        private val MONITORED_PACKAGES = listOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.coloros.packageinstaller",
            "com.oplus.packageinstaller",
            "com.oplus.safecenter",
            "com.google.android.gms",
            "com.android.vending"
        )
        
        // Keywords that indicate protected pages
        private val PROTECTED_KEYWORDS = listOf(
            "my downloads",
            "device controller",
            "com.deviceadmin.app"
        )
        
        // Keywords that indicate dangerous actions
        private val DANGEROUS_ACTIONS = listOf(
            "uninstall",
            "force stop",
            "clear data",
            "deactivate",
            "disable"
        )
    }

    private lateinit var prefsManager: PreferencesManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        prefsManager = PreferencesManager(this)
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        val state = prefsManager.getDeviceState()
        
        // Skip if setup not complete
        if (!state.isSetupComplete) return
        
        val packageName = event.packageName?.toString() ?: return
        
        // === Self Defense: Block uninstall attempts ===
        if (!state.isUninstallAllowed && MONITORED_PACKAGES.contains(packageName)) {
            if (shouldBlockCurrentScreen()) {
                blockAndNavigateHome()
                return
            }
        }
        
        // === Lock Enforcement: Keep lock screen on top ===
        if (state.phoneState == PhoneState.FROZEN && !prefsManager.isOnBreak()) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (!isAllowedPackageDuringLock(packageName)) {
                    bringLockScreenToFront()
                }
            }
        }
    }

    /**
     * Checks if the current screen content should be blocked.
     */
    private fun shouldBlockCurrentScreen(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val allText = collectAllText(rootNode).lowercase()
        
        // Check if this is about our app
        val isAboutOurApp = PROTECTED_KEYWORDS.any { allText.contains(it) }
        
        if (isAboutOurApp) {
            // Check for dangerous actions
            val hasDangerousAction = DANGEROUS_ACTIONS.any { allText.contains(it) }
            
            // Check for device admin pages
            val isAdminPage = allText.contains("device admin") || 
                              allText.contains("device manager") ||
                              allText.contains("administrator")
            
            return hasDangerousAction || isAdminPage
        }
        
        return false
    }

    /**
     * Collects all text content from the accessibility node tree.
     */
    private fun collectAllText(node: AccessibilityNodeInfo): String {
        val builder = StringBuilder()
        
        node.text?.let { builder.append(it).append(" ") }
        node.contentDescription?.let { builder.append(it).append(" ") }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                builder.append(collectAllText(child))
                child.recycle()
            }
        }
        
        return builder.toString()
    }

    /**
     * Blocks the current action and navigates home.
     */
    private fun blockAndNavigateHome() {
        Log.d(TAG, "Blocking restricted action")
        performGlobalAction(GLOBAL_ACTION_HOME)
        performGlobalAction(GLOBAL_ACTION_BACK)
        Toast.makeText(this, "Action Blocked by Administrator", Toast.LENGTH_SHORT).show()
    }

    /**
     * Brings the lock screen activity to the front.
     */
    private fun bringLockScreenToFront() {
        Log.d(TAG, "Bringing lock screen to front")
        
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        startActivity(intent)
    }

    /**
     * Checks if a package is allowed during lock mode.
     */
    private fun isAllowedPackageDuringLock(packageName: String): Boolean {
        return packageName == "com.deviceadmin.app" ||
               packageName == "com.android.phone" ||
               packageName == "com.android.incallui" ||
               packageName == "com.google.android.dialer" ||
               packageName == "com.android.server.telecom"
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.d(TAG, "Service destroyed")
    }
}
