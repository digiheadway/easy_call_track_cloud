package com.miniclick.calltrackmanage.network

import com.google.gson.annotations.SerializedName

/**
 * Generic API Response wrapper to standardize communication between App and Server.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: T? = null
)

data class PairingResponse(
    @SerializedName("employee_name") val employeeName: String,
    @SerializedName("settings") val settings: EmployeeSettingsDto,
    @SerializedName("plan") val plan: PlanInfoDto
)

data class EmployeeSettingsDto(
    @SerializedName("allow_personal_exclusion") val allowPersonalExclusion: Int,
    @SerializedName("allow_changing_tracking_start_date") val allowChangingTrackingStartDate: Int,
    @SerializedName("allow_updating_tracking_sims") val allowUpdatingTrackingSims: Int,
    @SerializedName("default_tracking_starting_date") val defaultTrackingStartingDate: String?,
    @SerializedName("call_track") val callTrack: Int,
    @SerializedName("call_record_crm") val callRecordCrm: Int
)

data class PlanInfoDto(
    @SerializedName("expiry_date") val expiryDate: String?,
    @SerializedName("allowed_storage_gb") val allowedStorageGb: Float,
    @SerializedName("storage_used_bytes") val storageUsedBytes: Long
)

data class PersonUpdateDto(
    @SerializedName("phone") val phone: String,
    @SerializedName("name") val name: String?,
    @SerializedName("person_note") val personNote: String?,
    @SerializedName("label") val label: String?
)

data class CallUpdateDto(
    @SerializedName("unique_id") val uniqueId: String,
    @SerializedName("note") val note: String
)

data class SyncResponse(
    @SerializedName("person_updates") val personUpdates: List<PersonUpdateDto>?,
    @SerializedName("call_updates") val callUpdates: List<CallUpdateDto>?,
    @SerializedName("server_time") val serverTime: Long
)

data class ConfigResponse(
    @SerializedName("excluded_contacts") val excludedContacts: List<String>?,
    @SerializedName("settings") val settings: EmployeeSettingsDto,
    @SerializedName("plan") val plan: PlanInfoDto,
    @SerializedName("force_upload_over_mobile") val forceUploadOverMobile: Int? = null
)

data class StartCallResponse(
    @SerializedName("unique_id") val uniqueId: String,
    @SerializedName("upload_status") val uploadStatus: String,
    @SerializedName("created_ts") val createdTs: String
)

data class BatchSyncResponse(
    @SerializedName("synced_ids") val syncedIds: List<String>,
    @SerializedName("recording_statuses") val recordingStatuses: Map<String, String>?, // unique_id -> server_status
    @SerializedName("server_time") val serverTime: Long
)

// Generic response for updates (call, person) that return server time
data class ServerTimeResponse(
    @SerializedName("server_time") val serverTime: Long
)

data class CompletedRecordingsResponse(
    @SerializedName("completed_ids") val completedIds: List<String>
)

data class ChunkUploadResponse(
    @SerializedName("chunk_saved") val chunkSaved: Int
)

data class FinalizeUploadResponse(
    @SerializedName("recording_url") val recordingUrl: String?
)
