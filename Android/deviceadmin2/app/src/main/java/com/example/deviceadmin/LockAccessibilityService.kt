package com.example.deviceadmin

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class LockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!Utils.isProtected(this)) return

        val source = event.source ?: return
        
        // Scan for dangerous keywords in the window content
        if (scanForKeywords(source)) {
            blockAction()
        }
    }

    private fun scanForKeywords(node: AccessibilityNodeInfo): Boolean {
        val keywords = listOf("Uninstall", "Deactivate", "Force stop", "Clear data", "Remove admin")
        
        val text = node.text?.toString() ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""
        
        for (keyword in keywords) {
            if (text.contains(keyword, ignoreCase = true) || contentDescription.contains(keyword, ignoreCase = true)) {
                // Additional check to ensure we only block relevant settings for OUR app
                // Usually check packageName from event
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (scanForKeywords(child)) return true
        }
        
        return false
    }

    private fun blockAction() {
        // Show notification/toast
        Toast.makeText(this, "Blocked by Administrator", Toast.LENGTH_SHORT).show()
        
        // Go home to interrupt the action
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "Protection Service Connected", Toast.LENGTH_SHORT).show()
    }
}
