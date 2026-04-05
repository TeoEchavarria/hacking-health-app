package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientAlertsResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientDataResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientHealthSummaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API service for accessing patient health data as a caregiver.
 * 
 * Authorization:
 * - User can access their own data (patientId == current user)
 * - Caregiver can access linked patient's data (active pairing required)
 * 
 * All endpoints require Authorization header with Bearer token.
 */
interface PatientHealthApiService {
    
    /**
     * Get sensor data for a patient.
     * 
     * Returns accelerometer data collected from the patient's watch.
     * Data is sorted by timestamp descending (newest first).
     * 
     * @param authorization Bearer token
     * @param patientId ID of the patient
     * @param startTime Start timestamp in ms (inclusive), optional
     * @param endTime End timestamp in ms (inclusive), optional
     * @param limit Maximum records to return (1-1000, default 100)
     * @return Sensor data with pagination info
     */
    @GET("/health/patient/{patientId}/data")
    suspend fun getPatientData(
        @Header("Authorization") authorization: String,
        @Path("patientId") patientId: String,
        @Query("start_time") startTime: Long? = null,
        @Query("end_time") endTime: Long? = null,
        @Query("limit") limit: Int = 100
    ): Response<PatientDataResponse>
    
    /**
     * Get alerts for a patient.
     * 
     * Returns health alerts generated for the patient.
     * Alerts are sorted by creation time descending (newest first).
     * 
     * @param authorization Bearer token
     * @param patientId ID of the patient
     * @param cursor Pagination cursor (alert_id to start after), optional
     * @param limit Maximum alerts to return (1-100, default 50)
     * @param severity Filter by severity level (info|moderate|high|urgent), optional
     * @return Alerts with pagination info
     */
    @GET("/health/patient/{patientId}/alerts")
    suspend fun getPatientAlerts(
        @Header("Authorization") authorization: String,
        @Path("patientId") patientId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("severity") severity: String? = null
    ): Response<PatientAlertsResponse>
    
    /**
     * Get health summary for a patient (last 24 hours).
     * 
     * Returns summary of health metrics:
     * - Heart rate (average, min, max, last reading)
     * - Steps (total count)
     * - Sleep (total minutes)
     * - Unavailable metrics (SpO2, Blood Pressure, Temperature)
     * 
     * @param authorization Bearer token
     * @param patientId ID of the patient
     * @return Health summary with available metrics
     */
    @GET("/health/patient/{patientId}/summary")
    suspend fun getPatientHealthSummary(
        @Header("Authorization") authorization: String,
        @Path("patientId") patientId: String
    ): Response<PatientHealthSummaryResponse>
}
