package com.clicktoearn.linkbox.data.repository

import com.clicktoearn.linkbox.data.dao.AnalyticsDao
import com.clicktoearn.linkbox.data.entity.AnalyticsEventType
import com.clicktoearn.linkbox.data.entity.DailyAnalyticsEntity
import com.clicktoearn.linkbox.data.entity.LinkAnalyticsEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for Analytics operations.
 * Handles detailed link analytics tracking and aggregated reporting.
 * 
 * Provides:
 * - Detailed event logging
 * - Daily aggregated analytics
 * - Time-based queries
 * - Cleanup operations
 */
class AnalyticsRepository(private val dao: AnalyticsDao) {
    
    companion object {
        private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        
        /**
         * Get today's date as integer (YYYYMMDD format).
         */
        fun getTodayDate(): Int = dateFormat.format(Date()).toInt()
        
        /**
         * Convert timestamp to date integer.
         */
        fun getDateFromTimestamp(timestamp: Long): Int = dateFormat.format(Date(timestamp)).toInt()
    }
    
    // ==================== EVENT LOGGING ====================
    
    /**
     * Log a click event.
     */
    suspend fun logClick(
        sharingId: Long,
        country: String? = null,
        platform: String? = null,
        referrer: String? = null,
        visitorHash: String? = null
    ) {
        logEvent(
            sharingId = sharingId,
            eventType = AnalyticsEventType.CLICK,
            country = country,
            platform = platform,
            referrer = referrer,
            visitorHash = visitorHash
        )
        updateDailyClickCount(sharingId)
    }
    
    /**
     * Log a view event.
     */
    suspend fun logView(
        sharingId: Long,
        country: String? = null,
        platform: String? = null,
        referrer: String? = null,
        visitorHash: String? = null
    ) {
        logEvent(
            sharingId = sharingId,
            eventType = AnalyticsEventType.VIEW,
            country = country,
            platform = platform,
            referrer = referrer,
            visitorHash = visitorHash
        )
        updateDailyViewCount(sharingId)
    }
    
    /**
     * Log a download event.
     */
    suspend fun logDownload(
        sharingId: Long,
        country: String? = null,
        platform: String? = null,
        referrer: String? = null
    ) {
        logEvent(
            sharingId = sharingId,
            eventType = AnalyticsEventType.DOWNLOAD,
            country = country,
            platform = platform,
            referrer = referrer
        )
        updateDailyDownloadCount(sharingId)
    }
    
    /**
     * Log a unique visit event.
     */
    suspend fun logUniqueVisit(
        sharingId: Long,
        visitorHash: String,
        country: String? = null,
        platform: String? = null,
        referrer: String? = null
    ) {
        logEvent(
            sharingId = sharingId,
            eventType = AnalyticsEventType.UNIQUE_VISIT,
            country = country,
            platform = platform,
            referrer = referrer,
            visitorHash = visitorHash
        )
        updateDailyUniqueVisitCount(sharingId)
    }
    
    /**
     * Generic event logging.
     */
    private suspend fun logEvent(
        sharingId: Long,
        eventType: AnalyticsEventType,
        country: String? = null,
        city: String? = null,
        platform: String? = null,
        browser: String? = null,
        referrer: String? = null,
        visitorHash: String? = null
    ) {
        dao.insertAnalyticsEvent(
            LinkAnalyticsEntity(
                sharingId = sharingId,
                eventType = eventType,
                country = country,
                city = city,
                platform = platform,
                browser = browser,
                referrer = referrer,
                visitorHash = visitorHash
            )
        )
    }
    
    // ==================== DAILY ANALYTICS UPDATES ====================
    
    private suspend fun updateDailyClickCount(sharingId: Long, referrer: String? = null) {
        dao.incrementDailyAnalytics(
            sharingId = sharingId,
            date = getTodayDate(),
            clicks = 1,
            referrer = referrer
        )
    }
    
    private suspend fun updateDailyViewCount(sharingId: Long) {
        dao.incrementDailyAnalytics(
            sharingId = sharingId,
            date = getTodayDate(),
            views = 1
        )
    }
    
    private suspend fun updateDailyDownloadCount(sharingId: Long) {
        dao.incrementDailyAnalytics(
            sharingId = sharingId,
            date = getTodayDate(),
            downloads = 1
        )
    }
    
    private suspend fun updateDailyUniqueVisitCount(sharingId: Long) {
        dao.incrementDailyAnalytics(
            sharingId = sharingId,
            date = getTodayDate(),
            uniqueVisits = 1
        )
    }
    
    // ==================== READ OPERATIONS ====================
    
    /**
     * Get all analytics events for a link.
     */
    fun getAnalyticsForLink(sharingId: Long): Flow<List<LinkAnalyticsEntity>> = 
        dao.getAnalyticsForLink(sharingId)
    
    /**
     * Get recent analytics events for a link.
     */
    fun getRecentAnalyticsForLink(sharingId: Long, limit: Int = 100): Flow<List<LinkAnalyticsEntity>> = 
        dao.getRecentAnalyticsForLink(sharingId, limit)
    
    /**
     * Get analytics for a specific event type.
     */
    fun getAnalyticsByType(sharingId: Long, eventType: AnalyticsEventType): Flow<List<LinkAnalyticsEntity>> = 
        dao.getAnalyticsByType(sharingId, eventType)
    
    /**
     * Count events by type.
     */
    suspend fun countEventsByType(sharingId: Long, eventType: AnalyticsEventType): Int = 
        dao.countEventsByType(sharingId, eventType)
    
    /**
     * Get analytics in a time range.
     */
    fun getAnalyticsInTimeRange(startTime: Long, endTime: Long): Flow<List<LinkAnalyticsEntity>> = 
        dao.getAnalyticsInTimeRange(startTime, endTime)
    
    /**
     * Get analytics for a link in a time range.
     */
    fun getAnalyticsForLinkInRange(sharingId: Long, startTime: Long, endTime: Long): Flow<List<LinkAnalyticsEntity>> = 
        dao.getAnalyticsForLinkInRange(sharingId, startTime, endTime)
    
    // ==================== DAILY ANALYTICS READ ====================
    
    /**
     * Get daily aggregated analytics for a link.
     */
    fun getDailyAnalyticsForLink(sharingId: Long): Flow<List<DailyAnalyticsEntity>> = 
        dao.getDailyAnalyticsForLink(sharingId)
    
    /**
     * Get recent daily analytics.
     */
    fun getRecentDailyAnalytics(sharingId: Long, days: Int = 30): Flow<List<DailyAnalyticsEntity>> = 
        dao.getRecentDailyAnalytics(sharingId, days)
    
    /**
     * Get daily analytics in a date range.
     */
    fun getDailyAnalyticsInRange(startDate: Int, endDate: Int): Flow<List<DailyAnalyticsEntity>> = 
        dao.getDailyAnalyticsInRange(startDate, endDate)
    
    /**
     * Get total clicks for a link.
     */
    suspend fun getTotalClicksForLink(sharingId: Long): Int = 
        dao.getTotalClicksForLink(sharingId) ?: 0
    
    /**
     * Get total views for a link.
     */
    suspend fun getTotalViewsForLink(sharingId: Long): Int = 
        dao.getTotalViewsForLink(sharingId) ?: 0
    
    // ==================== CLEANUP OPERATIONS ====================
    
    /**
     * Delete analytics for a specific link.
     */
    suspend fun deleteAnalyticsForLink(sharingId: Long) {
        dao.deleteAnalyticsForLink(sharingId)
        dao.deleteDailyAnalyticsForLink(sharingId)
    }
    
    /**
     * Delete old analytics events (older than specified days).
     */
    suspend fun cleanupOldAnalytics(daysToKeep: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep.toLong() * 24 * 60 * 60 * 1000)
        dao.deleteOldAnalytics(cutoffTime)
        
        // Also cleanup old daily analytics
        val cutoffDate = getDateFromTimestamp(cutoffTime)
        dao.deleteOldDailyAnalytics(cutoffDate)
    }
    
    // ==================== AGGREGATION HELPERS ====================
    
    /**
     * Get last N days as date integers (for querying).
     */
    fun getLastNDays(n: Int): List<Int> {
        val calendar = Calendar.getInstance()
        return (0 until n).map { daysAgo ->
            calendar.timeInMillis = System.currentTimeMillis() - (daysAgo.toLong() * 24 * 60 * 60 * 1000)
            dateFormat.format(calendar.time).toInt()
        }
    }
    
    /**
     * Calculate analytics summary for a link.
     */
    suspend fun calculateLinkSummary(sharingId: Long): AnalyticsSummary {
        return AnalyticsSummary(
            totalClicks = countEventsByType(sharingId, AnalyticsEventType.CLICK),
            totalViews = countEventsByType(sharingId, AnalyticsEventType.VIEW),
            totalDownloads = countEventsByType(sharingId, AnalyticsEventType.DOWNLOAD),
            totalUniqueVisits = countEventsByType(sharingId, AnalyticsEventType.UNIQUE_VISIT)
        )
    }
}

/**
 * Data class for analytics summary.
 */
data class AnalyticsSummary(
    val totalClicks: Int,
    val totalViews: Int,
    val totalDownloads: Int,
    val totalUniqueVisits: Int
) {
    val totalInteractions: Int get() = totalClicks + totalViews + totalDownloads
}
