package com.samsung.android.health.sdk.sample.healthdiary.update.data.repository

import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.update.data.api.GitHubReleaseApi
import com.samsung.android.health.sdk.sample.healthdiary.update.data.model.GitHubRelease
import com.samsung.android.health.sdk.sample.healthdiary.update.data.model.UpdateResponse
import com.samsung.android.health.sdk.sample.healthdiary.update.data.model.VersionInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

interface UpdateRepository {
    suspend fun getUpdates(): Result<UpdateResponse>
}

/**
 * Repository that fetches update information from GitHub Releases API.
 * 
 * Mobile releases: https://github.com/TeoEchavarria/hacking-health-app/releases
 * Watch releases: https://github.com/TeoEchavarria/hacking-health-watch-app/releases
 */
@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val gitHubApi: GitHubReleaseApi
) : UpdateRepository {
    
    companion object {
        private const val TAG = "UpdateRepository"
    }
    
    override suspend fun getUpdates(): Result<UpdateResponse> = coroutineScope {
        try {
            // Fetch both releases in parallel
            val mobileDeferred = async { gitHubApi.getMobileLatestRelease() }
            val watchDeferred = async { gitHubApi.getWatchLatestRelease() }
            
            val mobileResponse = mobileDeferred.await()
            val watchResponse = watchDeferred.await()
            
            // Parse mobile release
            val mobileVersionInfo = if (mobileResponse.isSuccessful && mobileResponse.body() != null) {
                mapGitHubReleaseToVersionInfo(mobileResponse.body()!!)
            } else {
                Log.w(TAG, "Failed to fetch mobile release: ${mobileResponse.code()} ${mobileResponse.message()}")
                // Return a fallback indicating no update available
                VersionInfo(version = "0.0.0", url = "", force = false)
            }
            
            // Parse watch release
            val watchVersionInfo = if (watchResponse.isSuccessful && watchResponse.body() != null) {
                mapGitHubReleaseToVersionInfo(watchResponse.body()!!)
            } else {
                Log.w(TAG, "Failed to fetch watch release: ${watchResponse.code()} ${watchResponse.message()}")
                VersionInfo(version = "0.0.0", url = "", force = false)
            }
            
            Log.i(TAG, "Fetched updates - Mobile: ${mobileVersionInfo.version}, Watch: ${watchVersionInfo.version}")
            
            Result.success(UpdateResponse(
                mobile = mobileVersionInfo,
                watch = watchVersionInfo
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching updates from GitHub", e)
            Result.failure(e)
        }
    }
    
    /**
     * Maps a GitHub Release to our internal VersionInfo model.
     */
    private fun mapGitHubReleaseToVersionInfo(release: GitHubRelease): VersionInfo {
        val apkAsset = release.findApkAsset()
        
        return VersionInfo(
            version = release.getVersion(),
            url = apkAsset?.browserDownloadUrl ?: "",
            force = release.isForceUpdate(),
            sha256 = null // GitHub is trusted source, no SHA verification needed
        )
    }
}
