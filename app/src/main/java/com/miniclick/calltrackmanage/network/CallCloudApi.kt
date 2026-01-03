package com.miniclick.calltrackmanage.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface CallCloudApi {

    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun verifyPairingCode(
        @Field("action") action: String,
        @Field("org_id") orgId: String,
        @Field("user_id") userId: String,
        @Field("device_id") deviceId: String,
        @Field("device_model") deviceModel: String? = null,
        @Field("os_version") osVersion: String? = null,
        @Field("battery_level") batteryLevel: Int? = null
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun startCall(
        @Field("action") action: String,
        @Field("unique_id") uniqueId: String,
        @Field("org_id") orgId: String,
        @Field("user_id") userId: String,
        @Field("device_id") deviceId: String,
        @Field("device_phone") devicePhone: String,
        @Field("caller_name") callerName: String?,
        @Field("caller") caller: String,
        @Field("type") type: String,
        @Field("duration") duration: Int,
        @Field("call_time") callTime: String
    ): Response<Map<String, Any>>

    @Multipart
    @POST("sync_app.php")
    suspend fun uploadChunk(
        @Part("action") action: RequestBody,
        @Part("unique_id") uniqueId: RequestBody,
        @Part("chunk_index") chunkIndex: RequestBody,
        @Part chunk: MultipartBody.Part
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun finalizeUpload(
        @Field("action") action: String,
        @Field("unique_id") uniqueId: String,
        @Field("total_chunks") totalChunks: Int
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun updateNote(
        @Field("action") action: String,
        @Field("unique_id") uniqueId: String,
        @Field("note") note: String?,
        @Field("person_note") personNote: String?,
        @Field("label") label: String?
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun fetchContacts(
        @Field("action") action: String,
        @Field("org_id") orgId: String,
        @Field("user_id") userId: String,
        @Field("device_id") deviceId: String
    ): Response<Map<String, Any>>
    
    // NEW: Fetch updates since last sync time (delta sync)
    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun fetchUpdates(
        @Field("action") action: String,
        @Field("org_id") orgId: String,
        @Field("user_id") userId: String,
        @Field("device_id") deviceId: String,
        @Field("last_sync_time") lastSyncTime: Long
    ): Response<Map<String, Any>>
    
    // NEW: Update call metadata (reviewed, note, caller_name)
    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun updateCall(
        @Field("action") action: String,
        @Field("unique_id") uniqueId: String,
        @Field("reviewed") reviewed: Boolean?,
        @Field("note") note: String?,
        @Field("caller_name") callerName: String?,
        @Field("updated_at") updatedAt: Long
    ): Response<Map<String, Any>>
    
    // NEW: Update person metadata (personNote, label, name)
    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun updatePerson(
        @Field("action") action: String,
        @Field("phone") phone: String,
        @Field("org_id") orgId: String,
        @Field("person_note") personNote: String?,
        @Field("label") label: String?,
        @Field("name") name: String?,
        @Field("updated_at") updatedAt: Long
    ): Response<Map<String, Any>>

    // NEW: Fetch organizational config (Excluded contacts + Employee settings)
    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun fetchConfig(
        @Field("action") action: String,
        @Field("org_id") orgId: String,
        @Field("user_id") userId: String,
        @Field("os_version") osVersion: String? = null,
        @Field("battery_level") batteryLevel: Int? = null,
        @Field("device_model") deviceModel: String? = null
    ): Response<Map<String, Any>>
    @FormUrlEncoded
    @POST("sync_app.php")
    suspend fun checkRecordingsStatus(
        @Field("action") action: String,
        @Field("unique_ids") uniqueIdsJson: String
    ): Response<Map<String, Any>>

    @GET
    suspend fun fetchData(@Url url: String): Response<Map<String, Any>>
}
