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
    version = 4,
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
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callcloud_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
