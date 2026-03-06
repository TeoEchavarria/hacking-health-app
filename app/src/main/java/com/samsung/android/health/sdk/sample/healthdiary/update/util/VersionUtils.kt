package com.samsung.android.health.sdk.sample.healthdiary.update.util

object VersionUtils {
    /**
     * Compare semantic versions.
     * Returns > 0 if v1 > v2
     * Returns < 0 if v1 < v2
     * Returns 0 if v1 == v2
     */
    fun compare(v1: String, v2: String): Int {
        val v1Parts = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = v2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(v1Parts.size, v2Parts.size)
        
        for (i in 0 until length) {
            val part1 = v1Parts.getOrElse(i) { 0 }
            val part2 = v2Parts.getOrElse(i) { 0 }
            
            if (part1 > part2) return 1
            if (part1 < part2) return -1
        }
        return 0
    }

    fun isUpdateAvailable(currentVersion: String, remoteVersion: String): Boolean {
        return compare(remoteVersion, currentVersion) > 0
    }
}
