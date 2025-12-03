package com.samsung.android.health.sdk.sample.healthdiary.data.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room database
 * Handles conversion of complex types to/from database-compatible types
 */
class Converters {
    
    private val gson = Gson()
    
    /**
     * Convert List<String> to JSON string for storage
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    /**
     * Convert JSON string to List<String>
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }
    }
    
    /**
     * Convert Map<String, Any> to JSON string for storage
     */
    @TypeConverter
    fun fromMap(value: Map<String, Any>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    /**
     * Convert JSON string to Map<String, Any>
     */
    @TypeConverter
    fun toMap(value: String?): Map<String, Any>? {
        return value?.let {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(it, type)
        }
    }
}
