package com.calltracker.manager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CallDataEntity::class,
        PersonDataEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun callDataDao(): CallDataDao
    abstract fun personDataDao(): PersonDataDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 (old call_log_status table) to version 2 (new schema)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new call_data table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS call_data (
                        compositeId TEXT NOT NULL PRIMARY KEY,
                        systemId TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        contactName TEXT,
                        callType INTEGER NOT NULL,
                        callDate INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        photoUri TEXT,
                        subscriptionId INTEGER,
                        deviceId TEXT,
                        callNote TEXT,
                        localRecordingPath TEXT,
                        syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create new person_data table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS person_data (
                        phoneNumber TEXT NOT NULL PRIMARY KEY,
                        contactName TEXT,
                        photoUri TEXT,
                        personNote TEXT,
                        lastCallType INTEGER,
                        lastCallDuration INTEGER,
                        lastCallDate INTEGER,
                        lastRecordingPath TEXT,
                        lastCallCompositeId TEXT,
                        totalCalls INTEGER NOT NULL DEFAULT 0,
                        totalIncoming INTEGER NOT NULL DEFAULT 0,
                        totalOutgoing INTEGER NOT NULL DEFAULT 0,
                        totalMissed INTEGER NOT NULL DEFAULT 0,
                        totalDuration INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Migrate existing data from old table if it exists
                try {
                    database.execSQL("""
                        INSERT INTO call_data (
                            compositeId, systemId, phoneNumber, callType, callDate, duration,
                            callNote, localRecordingPath, syncStatus, createdAt, updatedAt
                        )
                        SELECT 
                            compositeId, compositeId, '', 0, lastUpdated, 0,
                            callNote, localRecordingPath, status, lastUpdated, lastUpdated
                        FROM call_log_status
                    """.trimIndent())
                } catch (e: Exception) {
                    // Old table might not exist, ignore
                }
                
                // Drop old table
                database.execSQL("DROP TABLE IF EXISTS call_log_status")
            }
        }

        // Migration from version 2 to version 3 (addition of isExcluded to person_data)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE person_data ADD COLUMN isExcluded INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        // Migration from version 3 to version 4 (addition of label to person_data)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE person_data ADD COLUMN label TEXT")
            }
        }
        
        // Migration from version 4 to version 5 (split sync status + reviewed + serverUpdatedAt)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to call_data
                database.execSQL("ALTER TABLE call_data ADD COLUMN reviewed INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE call_data ADD COLUMN metadataSyncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                database.execSQL("ALTER TABLE call_data ADD COLUMN recordingSyncStatus TEXT NOT NULL DEFAULT 'NOT_APPLICABLE'")
                database.execSQL("ALTER TABLE call_data ADD COLUMN serverUpdatedAt INTEGER")
                
                // Add serverUpdatedAt to person_data
                database.execSQL("ALTER TABLE person_data ADD COLUMN serverUpdatedAt INTEGER")
                
                // Migrate existing syncStatus to new columns:
                // If COMPLETED -> metadataSyncStatus=SYNCED, recordingSyncStatus=COMPLETED
                // If PENDING/FAILED -> metadataSyncStatus=PENDING, recordingSyncStatus based on duration
                database.execSQL("""
                    UPDATE call_data SET 
                        metadataSyncStatus = CASE 
                            WHEN syncStatus = 'COMPLETED' THEN 'SYNCED'
                            WHEN syncStatus = 'NOTE_UPDATE_PENDING' THEN 'UPDATE_PENDING'
                            WHEN syncStatus = 'FAILED' THEN 'FAILED'
                            ELSE 'PENDING'
                        END,
                        recordingSyncStatus = CASE 
                            WHEN syncStatus = 'COMPLETED' THEN 'COMPLETED'
                            WHEN syncStatus = 'COMPRESSING' THEN 'COMPRESSING'
                            WHEN syncStatus = 'UPLOADING' THEN 'UPLOADING'
                            WHEN duration > 0 AND localRecordingPath IS NOT NULL THEN 'PENDING'
                            ELSE 'NOT_APPLICABLE'
                        END
                """.trimIndent())
            }
        }
        
        // Migration from version 5 to version 6 (addition of syncError to call_data)
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE call_data ADD COLUMN syncError TEXT")
            }
        }
        
        // Migration from version 6 to version 7 (addition of indices to call_data)
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_call_data_phoneNumber ON call_data(phoneNumber)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_call_data_callDate ON call_data(callDate)")
            }
        }
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callcloud_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
