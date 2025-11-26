package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.OmhSyncRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.OmhSyncResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhDataPoint
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.buildReadRequest
import com.samsung.android.health.sdk.sample.healthdiary.utils.omh.OmhConverter
import com.samsung.android.health.sdk.sample.healthdiary.utils.omh.OmhConverterFactory
import com.samsung.android.health.sdk.sample.healthdiary.utils.omh.OmhTypeMapper
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.AggregatedData
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.response.DataResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime

class OmhSyncRepository(private val healthDataStore: HealthDataStore) {
    
    private val apiService = RetrofitClient.omhSyncApiService
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Syncs a specific data type to the backend in OMH format
     */
    suspend fun syncDataType(
        dataType: DataType,
        startDateTime: LocalDateTime? = null,
        endDateTime: LocalDateTime? = null
    ): Result<OmhSyncResponse> {
        return try {
            if (!TokenManager.hasToken()) {
                return Result.failure(Exception("No authentication token available. Please set your access token."))
            }
            
            if (!OmhTypeMapper.isSupported(dataType)) {
                return Result.failure(Exception("Data type ${dataType.name} is not supported for OMH sync"))
            }
            
            val converter = OmhConverterFactory.getConverter(dataType)
                ?: return Result.failure(Exception("No converter available for data type ${dataType.name}"))
            
            // Read data from HealthDataStore
            val omhDataPoints = when (dataType) {
                DataTypes.STEPS -> {
                    // Steps use aggregated data
                    readAndConvertAggregatedSteps(converter, startDateTime, endDateTime)
                }
                else -> {
                    // Other types use regular data points
                    readAndConvertDataPoints(dataType, converter, startDateTime, endDateTime)
                }
            }
            
            if (omhDataPoints.isEmpty()) {
                return Result.failure(Exception("No data found for ${dataType.name}"))
            }
            
            // Get endpoint method
            val method = OmhTypeMapper.getEndpointMethod(dataType)
            
            // Send each data point
            val results = omhDataPoints.mapNotNull { omhDataPoint ->
                sendOmhDataPoint(method, omhDataPoint)
            }
            
            if (results.isEmpty()) {
                Result.failure(Exception("Failed to sync any data points"))
            } else {
                // Return the last successful result
                Result.success(results.last())
            }
        } catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}", e))
        } catch (e: Exception) {
            Log.e("OmhSyncRepository", "Error syncing ${dataType.name}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun readAndConvertDataPoints(
        dataType: DataType,
        converter: OmhConverter<out OmhDataPoint>,
        startDateTime: LocalDateTime?,
        endDateTime: LocalDateTime?
    ): List<OmhDataPoint> {
        val start = startDateTime ?: LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val end = endDateTime ?: start.plusDays(1)
        
        @Suppress("UNCHECKED_CAST")
        val readRequest = try {
            (dataType as? DataType.Readable<HealthDataPoint, *>)?.buildReadRequest(
                start = start,
                end = end,
                ordering = Ordering.ASC
            ) ?: return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
        
        val dataList = healthDataStore.readData(readRequest).dataList
        return converter.convertList(dataList)
    }
    
    private suspend fun readAndConvertAggregatedSteps(
        converter: OmhConverter<out OmhDataPoint>,
        startDateTime: LocalDateTime?,
        endDateTime: LocalDateTime?
    ): List<OmhDataPoint> {
        val start = startDateTime ?: LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val end = endDateTime ?: start.plusDays(1)
        
        val localTimeFilter = LocalTimeFilter.of(start, end)
        val aggregateRequest = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilter(localTimeFilter)
            .setOrdering(Ordering.ASC)
            .build()
        
        val result: DataResponse<AggregatedData<Long>> = healthDataStore.aggregateData(aggregateRequest)
        return result.dataList.mapNotNull { aggregatedData ->
            converter.convertAggregated(aggregatedData)
        }
    }
    
    private suspend fun sendOmhDataPoint(
        method: String,
        omhDataPoint: OmhDataPoint
    ): OmhSyncResponse? {
        return try {
            // Serialize OMH data point to JSON based on its type
            val jsonString = when (omhDataPoint) {
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhStepCount ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhStepCount.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhHeartRate ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhHeartRate.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhSleepDuration ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhSleepDuration.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhOxygenSaturation ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhOxygenSaturation.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBodyTemperature ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBodyTemperature.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhSkinTemperature ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhSkinTemperature.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBloodPressure ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBloodPressure.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBloodGlucose ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBloodGlucose.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBodyWeight ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBodyWeight.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBodyFatPercentage ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhBodyFatPercentage.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhPhysicalActivity ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhPhysicalActivity.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhFluidIntake ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhFluidIntake.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhNutrition ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhNutrition.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhFloorsClimbed ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhFloorsClimbed.serializer(), omhDataPoint)
                is com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhEnergyScore ->
                    json.encodeToString(com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhEnergyScore.serializer(), omhDataPoint)
                else -> {
                    Log.w("OmhSyncRepository", "Unknown OMH data point type: ${omhDataPoint::class.simpleName}")
                    return null
                }
            }
            
            val jsonObject = json.parseToJsonElement(jsonString) as? JsonObject
                ?: return null
            
            val request = OmhSyncRequest(data = jsonObject)
            
            val response = apiService.syncOmhData(method, request)
            
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("OmhSyncRepository", "Error syncing to $method: ${response.code()} - $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("OmhSyncRepository", "Error sending OMH data point to $method", e)
            null
        }
    }
    
    // Convenience methods for specific data types
    suspend fun syncSteps(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.STEPS, startDateTime, endDateTime)
    
    suspend fun syncHeartRate(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.HEART_RATE, startDateTime, endDateTime)
    
    suspend fun syncSleep(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.SLEEP, startDateTime, endDateTime)
    
    suspend fun syncBloodOxygen(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.BLOOD_OXYGEN, startDateTime, endDateTime)
    
    suspend fun syncBodyTemperature(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.BODY_TEMPERATURE, startDateTime, endDateTime)
    
    suspend fun syncSkinTemperature(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.SKIN_TEMPERATURE, startDateTime, endDateTime)
    
    suspend fun syncBloodPressure(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.BLOOD_PRESSURE, startDateTime, endDateTime)
    
    suspend fun syncBloodGlucose(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.BLOOD_GLUCOSE, startDateTime, endDateTime)
    
    suspend fun syncBodyComposition(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.BODY_COMPOSITION, startDateTime, endDateTime)
    
    suspend fun syncExercise(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.EXERCISE, startDateTime, endDateTime)
    
    suspend fun syncFloorsClimbed(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.FLOORS_CLIMBED, startDateTime, endDateTime)
    
    suspend fun syncWaterIntake(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.WATER_INTAKE, startDateTime, endDateTime)
    
    suspend fun syncNutrition(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.NUTRITION, startDateTime, endDateTime)
    
    suspend fun syncEnergyScore(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) =
        syncDataType(DataTypes.ENERGY_SCORE, startDateTime, endDateTime)
    
    fun validateToken(): Boolean {
        return TokenManager.hasToken()
    }
}

