package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

data class SyncedItems(
    @SerializedName("health")
    val health: Int = 0,
    @SerializedName("food")
    val food: Int = 0
)

