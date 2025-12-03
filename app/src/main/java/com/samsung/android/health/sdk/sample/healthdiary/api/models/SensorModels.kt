package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

data class SensorBatchRequest(
    @SerializedName("records")
    val records: List<SensorRecordDto>
)

data class SensorRecordDto(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("x")
    val x: Float,
    @SerializedName("y")
    val y: Float,
    @SerializedName("z")
    val z: Float,
    @SerializedName("source")
    val source: String = "watch"
)
