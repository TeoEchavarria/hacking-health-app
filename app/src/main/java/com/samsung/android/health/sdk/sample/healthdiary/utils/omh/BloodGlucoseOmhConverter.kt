package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class BloodGlucoseOmhConverter : OmhConverter<OmhBloodGlucose> {
    override fun convert(dataPoint: HealthDataPoint): OmhBloodGlucose? {
        // Try to get glucose value - we'll need to iterate through known fields
        // For now, return null as we need the DataType to access fields properly
        // This converter should be used with specific field access in the repository
        return null
    }
}

