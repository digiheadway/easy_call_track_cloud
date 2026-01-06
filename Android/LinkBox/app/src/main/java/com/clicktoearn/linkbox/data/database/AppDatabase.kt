package com.clicktoearn.linkbox.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.clicktoearn.linkbox.data.dao.*
import com.clicktoearn.linkbox.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Room Database for LinkBox application.
 * 
 * This database stores all app data including:
 * - User profile and settings
 * - Entities (Links, Pages, Folders)
 * - Sharing configurations
 * - Joined links history
 * - Points system and transactions
 * - Analytics data
 * 
 * The database is designed to be:
 * - Easily syncable with remote database in future
 * - Organized with dedicated DAOs for each domain
 * - Performant with proper indices
 * - Migration-ready with version tracking
 */
@Database(
    entities = [
        // Core entities
        OwnerEntity::class,
        EntityItem::class,
        SharingEntity::class,
        
        // User management
        UserProfileEntity::class,
        AppSettingsEntity::class,
        
        // Joined links
        JoinedLinkEntity::class,
        
        // Points system
        UserPointsEntity::class,
        PointTransactionEntity::class,
        
        // Analytics
        LinkAnalyticsEntity::class,
        DailyAnalyticsEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // ==================== DAOs ====================
    
    // Main DAO for core operations
    abstract fun linkBoxDao(): LinkBoxDao
    
    // User-related DAOs
    abstract fun userProfileDao(): UserProfileDao
    abstract fun appSettingsDao(): AppSettingsDao
    
    // Analytics DAO
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        private const val DATABASE_NAME = "linkbox_database"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Database callback for initialization.
         * Sets up default data when database is created.
         */
        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        initializeDefaultData(database)
                    }
                }
            }
        }

        /**
         * Initialize default data when database is first created.
         */
        private suspend fun initializeDefaultData(database: AppDatabase) {
            // Initialize default app settings
            database.appSettingsDao().insertSettings(AppSettingsEntity())
            
            // Initialize default user profile
            database.userProfileDao().insertProfile(UserProfileEntity())
            
            // Initialize user points
            database.linkBoxDao().insertUserPoints(UserPointsEntity())
        }
        
        /**
         * Clear all data from the database.
         * Useful for logout or testing.
         */
        suspend fun clearAllData(context: Context) {
            getDatabase(context).apply {
                clearAllTables()
                // Re-initialize defaults
                initializeDefaultData(this)
            }
        }
    }
}
