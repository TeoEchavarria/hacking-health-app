package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

/**
 * Generic success response from API operations.
 */
data class SuccessResponse(
    @SerializedName("success")
    val success: Boolean
)
