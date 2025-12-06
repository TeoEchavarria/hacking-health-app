package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

data class RefreshRequest(
    @SerializedName("refresh")
    val refresh: String
)
