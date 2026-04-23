package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

/**
 * Request to update the user's current location.
 */
data class LocationUpdateRequest(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("accuracy")
    val accuracy: Float? = null,
    
    @SerializedName("timestamp")
    val timestamp: Long? = null
)

/**
 * Response after updating location.
 */
data class LocationUpdateResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("updatedAt")
    val updatedAt: Long
)

/**
 * Request to toggle location sharing.
 */
data class SharingToggleRequest(
    @SerializedName("sharingEnabled")
    val sharingEnabled: Boolean
)

/**
 * Response containing sharing status.
 */
data class SharingStatusResponse(
    @SerializedName("sharingEnabled")
    val sharingEnabled: Boolean
)

/**
 * Location data for a user.
 */
data class LocationData(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("userName")
    val userName: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("accuracy")
    val accuracy: Float? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: Long
)

/**
 * Response containing paired user's location.
 */
data class PairedLocationResponse(
    @SerializedName("found")
    val found: Boolean,
    
    @SerializedName("location")
    val location: LocationData? = null,
    
    @SerializedName("message")
    val message: String? = null
)

/**
 * Response containing location history.
 */
data class LocationHistoryResponse(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("locations")
    val locations: List<LocationData>
)
