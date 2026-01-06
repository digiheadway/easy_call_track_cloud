package com.deviceadmin.app.data.remote

import com.deviceadmin.app.data.model.DeviceStatusResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service interface for server communication.
 */
interface DeviceApiService {

    companion object {
        const val BASE_URL = "https://api.miniclickcrm.com/"
    }

    /**
     * Fetches the current device status from the server.
     * @param deviceId Unique identifier for the device
     * @return Device status response containing lock state, protection state, etc.
     */
    @GET("admin/status.php")
    suspend fun getDeviceStatus(
        @Query("deviceid") deviceId: String
    ): Response<DeviceStatusResponse>
}
