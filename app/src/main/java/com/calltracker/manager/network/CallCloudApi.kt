package com.calltracker.manager.network

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
        @Field("device_id") deviceId: String
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
        @Field("duration") duration: Int
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
        @Field("person_note") personNote: String?
    ): Response<Map<String, Any>>
}
