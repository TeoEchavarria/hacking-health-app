package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

/**
 * OpenWearables SDK credentials returned from login
 */
data class OpenWearablesCredentials(
    @SerializedName("ow_user_id")
    val owUserId: String?,
    @SerializedName("ow_access_token")
    val owAccessToken: String?,
    @SerializedName("ow_refresh_token")
    val owRefreshToken: String?
)

data class LoginResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("refresh")
    val refresh: String,
    @SerializedName("expiry")
    val expiry: String,
    @SerializedName("open_wearables")
    val openWearables: OpenWearablesCredentials? = null
)

