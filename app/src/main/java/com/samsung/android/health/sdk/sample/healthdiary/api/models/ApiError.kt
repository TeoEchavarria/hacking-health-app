package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

data class ApiError(
    @SerializedName("detail")
    val detail: String? = null,
    @SerializedName("message")
    val message: String? = null
)



