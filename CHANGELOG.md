# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- Added `LICENSE-PROPRIETARY.txt` with proprietary license for all new code and assets
- Added `NOTICE` file acknowledging Apache-2.0 licensed components (Gradle wrapper scripts)
- Added Licenses section to README.md explaining license separation

### Changed
- Rebranded application from "vitals-tracker" to "Hacking Health"
  - Updated app display name in `strings.xml`
  - Updated project name in `settings.gradle.kts`
  - Updated README.md title and references

### License
- **Proprietary Code**: All code in `app/`, `core/`, `data/`, `database/`, `domain/`, `presentation/`, and `includeBuild/` directories is now licensed under LICENSE-PROPRIETARY.txt (All Rights Reserved)
- **Third-Party Components**: Gradle wrapper scripts (`gradlew`, `gradlew.bat`) remain under Apache License 2.0 as originally licensed
- We preserve the Apache-2.0 license for all original code that included it. We are not removing or reclassifying third-party material without permission. To relicense the entire project as proprietary, we would require consent from the original authors.

