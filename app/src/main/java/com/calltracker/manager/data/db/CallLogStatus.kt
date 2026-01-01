package com.calltracker.manager.data.db

enum class CallLogStatus {
    PENDING,
    COMPRESSING,
    UPLOADING,
    COMPLETED,
    FAILED,
    NOTE_UPDATE_PENDING;

    companion object {
        fun fromString(value: String): CallLogStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}
