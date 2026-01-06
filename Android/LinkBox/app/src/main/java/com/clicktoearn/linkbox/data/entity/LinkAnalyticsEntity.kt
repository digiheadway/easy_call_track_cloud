package com.clicktoearn.linkbox.data.entity

import androidx.room.*

/**
 * Represents detailed analytics for each sharing link.
 * This provides granular tracking of link performance over time.
 */
@Entity(
    tableName = "link_analytics",
    foreignKeys = [
        ForeignKey(
            entity = SharingEntity::class,
            parentColumns = ["id"],
            childColumns = ["sharing_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sharing_id"),
        Index("recorded_at")
    ]
)
data class LinkAnalyticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "sharing_id")
    val sharingId: Long,
    
    // Event type: CLICK, VIEW, DOWNLOAD, UNIQUE_VISIT
    @ColumnInfo(name = "event_type")
    val eventType: AnalyticsEventType,
    
    // Geographic info (for future use)
    val country: String? = null,
    val city: String? = null,
    
    // Device info
    val platform: String? = null, // Android, iOS, Web, etc.
    val browser: String? = null,
    
    // Referrer
    val referrer: String? = null,
    
    // User agent (for unique visit tracking)
    @ColumnInfo(name = "visitor_hash")
    val visitorHash: String? = null,
    
    // Timestamp
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long = System.currentTimeMillis()
)

enum class AnalyticsEventType {
    CLICK,
    VIEW,
    DOWNLOAD,
    UNIQUE_VISIT
}

/**
 * Daily aggregated analytics for reporting
 */
@Entity(
    tableName = "daily_analytics",
    foreignKeys = [
        ForeignKey(
            entity = SharingEntity::class,
            parentColumns = ["id"],
            childColumns = ["sharing_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sharing_id"),
        Index("date")
    ]
)
data class DailyAnalyticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "sharing_id")
    val sharingId: Long,
    
    // Date in YYYYMMDD format for easy querying
    val date: Int,
    
    // Aggregated counts
    val clicks: Int = 0,
    val views: Int = 0,
    
    @ColumnInfo(name = "unique_visits")
    val uniqueVisits: Int = 0,
    
    val downloads: Int = 0,
    
    // Top referrer for the day
    @ColumnInfo(name = "top_referrer")
    val topReferrer: String? = null,
    
    // Timestamp of last update
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
