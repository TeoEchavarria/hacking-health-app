package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class TimeInterval(
    val start_date_time: String,
    val end_date_time: String
)


