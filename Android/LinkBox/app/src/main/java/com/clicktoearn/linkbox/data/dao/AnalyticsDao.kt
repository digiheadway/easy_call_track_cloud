package com.clicktoearn.linkbox.data.dao

import androidx.room.*
import com.clicktoearn.linkbox.data.entity.AnalyticsEventType
import com.clicktoearn.linkbox.data.entity.LinkAnalyticsEntity
import com.clicktoearn.linkbox.data.entity.DailyAnalyticsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Link Analytics operations.
 * Handles detailed analytics tracking and aggregated reporting.
 */
@Dao
interface AnalyticsDao {
    
    // ==================== Link Analytics - Create ====================
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnalyticsEvent(event: LinkAnalyticsEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnalyticsEvents(events: List<LinkAnalyticsEntity>)
    
    // ==================== Link Analytics - Read ====================
    
    @Query("SELECT * FROM link_analytics WHERE sharing_id = :sharingId ORDER BY recorded_at DESC")
    fun getAnalyticsForLink(sharingId: Long): Flow<List<LinkAnalyticsEntity>>
    
    @Query("SELECT * FROM link_analytics WHERE sharing_id = :sharingId ORDER BY recorded_at DESC LIMIT :limit")
    fun getRecentAnalyticsForLink(sharingId: Long, limit: Int): Flow<List<LinkAnalyticsEntity>>
    
    @Query("SELECT * FROM link_analytics WHERE sharing_id = :sharingId AND event_type = :eventType ORDER BY recorded_at DESC")
    fun getAnalyticsByType(sharingId: Long, eventType: AnalyticsEventType): Flow<List<LinkAnalyticsEntity>>
    
    @Query("SELECT COUNT(*) FROM link_analytics WHERE sharing_id = :sharingId AND event_type = :eventType")
    suspend fun countEventsByType(sharingId: Long, eventType: AnalyticsEventType): Int
    
    @Query("SELECT * FROM link_analytics WHERE recorded_at >= :startTime AND recorded_at <= :endTime ORDER BY recorded_at DESC")
    fun getAnalyticsInTimeRange(startTime: Long, endTime: Long): Flow<List<LinkAnalyticsEntity>>
    
    @Query("SELECT * FROM link_analytics WHERE sharing_id = :sharingId AND recorded_at >= :startTime AND recorded_at <= :endTime ORDER BY recorded_at DESC")
    fun getAnalyticsForLinkInRange(sharingId: Long, startTime: Long, endTime: Long): Flow<List<LinkAnalyticsEntity>>
    
    // ==================== Link Analytics - Delete ====================
    
    @Query("DELETE FROM link_analytics WHERE sharing_id = :sharingId")
    suspend fun deleteAnalyticsForLink(sharingId: Long)
    
    @Query("DELETE FROM link_analytics WHERE recorded_at < :beforeTime")
    suspend fun deleteOldAnalytics(beforeTime: Long)
    
    // ==================== Daily Analytics - Create / Update ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyAnalytics(daily: DailyAnalyticsEntity)
    
    @Query("""
        INSERT OR REPLACE INTO daily_analytics 
        (id, sharing_id, date, clicks, views, unique_visits, downloads, top_referrer, updated_at)
        VALUES (
            (SELECT id FROM daily_analytics WHERE sharing_id = :sharingId AND date = :date),
            :sharingId,
            :date,
            COALESCE((SELECT clicks FROM daily_analytics WHERE sharing_id = :sharingId AND date = :date), 0) + :clicks,
            COALESCE((SELECT views FROM daily_analytics WHERE sharing_id = :sharingId AND date = :date), 0) + :views,
            COALESCE((SELECT unique_visits FROM daily_analytics WHERE sharing_id = :sharingId AND date = :date), 0) + :uniqueVisits,
            COALESCE((SELECT downloads FROM daily_analytics WHERE sharing_id = :sharingId AND date = :date), 0) + :downloads,
            :referrer,
            :timestamp
        )
    """)
    suspend fun incrementDailyAnalytics(
        sharingId: Long,
        date: Int,
        clicks: Int = 0,
        views: Int = 0,
        uniqueVisits: Int = 0,
        downloads: Int = 0,
        referrer: String? = null,
        timestamp: Long = System.currentTimeMillis()
    )
    
    // ==================== Daily Analytics - Read ====================
    
    @Query("SELECT * FROM daily_analytics WHERE sharing_id = :sharingId ORDER BY date DESC")
    fun getDailyAnalyticsForLink(sharingId: Long): Flow<List<DailyAnalyticsEntity>>
    
    @Query("SELECT * FROM daily_analytics WHERE sharing_id = :sharingId ORDER BY date DESC LIMIT :days")
    fun getRecentDailyAnalytics(sharingId: Long, days: Int): Flow<List<DailyAnalyticsEntity>>
    
    @Query("SELECT * FROM daily_analytics WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getDailyAnalyticsInRange(startDate: Int, endDate: Int): Flow<List<DailyAnalyticsEntity>>
    
    @Query("SELECT SUM(clicks) FROM daily_analytics WHERE sharing_id = :sharingId")
    suspend fun getTotalClicksForLink(sharingId: Long): Int?
    
    @Query("SELECT SUM(views) FROM daily_analytics WHERE sharing_id = :sharingId")
    suspend fun getTotalViewsForLink(sharingId: Long): Int?
    
    // ==================== Daily Analytics - Delete ====================
    
    @Query("DELETE FROM daily_analytics WHERE sharing_id = :sharingId")
    suspend fun deleteDailyAnalyticsForLink(sharingId: Long)
    
    @Query("DELETE FROM daily_analytics WHERE date < :beforeDate")
    suspend fun deleteOldDailyAnalytics(beforeDate: Int)
}
