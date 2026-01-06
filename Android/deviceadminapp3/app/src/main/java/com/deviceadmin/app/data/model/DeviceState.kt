package com.deviceadmin.app.data.model

/**
 * Local device state representation.
 * Used for storing and managing device state in SharedPreferences.
 */
data class DeviceState(
    val deviceId: String = "",
    val isSetupComplete: Boolean = false,
    val phoneState: PhoneState = PhoneState.ACTIVE,
    val protectionState: ProtectionState = ProtectionState.DISABLED,
    val isUninstallAllowed: Boolean = true,
    val emiAmount: Int = 0,
    val message: String? = null,
    val callToNumber: String? = null,
    val breakEndTime: Long = 0L
)

/**
 * Represents the lock state of the device.
 */
enum class PhoneState {
    ACTIVE,     // Device is unlocked and usable
    FROZEN;     // Device is locked by the server
    
    companion object {
        fun fromString(value: String?): PhoneState {
            return when (value?.lowercase()?.trim()) {
                "freeze", "frozen", "lock", "locked" -> FROZEN
                else -> ACTIVE
            }
        }
    }
}

/**
 * Represents the protection state of the device.
 */
enum class ProtectionState {
    ENABLED,    // Uninstall and settings access blocked
    DISABLED;   // Device can be uninstalled normally
    
    companion object {
        fun fromString(value: String?): ProtectionState {
            return when (value?.lowercase()?.trim()) {
                "enabled", "true", "1" -> ENABLED
                else -> DISABLED
            }
        }
        
        fun fromBoolean(value: Boolean?): ProtectionState {
            return if (value == true) ENABLED else DISABLED
        }
    }
}
