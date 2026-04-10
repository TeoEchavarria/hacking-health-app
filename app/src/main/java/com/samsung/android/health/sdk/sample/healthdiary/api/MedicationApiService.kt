package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for managing medications and reminders.
 * 
 * All endpoints require Authorization header with Bearer token.
 * Operations are scoped to the authenticated user.
 */
interface MedicationApiService {
    
    /**
     * Get all medications for the current user.
     * 
     * @param authorization Bearer token
     * @param includeInactive Include inactive medications (default false)
     * @return List of medications sorted by time
     */
    @GET("/medications")
    suspend fun getMedications(
        @Header("Authorization") authorization: String,
        @Query("include_inactive") includeInactive: Boolean = false
    ): Response<List<Medication>>
    
    /**
     * Create a new medication reminder.
     * 
     * @param authorization Bearer token
     * @param request Medication creation request
     * @return Created medication
     */
    @POST("/medications")
    suspend fun createMedication(
        @Header("Authorization") authorization: String,
        @Body request: MedicationCreateRequest
    ): Response<Medication>
    
    /**
     * Get a specific medication.
     * 
     * @param authorization Bearer token
     * @param medicationId ID of the medication
     * @return Medication details
     */
    @GET("/medications/{medicationId}")
    suspend fun getMedication(
        @Header("Authorization") authorization: String,
        @Path("medicationId") medicationId: String
    ): Response<Medication>
    
    /**
     * Update an existing medication.
     * 
     * @param authorization Bearer token
     * @param medicationId ID of the medication
     * @param request Update request with fields to modify
     * @return Updated medication
     */
    @PUT("/medications/{medicationId}")
    suspend fun updateMedication(
        @Header("Authorization") authorization: String,
        @Path("medicationId") medicationId: String,
        @Body request: MedicationUpdateRequest
    ): Response<Medication>
    
    /**
     * Delete a medication (soft delete).
     * 
     * @param authorization Bearer token
     * @param medicationId ID of the medication
     * @return Success message
     */
    @DELETE("/medications/{medicationId}")
    suspend fun deleteMedication(
        @Header("Authorization") authorization: String,
        @Path("medicationId") medicationId: String
    ): Response<MedicationMessageResponse>
    
    /**
     * Record that a medication was taken.
     * 
     * @param authorization Bearer token
     * @param request Take medication request
     * @return Take record
     */
    @POST("/medications/take")
    suspend fun takeMedication(
        @Header("Authorization") authorization: String,
        @Body request: TakeMedicationRequest
    ): Response<MedicationTake>
    
    /**
     * Remove a medication take record.
     * 
     * @param authorization Bearer token
     * @param medicationId ID of the medication
     * @param date Date of the take to remove (YYYY-MM-DD)
     * @return Success message
     */
    @DELETE("/medications/untake/{medicationId}")
    suspend fun untakeMedication(
        @Header("Authorization") authorization: String,
        @Path("medicationId") medicationId: String,
        @Query("date") date: String
    ): Response<MedicationMessageResponse>
    
    /**
     * Get all medications with their take status for a specific date.
     * 
     * @param authorization Bearer token
     * @param date Date to check (YYYY-MM-DD). Defaults to today.
     * @return List of medications with take status
     */
    @GET("/medications/today/status")
    suspend fun getTodayStatus(
        @Header("Authorization") authorization: String,
        @Query("date") date: String? = null
    ): Response<List<MedicationWithTakes>>
    
    /**
     * Get monthly adherence report.
     * 
     * Returns statistics about medication adherence for a specific month.
     * 
     * @param authorization Bearer token
     * @param year Year of the report
     * @param month Month of the report (1-12)
     * @return Monthly report with adherence statistics
     */
    @GET("/medications/report/monthly")
    suspend fun getMonthlyReport(
        @Header("Authorization") authorization: String,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<MonthlyReportResponse>
    
    /**
     * Get calendar events for a month.
     * 
     * Returns daily medication takes for calendar visualization.
     * 
     * @param authorization Bearer token
     * @param year Year
     * @param month Month (1-12)
     * @return List of calendar events for each day
     */
    @GET("/medications/calendar/events")
    suspend fun getCalendarEvents(
        @Header("Authorization") authorization: String,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<List<CalendarEvent>>
}
