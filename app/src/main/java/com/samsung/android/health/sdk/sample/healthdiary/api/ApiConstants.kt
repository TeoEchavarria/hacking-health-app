package com.samsung.android.health.sdk.sample.healthdiary.api

object ApiConstants {
    const val BASE_URL = "https://73244b572435.ngrok-free.app"
    const val API_PATH_SYNC = "/api/sync/data"
    const val API_PATH_LOGIN = "/login"
    
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val CONTENT_TYPE_JSON = "application/json"
    const val BEARER_PREFIX = "Bearer "
    
    const val SYNC_TYPE_HEALTH = "health"
    const val SYNC_TYPE_FOOD = "food"
    const val SYNC_TYPE_ALL = "all"
}

