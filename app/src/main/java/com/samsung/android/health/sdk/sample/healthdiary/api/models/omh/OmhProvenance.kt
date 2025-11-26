package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhProvenance(
    val source_name: String = "Samsung Health SDK",
    val modality: String? = "sensed"
)


