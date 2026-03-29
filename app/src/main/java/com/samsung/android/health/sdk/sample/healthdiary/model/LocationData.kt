package com.samsung.android.health.sdk.sample.healthdiary.model

/**
 * GPS location data model.
 * 
 * Contains current or last known location information
 * for the monitored person.
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val timestamp: Long,
    val accuracy: Float? = null
) {
    /**
     * Get a formatted coordinate string
     */
    fun getCoordinatesString(): String {
        return "%.4f, %.4f".format(latitude, longitude)
    }
    
    /**
     * Get Google Maps Static API URL for map preview
     */
    fun getStaticMapUrl(
        width: Int = 600,
        height: Int = 300,
        zoom: Int = 15,
        apiKey: String = ""
    ): String {
        return "https://maps.googleapis.com/maps/api/staticmap?" +
                "center=$latitude,$longitude" +
                "&zoom=$zoom" +
                "&size=${width}x${height}" +
                "&markers=color:red%7C$latitude,$longitude" +
                "&key=$apiKey"
    }
    
    /**
     * Get time elapsed since location was recorded
     */
    fun getTimeAgo(): String {
        val elapsed = System.currentTimeMillis() - timestamp
        val minutes = elapsed / 60_000
        
        return when {
            minutes < 1 -> "Hace un momento"
            minutes < 60 -> "Hace ${minutes.toInt()} min"
            minutes < 120 -> "Hace 1 hora"
            minutes < 1440 -> "Hace ${(minutes / 60).toInt()} horas"
            else -> "Hace ${(minutes / 1440).toInt()} días"
        }
    }
}
