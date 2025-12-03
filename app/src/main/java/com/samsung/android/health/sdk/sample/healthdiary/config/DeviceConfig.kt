package com.samsung.android.health.sdk.sample.healthdiary.config

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

/**
 * Device and IP configuration loader
 * Manages watch pairing, network endpoints, and device-specific settings
 */
object DeviceConfig {
    
    private const val PREFS_NAME = "device_config"
    private const val KEY_WATCH_IP = "watch_ip_address"
    private const val KEY_WATCH_PORT = "watch_port"
    private const val KEY_WATCH_PAIRED = "watch_paired"
    private const val KEY_BOUND_NODE_ID = "bound_node_id"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"
    
    private const val DEFAULT_WATCH_PORT = 8080
    
    private var prefs: SharedPreferences? = null
    private var deviceId: String? = null
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
    
    /**
     * Get the watch IP address if configured
     */
    fun getWatchIpAddress(): String? {
        return prefs?.getString(KEY_WATCH_IP, null)
    }
    
    /**
     * Get the watch port
     */
    fun getWatchPort(): Int {
        return prefs?.getInt(KEY_WATCH_PORT, DEFAULT_WATCH_PORT) ?: DEFAULT_WATCH_PORT
    }
    
    /**
     * Get the unique device ID
     */
    fun getDeviceId(): String {
        return deviceId ?: "unknown_device"
    }
    
    fun getBoundNodeId(): String? {
        return prefs?.getString(KEY_BOUND_NODE_ID, null)
    }

    fun setBoundNodeId(nodeId: String?) {
        if (nodeId == null) {
            prefs?.edit()?.remove(KEY_BOUND_NODE_ID)?.apply()
        } else {
            prefs?.edit()?.putString(KEY_BOUND_NODE_ID, nodeId)?.apply()
        }
    }

    /**
     * Get watch connection status
     */
    fun getWatchConnectionStatus(): ConnectionStatus {
        val isPaired = prefs?.getBoolean(KEY_WATCH_PAIRED, false) ?: false
        val hasIp = !getWatchIpAddress().isNullOrEmpty()
        
        return when {
            isPaired && hasIp -> ConnectionStatus.CONNECTED
            hasIp -> ConnectionStatus.CONFIGURED
            else -> ConnectionStatus.DISCONNECTED
        }
    }
    
    /**
     * Save watch configuration
     */
    fun saveWatchConfig(ip: String, port: Int = DEFAULT_WATCH_PORT) {
        prefs?.edit()?.apply {
            putString(KEY_WATCH_IP, ip)
            putInt(KEY_WATCH_PORT, port)
            putBoolean(KEY_WATCH_PAIRED, true)
            apply()
        }
    }
    
    /**
     * Clear watch configuration
     */
    fun clearWatchConfig() {
        prefs?.edit()?.apply {
            remove(KEY_WATCH_IP)
            remove(KEY_WATCH_PORT)
            putBoolean(KEY_WATCH_PAIRED, false)
            apply()
        }
    }
    
    /**
     * Update last sync timestamp
     */
    fun updateLastSyncTime() {
        prefs?.edit()?.putLong(KEY_LAST_SYNC, System.currentTimeMillis())?.apply()
    }
    
    /**
     * Get last sync timestamp
     */
    fun getLastSyncTime(): Long {
        return prefs?.getLong(KEY_LAST_SYNC, 0L) ?: 0L
    }
    
    /**
     * Get full watch endpoint URL
     */
    /**
     * Get full watch endpoint URL
     */
    fun getWatchEndpoint(): String? {
        val ip = getWatchIpAddress() ?: return null
        val port = getWatchPort()
        return "http://$ip:$port"
    }

    private const val KEY_API_BASE_URL = "api_base_url"
    private const val DEFAULT_API_BASE_URL = " https://73244b572435.ngrok-free.app"

    fun getApiBaseUrl(): String {
        return prefs?.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL
    }

    fun setApiBaseUrl(url: String) {
        prefs?.edit()?.putString(KEY_API_BASE_URL, url)?.apply()
    }
}

enum class ConnectionStatus {
    CONNECTED,
    CONFIGURED,
    DISCONNECTED
}
