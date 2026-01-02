package com.calltracker.manager.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: CallLogStatus): String = status.name

    @TypeConverter
    fun toStatus(status: String): CallLogStatus = CallLogStatus.fromString(status)

    @TypeConverter
    fun fromMetadataStatus(status: MetadataSyncStatus): String = status.name

    @TypeConverter
    fun toMetadataStatus(status: String): MetadataSyncStatus = MetadataSyncStatus.fromString(status)

    @TypeConverter
    fun fromRecordingStatus(status: RecordingSyncStatus): String = status.name

    @TypeConverter
    fun toRecordingStatus(status: String): RecordingSyncStatus = RecordingSyncStatus.fromString(status)
}
