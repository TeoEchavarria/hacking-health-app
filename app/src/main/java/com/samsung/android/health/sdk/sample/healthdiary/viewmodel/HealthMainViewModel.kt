/*
 * Copyright (C) 2024 Samsung Electronics Co., Ltd. All rights reserved
 */
package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val permSet = setOf(
            Permission.of(DataTypes.STEPS, AccessType.READ),
            Permission.of(DataTypes.SLEEP, AccessType.READ),
            Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ),
            Permission.of(DataTypes.SKIN_TEMPERATURE, AccessType.READ),
            Permission.of(DataTypes.NUTRITION, AccessType.READ),
            Permission.of(DataTypes.HEART_RATE, AccessType.READ),
            Permission.of(DataTypes.NUTRITION, AccessType.WRITE)
        )
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

    companion object {
        private const val TAG = "[HTK]HealthDiaryViewModel"
    }
}
