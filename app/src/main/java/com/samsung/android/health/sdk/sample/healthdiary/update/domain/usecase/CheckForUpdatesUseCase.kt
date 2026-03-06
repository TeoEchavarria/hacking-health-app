package com.samsung.android.health.sdk.sample.healthdiary.update.domain.usecase

import com.samsung.android.health.sdk.sample.healthdiary.update.data.model.UpdateResponse
import com.samsung.android.health.sdk.sample.healthdiary.update.data.repository.UpdateRepository
import com.samsung.android.health.sdk.sample.healthdiary.update.util.VersionUtils
import javax.inject.Inject

class CheckForUpdatesUseCase @Inject constructor(
    private val repository: UpdateRepository
) {
    /**
     * Checks for updates.
     * Returns the UpdateResponse only if an update is available (version > currentVersion).
     * If no update is available, returns null in success.
     * If update check fails, returns Result.failure.
     */
    suspend operator fun invoke(currentMobileVersion: String): Result<UpdateResponse?> {
        return repository.getUpdates().map { response ->
            val mobileUpdateAvailable = VersionUtils.isUpdateAvailable(currentMobileVersion, response.mobile.version)
            
            // We can also check watch updates here if we had the current watch version.
            // For now, let's just return the response if ANY update logic implies available.
            // But usually we want to trigger update flow if mobile is outdated.
            
            if (mobileUpdateAvailable) {
                response
            } else {
                null
            }
        }
    }
}
