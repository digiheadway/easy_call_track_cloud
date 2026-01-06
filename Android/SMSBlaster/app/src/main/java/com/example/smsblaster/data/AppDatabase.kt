package com.example.smsblaster.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smsblaster.data.dao.CampaignDao
import com.example.smsblaster.data.dao.CampaignMessageDao
import com.example.smsblaster.data.dao.ContactDao
import com.example.smsblaster.data.dao.TemplateDao
import com.example.smsblaster.data.model.Campaign
import com.example.smsblaster.data.model.CampaignMessage
import com.example.smsblaster.data.model.Contact
import com.example.smsblaster.data.model.Template

@Database(
    entities = [
        Contact::class,
        Template::class,
        Campaign::class,
        CampaignMessage::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun templateDao(): TemplateDao
    abstract fun campaignDao(): CampaignDao
    abstract fun campaignMessageDao(): CampaignMessageDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_blaster_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
