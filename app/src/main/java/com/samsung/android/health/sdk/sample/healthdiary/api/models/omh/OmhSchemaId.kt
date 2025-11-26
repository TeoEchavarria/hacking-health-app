package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhSchemaId(
    val namespace: String = "omh",
    val name: String,
    val version: String = "1.0"
)


