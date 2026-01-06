package com.example.deviceadmin

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class LockAccessibilityService : AccessibilityService() {

    companion object {
        var isRunning = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isSetupComplete = prefs.getBoolean("setup_complete", false)
        
        // CRITICAL: Don't enforce ANYTHING until setup is complete
        if (!isSetupComplete) {
            return
        }
        
        val lastStatus = prefs.getString("last_status", "UNLOCK")
        val isUninstallAllowed = prefs.getBoolean("uninstall_allowed", false)
        val packageName = event.packageName?.toString() ?: ""

        // --- 1. SELF DEFENSE (Runs even if UNLOCKED, but only after setup) ---
        if (!isUninstallAllowed) {
            // Block Settings, Package Installer, and Google Play Store if they try to touch our app
            val restrictedPackages = listOf(
                "com.android.settings",
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.samsung.android.packageinstaller",
                "com.coloros.packageinstaller",
                "com.oplus.packageinstaller",
                "com.oplus.safecenter",
                "com.google.android.gms", // For some admin settings
                "com.android.vending" // Play Store
            )

            if (restrictedPackages.contains(packageName)) {
                val rootNode = rootInActiveWindow
                if (isRestrictedSettingsPage(rootNode) || containsRestrictedText(event.text)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Toast.makeText(this, "Action Blocked by Administrator", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        // --- 2. LOCK ENFORCEMENT (Runs only if LOCKED) ---
        if (lastStatus == "LOCK") {
            val breakEndTime = prefs.getLong("break_end_time", 0)
            if (System.currentTimeMillis() < breakEndTime) {
                return // On break
            }

            // Only enforce on Window State Changes to avoid event loops from background UI updates
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                
                // Block other apps
                if (packageName != "com.example.deviceadmin") {
                    val allowedPackages = listOf(
                        "com.android.phone",
                        "com.android.incallui",
                        "com.google.android.dialer",
                        "com.android.server.telecom",
                        "com.android.settings" // Settings allowed (but restricted pages blocked above)
                    )

                    if (!allowedPackages.contains(packageName)) {
                        // Special handling for SystemUI (Notification Shade)
                        if (packageName == "com.android.systemui") {
                            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                        
                        // Force Lock Screen to Front
                        launchLockScreen()
                    }
                }
            }
        }
    }

    private fun isRestrictedSettingsPage(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        val textList = getAllText(rootNode)
        val fullText = textList.joinToString(" ").lowercase()
        
        // 1. Check for Device Admin / Deactivate screen
        val isAdminPage = (fullText.contains("device admin") || 
                           fullText.contains("device manager") || 
                           fullText.contains("administrator")) && 
                          (fullText.contains("active") || 
                           fullText.contains("apps") || 
                           fullText.contains("deactivate") || 
                           fullText.contains("remove"))
        
        if (isAdminPage) {
            // Only block if it's OUR app's admin page
            if (fullText.contains("my downloads") || 
                fullText.contains("device controller") || 
                fullText.contains("com.example.deviceadmin")) {
                return true
            }
        }

        // 2. Check for App Info / Uninstall screen for OUR app
        val isAppInfoPage = fullText.contains("app info") || 
                            fullText.contains("application info") || 
                            fullText.contains("app details") || 
                            fullText.contains("application details") ||
                            fullText.contains("storage") // Sometimes used in some UIs
        
        if (isAppInfoPage) {
            // Check if it's OUR app
            if (fullText.contains("my downloads") || 
                fullText.contains("device controller") || 
                fullText.contains("com.example.deviceadmin")) {
                return true
            }
        }
        
        // 3. Check for specific Uninstall Dialogs
        if (fullText.contains("uninstall") && 
            (fullText.contains("my downloads") || fullText.contains("device controller"))) {
            return true
        }

        return false
    }

    private fun containsRestrictedText(textList: List<CharSequence>): Boolean {
        val str = textList.joinToString(" ").lowercase()
        
        // Check for deactivation or uninstallation of the app
        val isDeactivation = (str.contains("device admin") || str.contains("admin")) && 
                             (str.contains("deactivate") || str.contains("off") || str.contains("disable"))
        
        val isOurApp = str.contains("my downloads") || 
                       str.contains("device controller") || 
                       str.contains("com.example.deviceadmin")
        
        if (isDeactivation && isOurApp) return true
        if (isOurApp && (str.contains("uninstall") || str.contains("force stop") || str.contains("clear data"))) return true
        
        return false
    }
    
    private fun getAllText(node: AccessibilityNodeInfo, list: MutableList<String> = mutableListOf()): List<String> {
        if (node.text != null) list.add(node.text.toString())
        if (node.contentDescription != null) list.add(node.contentDescription.toString())
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                getAllText(child, list)
                child.recycle() // Important for memory
            }
        }
        return list
    }

    private fun launchLockScreen() {
        // Only launch if not already on top? 
        // We can't easily check, but firing intent with SINGLE_TOP is safe.
        val intent = Intent(this, LockActivity::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}
