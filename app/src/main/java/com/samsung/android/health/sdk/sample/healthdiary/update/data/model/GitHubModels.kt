package com.samsung.android.health.sdk.sample.healthdiary.update.data.model

import com.google.gson.annotations.SerializedName

/**
 * GitHub Release response from the GitHub API.
 * Endpoint: GET /repos/{owner}/{repo}/releases/latest
 */
data class GitHubRelease(
    @SerializedName("id") val id: Long,
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("prerelease") val prerelease: Boolean = false,
    @SerializedName("draft") val draft: Boolean = false,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("assets") val assets: List<GitHubAsset> = emptyList(),
    @SerializedName("html_url") val htmlUrl: String?
) {
    /**
     * Extract version from tag_name.
     * Handles both "v1.2.3" and "1.2.3" formats.
     */
    fun getVersion(): String {
        return tagName.removePrefix("v").removePrefix("V")
    }
    
    /**
     * Check if this is a force update release.
     * Convention: tag ends with "-force" or release body contains "[FORCE]"
     */
    fun isForceUpdate(): Boolean {
        return tagName.endsWith("-force", ignoreCase = true) ||
               body?.contains("[FORCE]", ignoreCase = true) == true
    }
    
    /**
     * Find the APK asset from the release assets.
     */
    fun findApkAsset(): GitHubAsset? {
        return assets.find { it.name.endsWith(".apk", ignoreCase = true) }
    }
}

/**
 * GitHub Release Asset (files attached to a release).
 */
data class GitHubAsset(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("content_type") val contentType: String?,
    @SerializedName("size") val size: Long,
    @SerializedName("download_count") val downloadCount: Int = 0,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)
