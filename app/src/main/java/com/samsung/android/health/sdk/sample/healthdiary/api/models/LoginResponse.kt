package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("refresh")
    val refresh: String,
    @SerializedName("expiry")
    val expiry: String
)

