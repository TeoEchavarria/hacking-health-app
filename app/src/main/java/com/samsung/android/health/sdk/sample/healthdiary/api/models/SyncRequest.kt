package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

data class SyncRequest(
    @SerializedName("sync_type")
    val syncType: String,
    @SerializedName("date_range")
    val dateRange: DateRange? = null,
    @SerializedName("force_refresh")
    val forceRefresh: Boolean = false
)


