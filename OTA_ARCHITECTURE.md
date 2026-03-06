# Android OTA Update System Architecture

This document outlines the architecture for the Over-The-Air (OTA) update system implemented in the Hacking Health application.

## 1. Architecture Overview

The system follows a **Clean Architecture** approach with **MVVM** (Model-View-ViewModel) pattern, leveraging modern Android components.

```mermaid
graph TD
    subgraph Presentation Layer
        UI[UpdateDialog (Compose)] --> VM[UpdateViewModel]
        Notify[UpdateWorker (WorkManager)] --> VM
    end

    subgraph Domain Layer
        VM --> CheckUseCase[CheckForUpdatesUseCase]
        VM --> DownloadUseCase[AppUpdateManager]
        VM --> VerifyUseCase[VerifyUpdateUseCase]
        VM --> WearUseCase[WearUpdateManager]
    end

    subgraph Data Layer
        CheckUseCase --> Repo[UpdateRepository]
        Repo --> API[UpdateApi (Retrofit)]
        API --> Backend[Backend /app/updates]
        
        DownloadUseCase --> DM[Android DownloadManager]
        WearUseCase --> WearAPI[Wearable Data Layer API]
    end
```

### Components

*   **UpdateViewModel**: Manages the UI state (Idle, Checking, Downloading, ReadyToInstall, Error). It orchestrates the flow based on user interaction or background checks.
*   **CheckForUpdatesUseCase**: Encapsulates the logic for fetching version info and comparing semantic versions (`1.0.0` vs `1.1.0`).
*   **UpdateRepository**: Abstraction over the network API.
*   **AppUpdateManager**: Handles the platform-specific logic for downloading files via `DownloadManager` and triggering installation via `Intent`.
*   **WearUpdateManager**: Handles transferring the APK to the connected Wear OS device using `ChannelClient`.
*   **UpdateWorker**: A periodic background task (every 24h) that checks for updates and shows a notification if one is available.

## 2. Permissions & Manifest

The following permissions are required and have been added:

*   `INTERNET`: For fetching update info.
*   `REQUEST_INSTALL_PACKAGES`: To trigger the installation of the downloaded APK.
*   `FOREGROUND_SERVICE`: For reliable Wear OS transfers (if needed in future expansions).
*   `FileProvider`: To securely share the downloaded APK file URI with the package installer.

## 3. Security Considerations

1.  **SHA-256 Verification**:
    *   The backend provides a `sha256` hash for the APK.
    *   Before installation, `VerifyUpdateUseCase` calculates the hash of the downloaded file and compares it.
    *   If the hash mismatches, the update is aborted to prevent tampering or corruption.

2.  **HTTPS**:
    *   All network communication is done over HTTPS (enforced by Android's default security config).

3.  **FileProvider**:
    *   We use `FileProvider` to grant temporary read access to the APK file for the installer, rather than using world-readable files.

## 4. User Experience (UX)

*   **Non-Intrusive**: Updates are checked silently in the background.
*   **Dialog**: When the app is opened (or via notification), a dialog prompts the user.
*   **Progress**: A progress bar shows the download status.
*   **Force Update**: If the backend flags `force: true`, the dialog is not dismissible, ensuring critical updates are applied.

## 5. Wear OS Updates

*   The phone app acts as the "downloader" for the watch.
*   The APK is downloaded to the phone storage.
*   `WearUpdateManager` opens a high-bandwidth channel (`ChannelClient`) to the watch to transfer the APK.
*   Once transferred, a message is sent to the watch app (which must have a corresponding listener) to trigger its own self-update or installation process.

## 6. Edge Cases & Handling

*   **No Internet**: `UpdateRepository` returns failure, UI shows a generic error or stays idle.
*   **Download Failure**: `DownloadManager` status is checked; errors are propagated to the UI.
*   **Corrupted File**: SHA-256 check fails, installation is blocked.
*   **Battery**: Background checks are scheduled with `WorkManager`, which respects battery optimization (Doze mode).

## 7. Scaling Suggestions

*   **CDN**: Serve APKs via a CDN (e.g., Cloudfront, S3) instead of direct GitHub links for better performance and reliability.
*   **Differential Updates**: Implement smart updates (bsdiff) to download only the binary difference, saving bandwidth.
*   **Staged Rollouts**: Modify the backend to return new versions only to a subset of users (based on device ID hash) to test stability before full release.
