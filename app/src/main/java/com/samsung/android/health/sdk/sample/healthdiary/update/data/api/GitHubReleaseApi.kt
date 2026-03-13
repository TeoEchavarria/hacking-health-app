package com.samsung.android.health.sdk.sample.healthdiary.update.data.api

import com.samsung.android.health.sdk.sample.healthdiary.update.data.model.GitHubRelease
import retrofit2.Response
import retrofit2.http.GET

/**
 * GitHub Releases API interface.
 * Used to check for app updates from GitHub Releases.
 */
interface GitHubReleaseApi {
    
    companion object {
        const val BASE_URL = "https://api.github.com/"
        const val MOBILE_REPO = "TeoEchavarria/hacking-health-app"
        const val WATCH_REPO = "TeoEchavarria/hacking-health-watch-app"
    }
    
    /**
     * Get the latest release for the mobile app.
     */
    @GET("repos/$MOBILE_REPO/releases/latest")
    suspend fun getMobileLatestRelease(): Response<GitHubRelease>
    
    /**
     * Get the latest release for the watch app.
     */
    @GET("repos/$WATCH_REPO/releases/latest")
    suspend fun getWatchLatestRelease(): Response<GitHubRelease>
}
