package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhDataPoint
import com.samsung.android.sdk.health.data.data.AggregatedData
import com.samsung.android.sdk.health.data.data.HealthDataPoint

/**
 * Base interface for converting Samsung Health SDK data to OMH format
 */
interface OmhConverter<T : OmhDataPoint> {
    /**
     * Converts a HealthDataPoint to OMH format
     */
    fun convert(dataPoint: HealthDataPoint): T?

    /**
     * Converts a list of HealthDataPoints to OMH format
     */
    fun convertList(dataPoints: List<HealthDataPoint>): List<T> {
        return dataPoints.mapNotNull { convert(it) }
    }

    /**
     * Converts AggregatedData to OMH format (for step counts, etc.)
     */
    fun convertAggregated(aggregatedData: AggregatedData<*>): T? {
        return null // Override in subclasses that support aggregated data
    }

    /**
     * Converts a list of AggregatedData to OMH format
     */
    fun convertAggregatedList(aggregatedDataList: List<AggregatedData<*>>): List<T> {
        return aggregatedDataList.mapNotNull { convertAggregated(it) }
    }
}


