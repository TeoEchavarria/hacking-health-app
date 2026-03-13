# Copilot Instructions

## Protected Code - DO NOT MODIFY

The following files and systems are **locked** and should not be modified without explicit user approval:

### OTA Update System
The app update system is fully functional. **DO NOT** modify these files:

- `app/src/main/java/.../update/` - Entire update package
  - `UpdateViewModel.kt`
  - `UpdateDialog.kt`
  - `AppUpdateManager.kt`
  - `data/` - Update data models and repository
- `.github/workflows/release.yml` - CI/CD release workflow
- `app/build.gradle` - Signing configuration and version extraction logic (lines 10-55)

### Keystore & Signing
- The release signing uses environment variables (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)
- Dynamic versioning extracts from `VERSION_TAG` environment variable
- **versionCode formula:** `major * 10000 + minor * 100 + patch`
- Local development uses `config/release.keystore` via `dev_tools.sh`

## Safe to Modify

Everything else in the codebase is safe to modify, including:
- UI screens (except UpdateDialog)
- ViewModels (except UpdateViewModel)
- Data models
- API integrations
- Health data features

## Versioning

When creating new releases:
1. Update header version in `HomeScreen.kt` to match tag
2. Create git tag following `vX.Y.Z` format
3. Push tag to trigger GitHub Actions release
