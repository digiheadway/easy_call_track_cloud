package com.miniclickcrm.deviceadmin3.api

import retrofit2.http.GET
import retrofit2.http.Query

data class DeviceData(
    val amount: Int,
    val message: String,
    val is_freezed: Boolean,
    val call_to: String,
    val is_protected: Boolean,
    val unlock_codes: List<String>? = null
)

data class DeviceStatusResponse(
    val success: Boolean,
    val pairingcode: String?,
    val data: DeviceData?,
    val error: String?
)

interface ApiService {
    @GET("check.php")
    suspend fun checkStatus(
        @Query("pairingcode") pairingCode: String,
        @Query("fcm_token") fcmToken: String? = null,
        @Query("imei") imei: String? = null,
        @Query("imei2") imei2: String? = null,
        @Query("device_name") deviceName: String? = null,
        @Query("device_model") deviceModel: String? = null
    ): DeviceStatusResponse
}
