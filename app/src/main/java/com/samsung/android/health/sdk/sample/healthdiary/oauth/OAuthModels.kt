package com.samsung.android.health.sdk.sample.healthdiary.oauth

import com.google.gson.annotations.SerializedName

/**
 * Request to exchange OAuth provider token for app tokens.
 */
data class OAuthTokenRequest(
    @SerializedName("provider")
    val provider: String,
    @SerializedName("id_token")
    val idToken: String,
    @SerializedName("device_info")
    val deviceInfo: DeviceInfo? = null,
    @SerializedName("fcm_token")
    val fcmToken: String? = null
)

/**
 * Device information for OAuth requests.
 */
data class DeviceInfo(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("device_name")
    val deviceName: String,
    @SerializedName("os_version")
    val osVersion: String,
    @SerializedName("app_version")
    val appVersion: String
)

/**
 * OAuth2-compliant token response from backend.
 */
data class OAuthTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String = "Bearer",
    @SerializedName("expires_in")
    val expiresIn: Int
)

/**
 * Available OAuth providers response.
 */
data class OAuthProvidersResponse(
    @SerializedName("providers")
    val providers: List<String>
)

/**
 * Supported OAuth providers.
 */
enum class OAuthProvider(val value: String) {
    GOOGLE("google"),
    GITHUB("github"),
    APPLE("apple");
    
    companion object {
        fun fromValue(value: String): OAuthProvider? {
            return entries.find { it.value == value.lowercase() }
        }
    }
}

/**
 * OAuth authentication state.
 */
sealed class OAuthState {
    object Idle : OAuthState()
    object Loading : OAuthState()
    data class Success(val response: OAuthTokenResponse) : OAuthState()
    data class Error(val message: String, val exception: Throwable? = null) : OAuthState()
}
