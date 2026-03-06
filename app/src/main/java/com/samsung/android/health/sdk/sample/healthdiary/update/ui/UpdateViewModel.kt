package com.samsung.android.health.sdk.sample.healthdiary.update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig
import com.samsung.android.health.sdk.sample.healthdiary.update.domain.usecase.CheckForUpdatesUseCase
import com.samsung.android.health.sdk.sample.healthdiary.update.domain.usecase.VerifyUpdateUseCase
import com.samsung.android.health.sdk.sample.healthdiary.update.manager.AppUpdateManager
import com.samsung.android.health.sdk.sample.healthdiary.update.manager.DownloadStatus
import com.samsung.android.health.sdk.sample.healthdiary.update.manager.TransferStatus
import com.samsung.android.health.sdk.sample.healthdiary.update.manager.WearUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import com.samsung.android.health.sdk.sample.healthdiary.update.data.model.VersionInfo

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpdateAvailable(
        val versionInfo: VersionInfo,
        val isMobile: Boolean
    ) : UpdateUiState()
    data class Downloading(val progress: Int) : UpdateUiState()
    data class ReadyToInstall(val file: File) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
    object Completed : UpdateUiState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val checkForUpdatesUseCase: CheckForUpdatesUseCase,
    private val appUpdateManager: AppUpdateManager,
    private val wearUpdateManager: WearUpdateManager,
    private val verifyUpdateUseCase: VerifyUpdateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            val result = checkForUpdatesUseCase(BuildConfig.VERSION_NAME)
            
            result.onSuccess { updateResponse ->
                if (updateResponse != null) {
                    _uiState.value = UpdateUiState.UpdateAvailable(
                        versionInfo = updateResponse.mobile,
                        isMobile = true
                    )
                } else {
                    _uiState.value = UpdateUiState.Idle
                }
            }.onFailure {
                _uiState.value = UpdateUiState.Error(it.message ?: "Failed to check for updates")
            }
        }
    }

    fun startDownload(versionInfo: VersionInfo, isMobile: Boolean) {
        val fileName = if (isMobile) "update_mobile_${versionInfo.version}.apk" else "update_watch_${versionInfo.version}.apk"
        
        viewModelScope.launch {
            appUpdateManager.downloadUpdate(versionInfo.url, fileName).collect { status ->
                when (status) {
                    is DownloadStatus.Downloading -> {
                        val progress = if (isMobile) status.progress else status.progress / 2
                        _uiState.value = UpdateUiState.Downloading(progress)
                    }
                    is DownloadStatus.Completed -> {
                        if (versionInfo.sha256 != null && !verifyUpdateUseCase(status.file, versionInfo.sha256)) {
                            _uiState.value = UpdateUiState.Error("Update verification failed (SHA256 mismatch)")
                        } else {
                            if (isMobile) {
                                _uiState.value = UpdateUiState.ReadyToInstall(status.file)
                                appUpdateManager.installUpdate(status.file)
                            } else {
                                // Send to watch
                                wearUpdateManager.sendUpdateToWatch(status.file).collect { transferStatus ->
                                     when (transferStatus) {
                                         is TransferStatus.Sending -> _uiState.value = UpdateUiState.Downloading(50 + (transferStatus.progress / 2))
                                         is TransferStatus.Completed -> _uiState.value = UpdateUiState.Completed
                                         is TransferStatus.Error -> _uiState.value = UpdateUiState.Error(transferStatus.message)
                                         TransferStatus.Idle -> {}
                                     }
                                }
                            }
                        }
                    }
                    is DownloadStatus.Error -> _uiState.value = UpdateUiState.Error(status.message)
                    DownloadStatus.Idle -> {}
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = UpdateUiState.Idle
    }

    fun installUpdate(file: File) {
        appUpdateManager.installUpdate(file)
    }
}
