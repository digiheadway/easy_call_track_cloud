package com.calltracker.manager.network

import com.google.gson.annotations.SerializedName

data class PersonUpdateDto(
    @SerializedName("phone") val phone: String,
    @SerializedName("person_note") val personNote: String?,
    @SerializedName("label") val label: String?
)

data class CallUpdateDto(
    @SerializedName("unique_id") val uniqueId: String,
    @SerializedName("note") val note: String
)
