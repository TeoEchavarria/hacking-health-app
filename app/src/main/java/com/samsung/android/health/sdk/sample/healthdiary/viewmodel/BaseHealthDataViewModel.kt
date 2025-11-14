package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricDefinition
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricFieldValue
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricRecord
import com.samsung.android.health.sdk.sample.healthdiary.entries.ReadableMetricType
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants
import com.samsung.android.health.sdk.sample.healthdiary.utils.buildReadRequest
import com.samsung.android.health.sdk.sample.healthdiary.utils.dateFormat
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.DataPoint
import com.samsung.android.sdk.health.data.data.Field
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.UserDataPoint
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.request.ReadDataRequest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseHealthDataViewModel<T : DataPoint>(
    private val healthDataStore: HealthDataStore,
    protected val metricDefinition: HealthMetricDefinition<T>
) : ViewModel() {

    private val _records = MutableLiveData<List<HealthMetricRecord>>()
    val records: LiveData<List<HealthMetricRecord>> = _records

    val dayStartTimeAsText = ObservableField<String>()
    val isLoading = ObservableBoolean(false)

    private val _exceptionResponse = MutableStateFlow(Throwable("Default"))
    val exceptionResponse: StateFlow<Throwable> = _exceptionResponse

    private val _latestValue = MutableStateFlow("--")
    val latestValue: StateFlow<String> = _latestValue

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            _exceptionResponse.emit(exception)
            _statusMessage.emit(messageForException(exception))
        }
    }

    fun readMetricData(dateTime: LocalDateTime) {
        dayStartTimeAsText.set(dateTime.format(dateFormat))
        viewModelScope.launch(AppConstants.SCOPE_IO_DISPATCHERS + exceptionHandler) {
            isLoading.set(true)
            try {
                _statusMessage.emit(null)
                val request = buildReadRequest(dateTime)
                val dataList = healthDataStore.readData(request).dataList
                val mappedRecords = dataList.map { mapRecord(it) }
                _records.postValue(mappedRecords)
                val summary = mappedRecords.firstOrNull()?.fieldValues?.firstOrNull()?.value
                _latestValue.emit(summaryWithUnit(summary))
                if (mappedRecords.isEmpty()) {
                    _statusMessage.emit(NO_DATA_MESSAGE)
                }
            } finally {
                isLoading.set(false)
            }
        }
    }

    fun setDefaultValueToExceptionResponse() {
        _exceptionResponse.value = Throwable("Default")
    }

    private fun summaryWithUnit(value: String?): String {
        if (value.isNullOrBlank()) return "--"
        return metricDefinition.summaryUnit?.let { "$value $it" } ?: value
    }

    private fun mapRecord(dataPoint: T): HealthMetricRecord {
        metricDefinition.mapper?.let { return it(dataPoint) }
        return defaultRecord(dataPoint)
    }

    private fun defaultRecord(dataPoint: T): HealthMetricRecord {
        val zoneDefault = ZoneId.systemDefault()
        val sourcePoint = dataPoint as? HealthDataPoint
        val startInstant = sourcePoint?.startTime ?: Instant.now()
        val endInstant = sourcePoint?.endTime ?: startInstant
        val zoneId = sourcePoint?.zoneOffset?.let { ZoneId.ofOffset("UTC", it) } ?: zoneDefault
        val fieldValues = metricDefinition.dataType.allFields.map { field ->
            val formattedValue = formatValueSafe(dataPoint, field as Field<Any?>)
            HealthMetricFieldValue(
                label = prettifyFieldName(field.name),
                value = formattedValue
            )
        }
        return HealthMetricRecord(
            start = startInstant,
            end = endInstant,
            zoneId = zoneId,
            fieldValues = fieldValues,
            dataSourceName = sourcePoint?.dataSource?.appId
        )
    }

    private fun formatValueSafe(
        dataPoint: DataPoint,
        field: Field<Any?>
    ): String =
        try {
            when (val value = readFieldValue(dataPoint, field)) {
                null -> "-"
                is Float -> String.format(Locale.ENGLISH, "%.2f", value)
                is Double -> String.format(Locale.ENGLISH, "%.2f", value)
                is Int -> value.toString()
                is Long -> value.toString()
                is Boolean -> if (value) "Yes" else "No"
                is Instant -> value.atZone(ZoneId.systemDefault()).toString()
                is List<*> -> value.joinToString()
                else -> value.toString()
            }
        } catch (ex: Exception) {
            "-"
        }

    private fun prettifyFieldName(raw: String): String =
        raw.replace("_", " ")
            .lowercase(Locale.getDefault())
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            }

    private fun buildReadRequest(dateTime: LocalDateTime): ReadDataRequest<T> =
        readableDataType().buildReadRequest(
            start = dateTime,
            end = dateTime.plusDays(1),
            ordering = Ordering.DESC
        )

    @Suppress("UNCHECKED_CAST")
    private fun readableDataType(): ReadableMetricType<T> =
        metricDefinition.dataType as? ReadableMetricType<T>
            ?: throw UnsupportedOperationException(
                "DataType ${metricDefinition.dataType.name} is not readable"
            )

    private fun readFieldValue(
        dataPoint: DataPoint,
        field: Field<Any?>
    ): Any? = when (dataPoint) {
        is HealthDataPoint -> dataPoint.getValue(field)
        is UserDataPoint -> dataPoint.getValue(field)
        else -> null
    }

    private fun messageForException(exception: Throwable): String =
        if (exception is UnsupportedOperationException ||
            exception.message?.contains("not support", true) == true
        ) {
            METRIC_UNAVAILABLE_MESSAGE
        } else {
            exception.message ?: GENERIC_ERROR_MESSAGE
        }

    companion object {
        private const val NO_DATA_MESSAGE = "No data for the selected date."
        private const val METRIC_UNAVAILABLE_MESSAGE = "This metric is not available on this device."
        private const val GENERIC_ERROR_MESSAGE = "Unable to load data."
    }
}

