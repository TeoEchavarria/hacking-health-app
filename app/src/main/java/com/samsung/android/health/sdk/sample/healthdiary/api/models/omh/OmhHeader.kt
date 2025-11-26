package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhHeader(
    val schema_id: OmhSchemaId,
    val acquisition_provenance: OmhProvenance? = OmhProvenance()
)


