package com.samsung.android.health.sdk.sample.healthdiary.utils

import com.samsung.android.sdk.health.data.data.DataPoint
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.request.ReadDataRequest
import java.time.LocalDateTime

@Suppress("UNCHECKED_CAST")
fun <T : DataPoint> DataType.Readable<T, out ReadDataRequest.Builder<T>>.buildReadRequest(
    start: LocalDateTime,
    end: LocalDateTime,
    ordering: Ordering = Ordering.DESC,
    limit: Int? = null
): ReadDataRequest<T> {
    val builder = this.readDataRequestBuilder
    when (builder) {
        is ReadDataRequest.DualTimeBuilder<*> -> configureDualTimeBuilder(
            builder as ReadDataRequest.DualTimeBuilder<T>,
            start,
            end,
            ordering,
            limit
        )
        is ReadDataRequest.LocalDateBuilder<*> -> configureLocalDateBuilder(
            builder as ReadDataRequest.LocalDateBuilder<T>,
            start,
            end,
            ordering,
            limit
        )
        else -> limit?.let { trySetLimit(builder, it) }
    }
    return builder.build()
}

private fun <T : DataPoint> configureDualTimeBuilder(
    builder: ReadDataRequest.DualTimeBuilder<T>,
    start: LocalDateTime,
    end: LocalDateTime,
    ordering: Ordering,
    limit: Int?
) {
    builder
        .setLocalTimeFilter(LocalTimeFilter.of(start, end))
        .setOrdering(ordering)
    limit?.let { builder.setLimit(it) }
}

private fun <T : DataPoint> configureLocalDateBuilder(
    builder: ReadDataRequest.LocalDateBuilder<T>,
    start: LocalDateTime,
    end: LocalDateTime,
    ordering: Ordering,
    limit: Int?
) {
    builder
        .setLocalDateFilter(LocalDateFilter.of(start.toLocalDate(), end.toLocalDate()))
        .setOrdering(ordering)
    limit?.let { builder.setLimit(it) }
}

private fun <T : DataPoint> trySetLimit(
    builder: ReadDataRequest.Builder<T>,
    limit: Int
) {
    runCatching {
        builder::class.java.getMethod("setLimit", Int::class.javaPrimitiveType)
            .invoke(builder, limit)
    }
}


