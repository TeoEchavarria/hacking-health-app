package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

data class SyncResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("synced_items")
    val syncedItems: SyncedItems,
    @SerializedName("timestamp")
    val timestamp: String
)

