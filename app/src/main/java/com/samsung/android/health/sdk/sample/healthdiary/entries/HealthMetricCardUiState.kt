package com.samsung.android.health.sdk.sample.healthdiary.entries

data class HealthMetricCardUiState(
    val definition: HealthMetricDefinition<*>,
    val latestValue: String,
    val isLoading: Boolean
)

