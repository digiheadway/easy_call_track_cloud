package com.example.salescrm.data.local

import androidx.room.TypeConverter
import com.example.salescrm.data.TaskStatus
import com.example.salescrm.data.TaskType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Collections

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun localDateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it) }
    }

    @TypeConverter
    fun localTimeToTimestamp(time: LocalTime?): String? {
        return time?.toString()
    }

    @TypeConverter
    fun stringToList(data: String?): List<String> {
        if (data == null) {
            return Collections.emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    fun listToString(someObjects: List<String>): String {
        return gson.toJson(someObjects)
    }

    @TypeConverter
    fun stringToMap(data: String?): Map<String, String> {
        if (data == null) {
            return Collections.emptyMap()
        }
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(data, mapType)
    }

    @TypeConverter
    fun mapToString(map: Map<String, String>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTaskStatus(status: String): TaskStatus {
        return TaskStatus.valueOf(status)
    }

    @TypeConverter
    fun fromTaskType(type: TaskType): String {
        return type.name
    }

    @TypeConverter
    fun toTaskType(type: String): TaskType {
        return TaskType.valueOf(type)
    }
}
