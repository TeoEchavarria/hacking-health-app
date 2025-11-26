package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class WaterIntakeOmhConverter : OmhConverter<OmhFluidIntake> {
    override fun convert(dataPoint: HealthDataPoint): OmhFluidIntake? {
        // Water intake converter - simplified, return null for now
        // This should be implemented with proper field access in the repository
        return null
    }
}

