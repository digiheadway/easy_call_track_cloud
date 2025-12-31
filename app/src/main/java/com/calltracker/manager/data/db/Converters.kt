package com.calltracker.manager.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: CallLogStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(status: String): CallLogStatus {
        return CallLogStatus.fromString(status)
    }
}
