package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class EnergyScoreOmhConverter : OmhConverter<OmhEnergyScore> {
    override fun convert(dataPoint: HealthDataPoint): OmhEnergyScore? {
        val energyScore = dataPoint.getValue(DataType.EnergyScoreType.ENERGY_SCORE) as? Int
            ?: (dataPoint.getValue(DataType.EnergyScoreType.ENERGY_SCORE) as? Float)?.toInt()
            ?: (dataPoint.getValue(DataType.EnergyScoreType.ENERGY_SCORE) as? Long)?.toInt()
            ?: return null

        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        return OmhEnergyScore(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "energy-score", namespace = "custom")
            ),
            body = OmhEnergyScoreBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                energy_score = EnergyScoreValue(
                    value = energyScore
                )
            )
        )
    }
}

