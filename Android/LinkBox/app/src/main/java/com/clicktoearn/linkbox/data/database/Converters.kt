package com.clicktoearn.linkbox.data.database

import androidx.room.TypeConverter
import com.clicktoearn.linkbox.data.entity.AnalyticsEventType
import com.clicktoearn.linkbox.data.entity.EntityType
import com.clicktoearn.linkbox.data.entity.PointTransactionType
import com.clicktoearn.linkbox.data.entity.PrivacyType
import java.util.Date

/**
 * Type converters for Room database.
 * Handles conversion between Kotlin types and SQLite-compatible types.
 */
class Converters {
    
    // ==================== Enum Converters ====================
    
    @TypeConverter
    fun fromEntityType(value: EntityType): String = value.name

    @TypeConverter
    fun toEntityType(value: String): EntityType = EntityType.valueOf(value)

    @TypeConverter
    fun fromPrivacyType(value: PrivacyType): String = value.name

    @TypeConverter
    fun toPrivacyType(value: String): PrivacyType = PrivacyType.valueOf(value)

    @TypeConverter
    fun fromPointTransactionType(value: PointTransactionType): String = value.name

    @TypeConverter
    fun toPointTransactionType(value: String): PointTransactionType = PointTransactionType.valueOf(value)
    
    @TypeConverter
    fun fromAnalyticsEventType(value: AnalyticsEventType): String = value.name
    
    @TypeConverter
    fun toAnalyticsEventType(value: String): AnalyticsEventType = AnalyticsEventType.valueOf(value)
    
    // ==================== Date Converters ====================
    
    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }
    
    // ==================== List Converters (for future use) ====================
    
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString(separator = "|||")
    }

    @TypeConverter
    fun toStringList(data: String?): List<String>? {
        return data?.split("|||")?.filter { it.isNotEmpty() }
    }
}
