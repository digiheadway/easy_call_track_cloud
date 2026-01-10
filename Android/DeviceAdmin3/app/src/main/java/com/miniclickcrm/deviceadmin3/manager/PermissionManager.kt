package com.miniclickcrm.deviceadmin3.manager

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.miniclickcrm.deviceadmin3.receiver.MyAdminReceiver

data class PermissionItem(
    val id: String,
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val isRequired: Boolean = true
)

class PermissionManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, MyAdminReceiver::class.java)

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 1001
        const val REQUEST_CODE_DEVICE_ADMIN = 1002
        const val REQUEST_CODE_OVERLAY = 1003
        const val REQUEST_CODE_BATTERY_OPTIMIZATION = 1004
        const val REQUEST_CODE_ACCESSIBILITY = 1005
        
        val RUNTIME_PERMISSIONS = arrayOf(

            Manifest.permission.RECEIVE_SMS,

            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )
        
        val RUNTIME_PERMISSIONS_13 = arrayOf(
            Manifest.permission.POST_NOTIFICATIONS
        )
        

    }

    fun getAllPermissionItems(): List<PermissionItem> {
        val items = mutableListOf<PermissionItem>()
        
        // Device Admin
        items.add(
            PermissionItem(
                id = "device_admin",
                name = "Device Admin",
                description = "Required for device lock and security features",
                isGranted = isDeviceAdminActive(),
                isRequired = true
            )
        )
        

        
        // SMS
        items.add(
            PermissionItem(
                id = "sms",
                name = "SMS Access",
                description = "Required for unlock codes via SMS",
                isGranted = hasSmsPermission(),
                isRequired = true
            )
        )
        
        // Phone State
        items.add(
            PermissionItem(
                id = "phone_state",
                name = "Phone State",
                description = "Required for device identification",
                isGranted = hasPhoneStatePermission(),
                isRequired = true
            )
        )
        
        // Overlay
        items.add(
            PermissionItem(
                id = "overlay",
                name = "Display Over Apps",
                description = "Required for lock screen overlay",
                isGranted = hasOverlayPermission(),
                isRequired = true
            )
        )
        
        // Battery Optimization
        items.add(
            PermissionItem(
                id = "battery",
                name = "Battery Optimization",
                description = "Required for background service to run",
                isGranted = isIgnoringBatteryOptimizations(),
                isRequired = true
            )
        )
        
        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            items.add(
                PermissionItem(
                    id = "notifications",
                    name = "Notifications",
                    description = "Required for status notifications",
                    isGranted = hasNotificationPermission(),
                    isRequired = true
                )
            )
        }
        
        // Accessibility
        items.add(
            PermissionItem(
                id = "accessibility",
                name = "Accessibility Service",
                description = "Required to prevent device bypass",
                isGranted = isAccessibilityServiceEnabled(),
                isRequired = true
            )
        )
        
        return items
    }

    fun areAllPermissionsGranted(): Boolean {
        return getAllPermissionItems().filter { it.isRequired }.all { it.isGranted }
    }

    fun getGrantedPermissionsCount(): Pair<Int, Int> {
        val items = getAllPermissionItems().filter { it.isRequired }
        return Pair(items.count { it.isGranted }, items.size)
    }

    // Individual Permission Checks
    fun isDeviceAdminActive(): Boolean = dpm.isAdminActive(adminComponent)
    
    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)



    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(context, "com.miniclickcrm.deviceadmin3.service.MyAccessibilityService")
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabledServices.contains(expectedComponentName.flattenToString())
    }

    // Request Permissions
    fun requestDeviceAdmin(activity: Activity) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for device protection and lock features.")
        }
        activity.startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN)
    }

    fun requestRuntimePermissions(activity: Activity) {
        val permissionsToRequest = mutableListOf<String>()
        
        RUNTIME_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RUNTIME_PERMISSIONS_13.forEach { permission ->
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }



    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY)
    }

    fun requestBatteryOptimizationException(activity: Activity) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:${context.packageName}")
        activity.startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION)
    }

    fun requestAccessibilityPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.startActivityForResult(intent, REQUEST_CODE_ACCESSIBILITY)
    }

    fun requestPermissionById(activity: Activity, permissionId: String) {
        when (permissionId) {
            "device_admin" -> requestDeviceAdmin(activity)

            "sms" -> requestRuntimePermissions(activity)
            "phone_state" -> requestRuntimePermissions(activity)
            "overlay" -> requestOverlayPermission(activity)
            "battery" -> requestBatteryOptimizationException(activity)
            "notifications" -> requestRuntimePermissions(activity)
            "accessibility" -> requestAccessibilityPermission(activity)
        }
    }
}
