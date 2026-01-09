package com.miniclick.calltrackmanage.util.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * DevicePermissionGuide - Provides device-specific instructions for battery optimization,
 * auto-start permissions, and background restrictions.
 * 
 * Based on Callyzer Pro's approach of tailored instructions per manufacturer.
 */
object DevicePermissionGuide {

    private const val TAG = "DevicePermissionGuide"

    /**
     * Device manufacturer detection
     */
    val manufacturer: String = Build.MANUFACTURER.lowercase()
    val model: String = Build.MODEL
    val brand: String = Build.BRAND.lowercase()

    /**
     * Check which manufacturer this device belongs to
     */
    enum class DeviceType {
        XIAOMI,     // Xiaomi, Redmi, POCO
        OPPO,       // Oppo, Realme, OnePlus (ColorOS)
        VIVO,       // Vivo, iQOO
        HUAWEI,     // Huawei, Honor
        SAMSUNG,    // Samsung
        ASUS,       // ASUS ROG, Zenfone
        LENOVO,     // Lenovo, Motorola
        GENERIC     // Other/Stock Android
    }

    fun getDeviceType(): DeviceType {
        return when {
            manufacturer.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> DeviceType.XIAOMI
            manufacturer.contains("oppo") || brand.contains("realme") || brand.contains("oneplus") -> DeviceType.OPPO
            manufacturer.contains("vivo") || brand.contains("iqoo") -> DeviceType.VIVO
            manufacturer.contains("huawei") || brand.contains("honor") -> DeviceType.HUAWEI
            manufacturer.contains("samsung") -> DeviceType.SAMSUNG
            manufacturer.contains("asus") -> DeviceType.ASUS
            manufacturer.contains("lenovo") || manufacturer.contains("motorola") -> DeviceType.LENOVO
            else -> DeviceType.GENERIC
        }
    }

    /**
     * Get human-readable device name
     */
    fun getDeviceName(): String {
        return when (getDeviceType()) {
            DeviceType.XIAOMI -> "Xiaomi/Redmi/POCO"
            DeviceType.OPPO -> "OPPO/Realme/OnePlus"
            DeviceType.VIVO -> "Vivo/iQOO"
            DeviceType.HUAWEI -> "Huawei/Honor"
            DeviceType.SAMSUNG -> "Samsung"
            DeviceType.ASUS -> "ASUS"
            DeviceType.LENOVO -> "Lenovo/Motorola"
            DeviceType.GENERIC -> Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Get device-specific permission steps
     */
    fun getPermissionSteps(): List<PermissionStep> {
        return when (getDeviceType()) {
            DeviceType.XIAOMI -> getXiaomiSteps()
            DeviceType.OPPO -> getOppoSteps()
            DeviceType.VIVO -> getVivoSteps()
            DeviceType.HUAWEI -> getHuaweiSteps()
            DeviceType.SAMSUNG -> getSamsungSteps()
            DeviceType.ASUS -> getAsusSteps()
            DeviceType.LENOVO -> getLenovoSteps()
            DeviceType.GENERIC -> getGenericSteps()
        }
    }

    data class PermissionStep(
        val title: String,
        val description: String,
        val actionIntent: Intent? = null,
        val isOptional: Boolean = false
    )

    // ============== Device-Specific Steps ==============

    private fun getXiaomiSteps(): List<PermissionStep> = listOf(
        PermissionStep(
            title = "1. Enable Autostart",
            description = "Go to Settings → Apps → Manage apps → CallCloud → Autostart → Enable",
            actionIntent = try {
                Intent().apply {
                    setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
            } catch (e: Exception) { null }
        ),
        PermissionStep(
            title = "2. Disable Battery Saver",
            description = "Go to Settings → Apps → Manage apps → CallCloud → Battery saver → No restrictions",
            actionIntent = getBatterySettingsIntent()
        ),
        PermissionStep(
            title = "3. Lock App in Recent",
            description = "Open CallCloud, then in Recent Apps, tap the lock icon on the app card to prevent it from being killed",
            isOptional = true
        ),
        PermissionStep(
            title = "4. Disable MIUI Optimization (Advanced)",
            description = "Settings → Additional Settings → Developer options → Turn off 'MIUI optimization'",
            isOptional = true
        )
    )

    private fun getOppoSteps(): List<PermissionStep> = listOf(
        PermissionStep(
            title = "1. Enable Auto-Launch",
            description = "Go to Settings → App Management → App list → CallCloud → Auto-launch → Enable",
            actionIntent = try {
                Intent().apply {
                    setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                }
            } catch (e: Exception) { null }
        ),
        PermissionStep(
            title = "2. Disable Battery Optimization",
            description = "Settings → Battery → More settings → Optimize battery use → CallCloud → Don't optimize",
            actionIntent = getBatterySettingsIntent()
        ),
        PermissionStep(
            title = "3. Allow Background Activity",
            description = "Settings → App Management → App list → CallCloud → Power consumption → Allow background activity",
            isOptional = false
        ),
        PermissionStep(
            title = "4. Lock in Recent Apps",
            description = "Swipe down from the top of the app card in Recent Apps to lock it",
            isOptional = true
        )
    )

    private fun getVivoSteps(): List<PermissionStep> = listOf(
        PermissionStep(
            title = "1. Enable High Background Power Consumption",
            description = "Settings → Battery → High background power consumption → Enable for CallCloud",
            actionIntent = getBatterySettingsIntent()
        ),
        PermissionStep(
            title = "2. Enable Auto-Start",
            description = "Settings → More settings → Applications → Autostart → Enable CallCloud",
            actionIntent = try {
                Intent().apply {
                    setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                }
            } catch (e: Exception) { null }
        ),
        PermissionStep(
            title = "3. Whitelist in Battery Manager",
            description = "iManager → App manager → Autostart manager → Allow for CallCloud"
        )
    )

    private fun getHuaweiSteps(): List<PermissionStep> = listOf(
        PermissionStep(
            title = "1. Enable Auto-Launch",
            description = "Settings → Apps → Startup manager → CallCloud → Enable Manual manage and enable all toggles",
            actionIntent = try {
                Intent().apply {
                    setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                }
            } catch (e: Exception) { null }
        ),
        PermissionStep(
            title = "2. Disable Battery Optimization",
            description = "Settings → Apps → Apps → CallCloud → Battery → Launch → Manage manually → Enable all",
            actionIntent = getBatterySettingsIntent()
        ),
        PermissionStep(
            title = "3. Ignore Battery Optimization",
            description = "Settings → Battery → App launch → CallCloud → Disable automatic management",
            isOptional = false
        ),
        PermissionStep(
            title = "4. Lock App",
            description = "Open CallCloud, go to Recent Apps, swipe down on the app to lock it",
            isOptional = true
        )
    )

    private fun getSamsungSteps(): List<PermissionStep> = listOf(
        PermissionStep(
            title = "1. Disable Battery Optimization",
            description = "Settings → Apps → CallCloud → Battery → Unrestricted",
            actionIntent = getBatterySettingsIntent()
        ),
        PermissionStep(
            title = "2. Allow Background Usage Limits",
            description = "Settings → Device care → Battery → Background usage limits → Never sleeping apps → Add CallCloud",
            isOptional = false
        ),
        PermissionStep(
            title = "3. Disable Adaptive Battery",
            description = "Settings → Device care → Battery → More battery settings → Adaptive battery → Disable",
            isOptional = true
        )
    )

    private fun getAsusSteps(): List<PermissionStep> = listOf(
        PermissionStep(
            title = "1. Enable Auto-Start",
            description = "Settings → Apps & notifications → Special app access → Power manager → CallCloud → No restrictions",
            actionIntent = getBatterySettingsIntent()
        ),
        PermissionStep(
            title = "2. Disable Battery Optimization",
            description = "Settings → Battery → Battery optimization → CallCloud → Don't optimize"
        )
    )

    private fun getLenovoSteps(): List<PermissionStep> = listOf(
        PermissionStep(
            title = "1. Disable Battery Optimization",
            description = "Settings → Battery → Battery optimization → CallCloud → Don't optimize",
            actionIntent = getBatterySettingsIntent()
        ),
        PermissionStep(
            title = "2. Allow Background Activity",
            description = "Settings → Apps & notifications → CallCloud → Battery → Background activity → Allow"
        )
    )

    private fun getGenericSteps(): List<PermissionStep> = listOf(
        PermissionStep(
            title = "1. Disable Battery Optimization",
            description = "Settings → Apps → CallCloud → Battery → Unrestricted (or Don't optimize)",
            actionIntent = getBatterySettingsIntent()
        ),
        PermissionStep(
            title = "2. Check Background Restrictions",
            description = "Settings → Apps → CallCloud → Battery → Background activity → Allow",
            isOptional = false
        )
    )

    // ============== Intent Helpers ==============

    private fun getBatterySettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    /**
     * Open app settings page
     */
    fun getAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Try to open manufacturer-specific auto-start settings
     * Returns null if not available
     */
    fun getAutoStartIntent(): Intent? {
        return when (getDeviceType()) {
            DeviceType.XIAOMI -> try {
                Intent().apply {
                    setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
            } catch (e: Exception) { null }
            DeviceType.OPPO -> try {
                Intent().apply {
                    setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                }
            } catch (e: Exception) { null }
            DeviceType.VIVO -> try {
                Intent().apply {
                    setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                }
            } catch (e: Exception) { null }
            DeviceType.HUAWEI -> try {
                Intent().apply {
                    setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                }
            } catch (e: Exception) { null }
            else -> null
        }
    }

    /**
     * Check if device needs special permission handling
     */
    fun needsSpecialHandling(): Boolean {
        return getDeviceType() != DeviceType.GENERIC
    }

    /**
     * Log device info for debugging
     */
    fun logDeviceInfo() {
        Log.d(TAG, "Manufacturer: $manufacturer")
        Log.d(TAG, "Model: $model")
        Log.d(TAG, "Brand: $brand")
        Log.d(TAG, "Device Type: ${getDeviceType()}")
        Log.d(TAG, "Needs Special Handling: ${needsSpecialHandling()}")
    }
}
