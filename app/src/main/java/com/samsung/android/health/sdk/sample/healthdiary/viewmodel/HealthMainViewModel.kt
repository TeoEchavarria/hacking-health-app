/*
 * Copyright (C) 2024 Samsung Electronics Co., Ltd. All rights reserved
 */
package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricCardUiState
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricDefinition
import com.samsung.android.health.sdk.sample.healthdiary.entries.ReadableMetricType
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants
import com.samsung.android.health.sdk.sample.healthdiary.utils.HealthMetricRegistry
import com.samsung.android.health.sdk.sample.healthdiary.utils.buildReadRequest
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.DataPoint
import com.samsung.android.sdk.health.data.data.Field
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.UserDataPoint
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.request.ReadDataRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class HealthMainViewModel(private val healthDataStore: HealthDataStore) :
    ViewModel() {

    private val _permissionResponse = MutableStateFlow(Pair(AppConstants.WAITING, -1))
    private val _exceptionResponse = MutableStateFlow(Throwable("Default"))
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Excepción capturada en coroutine: ${exception.message}", exception)
        Log.e(TAG, "Stack trace: ${exception.stackTraceToString()}")
        viewModelScope.launch {
            _exceptionResponse.emit(exception)
        }
    }
    val permissionResponse: StateFlow<Pair<String, Int>> = _permissionResponse
    val exceptionResponse: StateFlow<Throwable> = _exceptionResponse

    private val _metricCards = MutableStateFlow(
        HealthMetricRegistry.metrics.map {
            HealthMetricCardUiState(it, "--", true)
        }
    )
    val metricCards: StateFlow<List<HealthMetricCardUiState>> = _metricCards

    init {
        refreshMetricCards()
    }

    fun checkForPermission(
        context: Context,
        permSet: MutableSet<Permission>,
        activityId: Int,
    ) {
        Log.d(TAG, "checkForPermission: Verificando ${permSet.size} permisos para activityId=$activityId")
        permSet.forEach { perm ->
            Log.d(TAG, "  - Permiso: ${perm.dataType} (${perm.accessType})")
        }
        
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                val grantedPermissions = healthDataStore.getGrantedPermissions(permSet)
                Log.d(TAG, "checkForPermission: ${grantedPermissions.size} permisos ya otorgados de ${permSet.size} solicitados")

                if (grantedPermissions.containsAll(permSet)) {
                    Log.i(TAG, "checkForPermission: Todos los permisos ya están otorgados")
                    _permissionResponse.emit(Pair(AppConstants.SUCCESS, activityId))
                } else {
                    Log.i(TAG, "checkForPermission: Faltan permisos, solicitando...")
                    requestForPermission(context, permSet, activityId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkForPermission: Error al verificar permisos", e)
                throw e
            }
        }
    }

    private fun requestForPermission(
        context: Context,
        permSet: MutableSet<Permission>,
        activityId: Int,
    ) {
        Log.d(TAG, "requestForPermission: Solicitando ${permSet.size} permisos")
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                val activity = context as Activity
                Log.d(TAG, "requestForPermission: Llamando a healthDataStore.requestPermissions()")
                val result = healthDataStore.requestPermissions(permSet, activity)
                Log.i(TAG, "requestPermissions: Resultado - ${result.size} permisos otorgados de ${permSet.size} solicitados")
                
                result.forEach { perm ->
                    Log.d(TAG, "  - Permiso otorgado: ${perm.dataType} (${perm.accessType})")
                }

                if (result.containsAll(permSet)) {
                    Log.i(TAG, "requestPermissions: Todos los permisos fueron otorgados exitosamente")
                    _permissionResponse.emit(Pair(AppConstants.SUCCESS, activityId))
                } else {
                    val missing = permSet - result.toSet()
                    Log.w(TAG, "requestPermissions: Faltan ${missing.size} permisos:")
                    missing.forEach { perm ->
                        Log.w(TAG, "  - Permiso faltante: ${perm.dataType} (${perm.accessType})")
                    }
                    withContext(Dispatchers.Main) {
                        _permissionResponse.emit(Pair(AppConstants.NO_PERMISSION, -1))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "requestForPermission: Error al solicitar permisos", e)
                throw e
            }
        }
    }

    // Permissions for all data types accessed in this application
    fun connectToSamsungHealth(context: Context) {
        Log.i(TAG, "connectToSamsungHealth: Iniciando conexión con Samsung Health")
        val additionalPermissions = HealthMetricRegistry.metrics.flatMap { it.permissions }
        val permSet = (
            additionalPermissions + setOf(
                Permission.of(DataTypes.STEPS, AccessType.READ),
                Permission.of(DataTypes.SLEEP, AccessType.READ),
                Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ),
                Permission.of(DataTypes.SKIN_TEMPERATURE, AccessType.READ),
                Permission.of(DataTypes.NUTRITION, AccessType.READ),
                Permission.of(DataTypes.HEART_RATE, AccessType.READ),
                Permission.of(DataTypes.NUTRITION, AccessType.WRITE)
            )
        ).toSet()
        Log.d(TAG, "connectToSamsungHealth: Solicitando ${permSet.size} permisos:")
        permSet.forEach { perm ->
            Log.d(TAG, "  - ${perm.dataType} (${perm.accessType})")
        }
        
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                Log.d(TAG, "connectToSamsungHealth: Llamando a healthDataStore.requestPermissions()")
                val activity = context as Activity
                val result = healthDataStore.requestPermissions(permSet, activity)
                Log.i(TAG, "connectToSamsungHealth: Resultado - ${result.size} permisos otorgados de ${permSet.size} solicitados")
                
                if (result.containsAll(permSet)) {
                    Log.i(TAG, "connectToSamsungHealth: ✓ Conexión exitosa - Todos los permisos otorgados")
                } else {
                    val missing = permSet - result.toSet()
                    Log.w(TAG, "connectToSamsungHealth: ⚠ Algunos permisos no fueron otorgados (${missing.size} faltantes)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "connectToSamsungHealth: ✗ Error al conectar con Samsung Health", e)
                throw e
            }
        }
    }

    fun resetPermissionResponse() {
        viewModelScope.launch {
            _permissionResponse.emit(Pair(AppConstants.WAITING, -1))
        }
    }

    fun setDefaultValueToExceptionResponse()
    {
        _exceptionResponse.value = Throwable("Default")
    }

    fun refreshMetricCards() {
        viewModelScope.launch(AppConstants.SCOPE_IO_DISPATCHERS + exceptionHandler) {
            _metricCards.emit(
                HealthMetricRegistry.metrics.map { card ->
                    HealthMetricCardUiState(card, "--", true)
                }
            )

            val updatedCards = HealthMetricRegistry.metrics.map { definition ->
                val value = runCatching { loadLatestValue(definition) }
                    .getOrElse { exception ->
                        Log.w(TAG, "Error leyendo ${definition.dataType.name}", exception)
                        "--"
                    }
                HealthMetricCardUiState(definition, value, false)
            }
            _metricCards.emit(updatedCards)
        }
    }

    private suspend fun loadLatestValue(definition: HealthMetricDefinition<out DataPoint>): String {
        val typedDefinition = definition as HealthMetricDefinition<DataPoint>
        val now = LocalDateTime.now()
        val request = buildReadRequest(
            typedDefinition,
            now.minusDays(30),
            now.plusDays(1),
            limit = 1
        )
        val dataPoint = healthDataStore.readData(request).dataList.firstOrNull()
        val firstField = typedDefinition.dataType.allFields.firstOrNull()
        if (dataPoint == null || firstField == null) {
            return "--"
        }
        return try {
            val formatted = extractFieldValue(dataPoint, firstField as Field<Any?>) ?: "--"
            typedDefinition.summaryUnit?.let { unit ->
                if (formatted == "--") formatted else "$formatted $unit"
            } ?: formatted
        } catch (ex: Exception) {
            "--"
        }
    }

    private fun extractFieldValue(
        dataPoint: DataPoint,
        field: Field<Any?>
    ): String? = when (dataPoint) {
        is HealthDataPoint -> dataPoint.getValue(field)?.toString()
        is UserDataPoint -> dataPoint.getValue(field)?.toString()
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildReadRequest(
        definition: HealthMetricDefinition<DataPoint>,
        start: LocalDateTime,
        end: LocalDateTime,
        limit: Int? = null
    ): ReadDataRequest<DataPoint> =
        readableDataType(definition).buildReadRequest(
            start = start,
            end = end,
            ordering = Ordering.DESC,
            limit = limit
        )

    @Suppress("UNCHECKED_CAST")
    private fun readableDataType(
        definition: HealthMetricDefinition<DataPoint>
    ): ReadableMetricType<DataPoint> =
        definition.dataType as? ReadableMetricType<DataPoint>
            ?: throw UnsupportedOperationException(
                "DataType ${definition.dataType.name} is not readable"
            )

    companion object {
        private const val TAG = "[HTK]HealthDiaryViewModel"
    }
}
