package com.samsung.android.health.sdk.sample.healthdiary.entries

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricRecord
import com.samsung.android.sdk.health.data.data.DataPoint
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.ReadDataRequest

typealias ReadableMetricType<T> =
    DataType.Readable<T, out ReadDataRequest.Builder<T>>

data class HealthMetricDefinition<T : DataPoint>(
    val activityId: Int,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val dataType: DataType,
    val permissions: Set<Permission>,
    val summaryUnit: String? = null,
    val mapper: ((T) -> HealthMetricRecord)? = null
)

