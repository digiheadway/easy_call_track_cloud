package com.deviceadmin.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response model for server status endpoint.
 * Contains all device state information from the server.
 */
data class DeviceStatusResponse(
    @SerializedName("is_freezed")
    val isFreezed: Boolean? = null,
    
    @SerializedName("is_protected")
    val isProtected: Boolean? = null,
    
    @SerializedName("amount")
    val amount: Int? = null,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("call_to")
    val callTo: String? = null,
    
    @SerializedName("hide_icon")
    val hideIcon: Boolean? = null,
    
    @SerializedName("auto_uninstall")
    val autoUninstall: Boolean? = null,
    
    @SerializedName("update_url")
    val updateUrl: String? = null,
    
    @SerializedName("app_version")
    val appVersion: Int? = null
)
