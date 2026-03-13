# Android OTA Update System Architecture

This document outlines the architecture for the Over-The-Air (OTA) update system implemented in the Hacking Health application.

## 1. Architecture Overview

The system follows a **Clean Architecture** approach with **MVVM** (Model-View-ViewModel) pattern, leveraging modern Android components and **GitHub Releases** as the update distribution platform.

```mermaid
graph TD
    subgraph Presentation Layer
        UI[UpdateDialog (Compose)] --> VM[UpdateViewModel]
        Notify[UpdateWorker (WorkManager)] --> VM
    end

    subgraph Domain Layer
        VM --> CheckUseCase[CheckForUpdatesUseCase]
        VM --> DownloadUseCase[AppUpdateManager]
        VM --> WearUseCase[WearUpdateManager]
    end

    subgraph Data Layer
        CheckUseCase --> Repo[UpdateRepository]
        Repo --> GitHubAPI[GitHubReleaseApi (Retrofit)]
        GitHubAPI --> MobileReleases[GitHub: hacking-health-app/releases]
        GitHubAPI --> WatchReleases[GitHub: hacking-health-watch-app/releases]
        
        DownloadUseCase --> DM[Android DownloadManager]
        WearUseCase --> WearAPI[Wearable Data Layer API]
    end
```

### Components

*   **UpdateViewModel**: Manages the UI state (Idle, Checking, Downloading, ReadyToInstall, Error). It orchestrates the flow based on user interaction or background checks.
*   **CheckForUpdatesUseCase**: Encapsulates the logic for fetching version info and comparing semantic versions (`1.0.0` vs `1.1.0`).
*   **UpdateRepository**: Fetches latest releases from GitHub API in parallel for both mobile and watch apps.
*   **GitHubReleaseApi**: Retrofit interface for GitHub Releases API (`api.github.com/repos/{owner}/{repo}/releases/latest`).
*   **AppUpdateManager**: Handles the platform-specific logic for downloading files via `DownloadManager` and triggering installation via `Intent`.
*   **WearUpdateManager**: Handles transferring the APK to the connected Wear OS device using `ChannelClient`.
*   **UpdateWorker**: A periodic background task (every 24h) that checks for updates and shows a notification if one is available.

## 2. GitHub Releases Integration

### Release Repositories

| App | Repository | Releases URL |
|-----|------------|--------------|
| Mobile | `TeoEchavarria/hacking-health-app` | https://github.com/TeoEchavarria/hacking-health-app/releases |
| Watch | `TeoEchavarria/hacking-health-watch-app` | https://github.com/TeoEchavarria/hacking-health-watch-app/releases |

### API Endpoints Used

```
GET https://api.github.com/repos/TeoEchavarria/hacking-health-app/releases/latest
GET https://api.github.com/repos/TeoEchavarria/hacking-health-watch-app/releases/latest
```

### Version Extraction

- Version is extracted from `tag_name` (e.g., `v1.2.3` → `1.2.3`)
- APK URL from `assets[].browser_download_url` where asset name ends with `.apk`
- Force update detected via tag suffix `-force` or `[FORCE]` in release body

### Automated Releases (GitHub Actions)

Both repositories have a `.github/workflows/release.yml` that:

1. Triggers on tag push (`v*`)
2. Builds the APK with `./gradlew assembleRelease`
3. Signs using repository secrets
4. Creates a GitHub Release with the APK as asset

**To publish a new release:**

```bash
# Update versionCode and versionName in app/build.gradle
git add -A && git commit -m "Release v1.2.0"
git tag v1.2.0
git push origin main --tags
```

**Required Repository Secrets:**

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

To generate `KEYSTORE_BASE64`:
```bash
base64 -i release.keystore | pbcopy  # macOS
base64 release.keystore | xclip -selection clipboard  # Linux
```

## 3. Permissions & Manifest

The following permissions are required and have been added:

*   `INTERNET`: For fetching update info.
*   `REQUEST_INSTALL_PACKAGES`: To trigger the installation of the downloaded APK.
*   `FOREGROUND_SERVICE`: For reliable Wear OS transfers (if needed in future expansions).
*   `FileProvider`: To securely share the downloaded APK file URI with the package installer.

## 4. Security Considerations

1.  **Trusted Source**:
    *   GitHub serves as a trusted source for APKs.
    *   Downloads use `browser_download_url` which goes through GitHub's CDN.
    *   SHA-256 verification is optional (available but not required since GitHub is trusted).

2.  **HTTPS**:
    *   All network communication is done over HTTPS (enforced by Android's default security config).

3.  **FileProvider**:
    *   We use `FileProvider` to grant temporary read access to the APK file for the installer, rather than using world-readable files.

## 5. User Experience (UX)

*   **Non-Intrusive**: Updates are checked silently in the background.
*   **Dialog**: When the app is opened (or via notification), a dialog prompts the user.
*   **Progress**: A progress bar shows the download status.
*   **Force Update**: If the release is tagged with `-force` suffix or body contains `[FORCE]`, the dialog is not dismissible.

## 6. Wear OS Updates

*   The phone app acts as the "downloader" for the watch.
*   The APK is downloaded to the phone storage.
*   `WearUpdateManager` opens a high-bandwidth channel (`ChannelClient`) to the watch to transfer the APK.
*   Once transferred, a message is sent to the watch app (which must have a corresponding listener) to trigger its own self-update or installation process.

## 7. Edge Cases & Handling

*   **No Internet**: `UpdateRepository` returns failure, UI shows a generic error or stays idle.
*   **Download Failure**: `DownloadManager` status is checked; errors are propagated to the UI.
*   **GitHub API Rate Limit**: Unauthenticated requests are limited to 60/hour. For higher limits, add a GitHub token.
*   **No Release Found**: Repository returns version `0.0.0` as fallback.
*   **Battery**: Background checks are scheduled with `WorkManager`, which respects battery optimization (Doze mode).

## 8. Advantages of GitHub Releases

| Benefit | Description |
|---------|-------------|
| Free CDN | GitHub provides global CDN for downloads |
| Versioning | Automatic version history and changelogs |
| Automation | GitHub Actions builds and publishes on tag |
| Transparency | Open source apps can share release notes |
| No Backend | No need to maintain update server |

## 9. File Structure

```
update/
├── data/
│   ├── api/
│   │   ├── GitHubReleaseApi.kt    # GitHub API interface
│   │   └── UpdateApi.kt           # (deprecated) Backend API
│   ├── model/
│   │   ├── GitHubModels.kt        # GitHub response DTOs
│   │   └── UpdateModels.kt        # Internal models
│   └── repository/
│       └── UpdateRepository.kt    # Fetches from GitHub
├── di/
│   └── UpdateModule.kt            # Hilt DI module
├── domain/
│   └── usecase/
│       ├── CheckForUpdatesUseCase.kt
│       └── VerifyUpdateUseCase.kt # Optional SHA verification
├── manager/
│   ├── AppUpdateManager.kt        # Download & install
│   └── WearUpdateManager.kt       # Watch transfer
├── ui/
│   ├── UpdateDialog.kt            # Compose UI
│   └── UpdateViewModel.kt
├── util/
│   └── VersionUtils.kt            # Version comparison
└── worker/
    └── UpdateWorker.kt            # Background checks
```
