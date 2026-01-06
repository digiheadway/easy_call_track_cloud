package com.example.smsblaster.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.smsblaster.data.model.CampaignStatus
import com.example.smsblaster.data.model.MessageStatus

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
    
    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromCampaignStatus(status: CampaignStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toCampaignStatus(value: String): CampaignStatus {
        return CampaignStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus {
        return MessageStatus.valueOf(value)
    }
}
