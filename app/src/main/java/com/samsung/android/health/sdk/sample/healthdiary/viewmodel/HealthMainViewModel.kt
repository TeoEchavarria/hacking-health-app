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
import com.samsung.android.sdk.health.data.data.AggregatedData
import com.samsung.android.sdk.health.data.data.DataPoint
import com.samsung.android.sdk.health.data.data.Field
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.UserDataPoint
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.request.ReadDataRequest
import com.samsung.android.sdk.health.data.response.DataResponse
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

    private val _todayStepsTotal = MutableStateFlow("0")
    val todayStepsTotal: StateFlow<String> = _todayStepsTotal

    private val _todaySleepTotal = MutableStateFlow("0.0h")
    val todaySleepTotal: StateFlow<String> = _todaySleepTotal

    init {
        refreshMetricCards()
        getTodayStepsTotal()
        getTodaySleepTotal()
    }

    /**
     * Obtiene todos los permisos requeridos para la aplicación (solo lectura)
     */
    private fun getAllRequiredPermissions(): Set<Permission> {
        val additionalPermissions = HealthMetricRegistry.metrics.flatMap { it.permissions }
        return (
            additionalPermissions + setOf(
                Permission.of(DataTypes.STEPS, AccessType.READ),
                Permission.of(DataTypes.SLEEP, AccessType.READ),
                Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ),
                Permission.of(DataTypes.SKIN_TEMPERATURE, AccessType.READ),
                Permission.of(DataTypes.NUTRITION, AccessType.READ),
                Permission.of(DataTypes.HEART_RATE, AccessType.READ)
            )
        ).toSet()
    }

    fun checkForPermission(
        context: Context,
        permSet: MutableSet<Permission>,
        activityId: Int,
    ) {
        Log.d(TAG, "checkForPermission: Verificando permisos para activityId=$activityId")
        
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                // Siempre verificar y solicitar TODOS los permisos requeridos, no solo los del card específico
                val allRequiredPermissions = getAllRequiredPermissions()
                val grantedPermissions = healthDataStore.getGrantedPermissions(allRequiredPermissions)
                Log.d(TAG, "checkForPermission: ${grantedPermissions.size} permisos ya otorgados de ${allRequiredPermissions.size} requeridos")

                if (grantedPermissions.containsAll(allRequiredPermissions)) {
                    Log.i(TAG, "checkForPermission: Todos los permisos ya están otorgados")
                    _permissionResponse.emit(Pair(AppConstants.SUCCESS, activityId))
                } else {
                    Log.i(TAG, "checkForPermission: Faltan permisos, solicitando TODOS los permisos de una vez...")
                    // Ejecutar requestPermissions dentro de la misma corrutina
                    requestAllPermissionsInSameCoroutine(context, activityId, allRequiredPermissions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkForPermission: Error al verificar permisos", e)
                _permissionResponse.emit(Pair(AppConstants.NO_PERMISSION, -1))
            }
        }
    }

    /**
     * Solicita TODOS los permisos requeridos de Samsung Health de una vez
     * Ejecuta dentro de la misma corrutina para evitar problemas de sincronización
     */
    private suspend fun requestAllPermissionsInSameCoroutine(
        context: Context,
        activityId: Int,
        permSet: Set<Permission>
    ) {
        Log.d(TAG, "requestAllPermissionsInSameCoroutine: Solicitando ${permSet.size} permisos en una sola petición:")
        permSet.forEach { perm ->
            Log.d(TAG, "  - ${perm.dataType} (${perm.accessType})")
        }
        
        try {
            val activity = context as Activity
            Log.d(TAG, "requestAllPermissionsInSameCoroutine: Llamando a healthDataStore.requestPermissions()")
            
            // Ejecutar en el hilo principal para que el diálogo se muestre correctamente
            val result = withContext(Dispatchers.Main) {
                healthDataStore.requestPermissions(permSet, activity)
            }
            
            Log.i(TAG, "requestAllPermissionsInSameCoroutine: Resultado - ${result.size} permisos otorgados de ${permSet.size} solicitados")
            
            result.forEach { perm ->
                Log.d(TAG, "  - Permiso otorgado: ${perm.dataType} (${perm.accessType})")
            }

            if (result.containsAll(permSet)) {
                Log.i(TAG, "requestAllPermissionsInSameCoroutine: Todos los permisos fueron otorgados exitosamente")
                _permissionResponse.emit(Pair(AppConstants.SUCCESS, activityId))
            } else {
                val missing = permSet - result.toSet()
                Log.w(TAG, "requestAllPermissionsInSameCoroutine: Faltan ${missing.size} permisos:")
                missing.forEach { perm ->
                    Log.w(TAG, "  - Permiso faltante: ${perm.dataType} (${perm.accessType})")
                }
                // Si el usuario canceló o rechazó, aún así intentar continuar si hay algunos permisos otorgados
                if (result.isNotEmpty()) {
                    Log.i(TAG, "requestAllPermissionsInSameCoroutine: Algunos permisos otorgados, continuando...")
                    _permissionResponse.emit(Pair(AppConstants.SUCCESS, activityId))
                } else {
                    _permissionResponse.emit(Pair(AppConstants.NO_PERMISSION, -1))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "requestAllPermissionsInSameCoroutine: Error al solicitar permisos", e)
            _permissionResponse.emit(Pair(AppConstants.NO_PERMISSION, -1))
        }
    }

    // Permissions for all data types accessed in this application (solo lectura)
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
                Permission.of(DataTypes.HEART_RATE, AccessType.READ)
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
        getTodayStepsTotal()
        getTodaySleepTotal()
    }

    fun getTodayStepsTotal() {
        viewModelScope.launch(AppConstants.SCOPE_IO_DISPATCHERS + exceptionHandler) {
            try {
                val today = LocalDateTime.now()
                val startOfDay = today.withHour(0).withMinute(0).withSecond(0).withNano(0)
                val endOfDay = startOfDay.plusDays(1)

                val localtimeFilter = LocalTimeFilter.of(startOfDay, endOfDay)
                val localTimeGroup = LocalTimeGroup.of(LocalTimeGroupUnit.HOURLY, 1)
                val aggregateRequest = DataType.StepsType.TOTAL.requestBuilder
                    .setLocalTimeFilterWithGroup(localtimeFilter, localTimeGroup)
                    .setOrdering(Ordering.ASC)
                    .build()

                val result = healthDataStore.aggregateData(aggregateRequest)
                var totalSteps: Long = 0

                result.dataList.forEach { stepData ->
                    val hourlySteps = stepData.value as Long
                    totalSteps += hourlySteps
                }

                // Format number with thousands separator
                val formattedTotal = String.format("%,d", totalSteps)
                _todayStepsTotal.emit(formattedTotal)
            } catch (e: Exception) {
                Log.w(TAG, "Error obteniendo pasos del día actual", e)
                _todayStepsTotal.emit("0")
            }
        }
    }

    fun getTodaySleepTotal() {
        viewModelScope.launch(AppConstants.SCOPE_IO_DISPATCHERS + exceptionHandler) {
            try {
                val today = LocalDateTime.now()
                val startOfDay = today.withHour(0).withMinute(0).withSecond(0).withNano(0)
                val endOfDay = startOfDay.plusDays(1)

                val readRequest = DataTypes.SLEEP.buildReadRequest(
                    start = startOfDay,
                    end = endOfDay,
                    ordering = Ordering.ASC
                )

                val sleepDataList = healthDataStore.readData(readRequest).dataList
                var totalSleepMinutes = 0L

                sleepDataList.forEach { sleepData ->
                    val duration = sleepData.getValue(DataType.SleepType.DURATION)
                    if (duration != null) {
                        // Duration is in seconds, convert to minutes
                        totalSleepMinutes += (duration as Long) / 60
                    }
                }

                // Convert minutes to hours with one decimal place
                val totalSleepHours = totalSleepMinutes / 60f
                val formattedTotal = String.format("%.1fh", totalSleepHours)
                _todaySleepTotal.emit(formattedTotal)
            } catch (e: Exception) {
                Log.w(TAG, "Error obteniendo sueño del día actual", e)
                _todaySleepTotal.emit("0.0h")
            }
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

