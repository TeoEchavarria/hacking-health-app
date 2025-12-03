/*
 * Copyright (C) 2024 Samsung Electronics Co., Ltd. All rights reserved
 */
package com.samsung.android.health.sdk.sample.healthdiary.config

/**
 * App-level constants
 * Centralized location for all application constants
 * 
 * Note: This is the new location for app constants. The old utils/AppConstants.kt
 * is maintained for backward compatibility but will be deprecated.
 */
object AppConstants {
    
    // ========== Permission & Connection States ==========
    const val SUCCESS = "SUCCESS"
    const val WAITING = "WAITING"
    const val NO_PERMISSION = "NO PERMISSION"
    const val NO_WRITE_PERMISSION = -1
    
    // ========== Activity IDs ==========
    const val NUTRITION_ACTIVITY = 0
    const val STEP_ACTIVITY = 1
    const val HEART_RATE_ACTIVITY = 2
    const val SLEEP_ACTIVITY = 3
    const val CHOOSE_FOOD_ACTIVITY = 4
    const val UPDATE_FOOD_ACTIVITY = 5
    const val EXERCISE_ACTIVITY = 6
    const val EXERCISE_LOCATION_ACTIVITY = 7
    const val SKIN_TEMPERATURE_ACTIVITY = 8
    const val BLOOD_OXYGEN_ACTIVITY = 9
    const val ACTIVITY_SUMMARY_ACTIVITY = 10
    const val FLOORS_CLIMBED_ACTIVITY = 11
    const val BLOOD_GLUCOSE_ACTIVITY = 12
    const val BLOOD_PRESSURE_ACTIVITY = 13
    const val BODY_COMPOSITION_ACTIVITY = 14
    const val SLEEP_GOAL_ACTIVITY = 15
    const val STEPS_GOAL_ACTIVITY = 16
    const val ACTIVE_CALORIES_GOAL_ACTIVITY = 17
    const val ACTIVE_TIME_GOAL_ACTIVITY = 18
    const val WATER_INTAKE_ACTIVITY = 19
    const val WATER_INTAKE_GOAL_ACTIVITY = 20
    const val NUTRITION_GOAL_ACTIVITY = 21
    const val ENERGY_SCORE_ACTIVITY = 22
    const val USER_PROFILE_ACTIVITY = 23
    const val BODY_TEMPERATURE_ACTIVITY = 24
    const val LOGS_DOCS_ACTIVITY = 25  // New: For watch-to-phone logs
    
    // ========== Measurement Units ==========
    const val SKIN_TEMP_UNIT = "\u2103"
    const val BLOOD_OXYGEN_UNIT = "\u0025"
    const val BODY_TEMP_UNIT = "\u2103"
    const val BLOOD_PRESSURE_UNIT = "mmHg"
    const val BLOOD_GLUCOSE_UNIT = "mg/dL"
    const val BODY_FAT_PERCENT_UNIT = "\u0025"
    const val BODY_WATER_PERCENT_UNIT = "\u0025"
    const val FLOOR_COUNT_UNIT = "floors"
    const val DURATION_UNIT = "min"
    const val WATER_VOLUME_UNIT = "mL"
    const val CALORIES_UNIT = "kcal"
    const val SCORE_UNIT = "pts"
    
    // ========== Bundle Keys ==========
    const val BUNDLE_KEY_MEAL_TYPE = "MEAL_TYPE"
    const val BUNDLE_KEY_INSERT_DATE = "INSERT_DATE"
    const val BUNDLE_KEY_NUTRITION_DATA = "NUTRITION_DATA"
    
    // ========== App Identifiers ==========
    const val APP_ID = "com.samsung.android.health.sdk.sample.healthdiary"
    
    // ========== Authentication ==========
    const val AUTO_AUTH_FALLBACK_PASSWORD = "HealthDiary!123"
    
    // ========== SharedPreferences Keys ==========
    const val PREFS_AUTH = "auth_prefs"
    const val PREFS_USER = "user_prefs"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_ID = "user_id"
    
    // ========== Network & API ==========
    const val DEFAULT_TIMEOUT_SECONDS = 30L
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 1000L
    
    // ========== Sensor Data ==========
    const val SENSOR_BATCH_SIZE = 100
    const val SENSOR_UPLOAD_INTERVAL_MS = 300000L // 5 minutes
    const val SENSOR_DATA_PATH = "/sensor_batch"
    
    // ========== Date/Time ==========
    const val DATE_FORMAT_DISPLAY = "MMM dd, yyyy"
    const val TIME_FORMAT_DISPLAY = "HH:mm"
    const val DATETIME_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'"
}
