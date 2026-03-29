package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Tracking Repository
 * 
 * Manages location tracking data with Room database persistence.
 * Provides mock data generation for demonstration purposes.
 */
class TrackingRepository(context: Context) {
    
    private val locationDao = AppDatabase.getDatabase(context).locationDao()
    
    /**
     * Get location points from the last 24 hours
     */
    fun getLast24Hours(): Flow<List<LocationPointEntity>> {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        return locationDao.getLast24Hours(cutoffTime)
    }
    
    /**
     * Get the most recent location
     */
    suspend fun getLatestLocation(): LocationPointEntity? {
        return locationDao.getLatest()
    }
    
    /**
     * Insert a new location point
     */
    suspend fun insertLocation(location: LocationPointEntity) {
        locationDao.insert(location)
    }
    
    /**
     * Insert multiple location points
     */
    suspend fun insertAll(locations: List<LocationPointEntity>) {
        locationDao.insertAll(locations)
    }
    
    /**
     * Delete location points older than 24 hours
     */
    suspend fun cleanupOldLocations() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        locationDao.deleteOlderThan(cutoffTime)
    }
    
    /**
     * Get count of location points in the last 24 hours
     */
    suspend fun getRecentLocationCount(): Int {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        return locationDao.getCountLast24Hours(cutoffTime)
    }
    
    /**
     * Generate a mock location near Parque del Retiro, Madrid
     * Base coordinates: 40.4153, -3.6844
     */
    fun generateMockLocationNearLastPosition(
        lastLocation: LocationPointEntity?,
        activityType: String = "Walking"
    ): LocationPointEntity {
        // Base: Parque del Retiro, Madrid
        val baseLat = lastLocation?.latitude ?: 40.4153
        val baseLng = lastLocation?.longitude ?: -3.6844
        
        // Small random offset (approx 50-100 meters)
        val latOffset = Random.nextDouble(-0.0008, 0.0008)
        val lngOffset = Random.nextDouble(-0.0012, 0.0012)
        
        val newLat = baseLat + latOffset
        val newLng = baseLng + lngOffset
        
        val address = when {
            activityType == "Still" -> "Parque del Retiro - Zona de Descanso"
            Random.nextBoolean() -> "Parque del Retiro, Madrid"
            else -> "Paseo del Prado, Madrid"
        }
        
        return LocationPointEntity(
            latitude = newLat,
            longitude = newLng,
            address = address,
            timestamp = System.currentTimeMillis(),
            activityType = activityType
        )
    }
    
    /**
     * Initialize database with sample historical data
     */
    suspend fun initializeMockData() {
        val count = getRecentLocationCount()
        if (count > 0) return // Already has data
        
        val now = System.currentTimeMillis()
        val mockLocations = listOf(
            // 8:00 AM - Home
            LocationPointEntity(
                latitude = 40.4200,
                longitude = -3.7000,
                address = "Hogar - Calle Mayor 12",
                timestamp = now - TimeUnit.HOURS.toMillis(8),
                activityType = "Still"
            ),
            // 10:30 AM - Pharmacy
            LocationPointEntity(
                latitude = 40.4180,
                longitude = -3.6920,
                address = "Farmacia San José - Calle de Alcalá",
                timestamp = now - TimeUnit.HOURS.toMillis(6) - TimeUnit.MINUTES.toMillis(30),
                activityType = "Still"
            ),
            // 11:00 AM - Walking to park
            LocationPointEntity(
                latitude = 40.4165,
                longitude = -3.6880,
                address = "Calle de Alfonso XII",
                timestamp = now - TimeUnit.HOURS.toMillis(5),
                activityType = "Walking"
            ),
            // Current position - Park entrance
            LocationPointEntity(
                latitude = 40.4153,
                longitude = -3.6844,
                address = "Parque del Retiro - Entrada Principal",
                timestamp = now - TimeUnit.MINUTES.toMillis(15),
                activityType = "Walking"
            )
        )
        
        insertAll(mockLocations)
    }
}
