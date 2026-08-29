# Changelog

All notable changes to Silema · Are You Dead Yet? will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Multi-language support: English, 简体中文, 日本語
- Wear OS main product with 5 screens (Home, Entry, SOS, Workout, AI Brief)
- Phone guardian app modernized (removed elderly self-use screens)
- Three-platform deployment: GitHub, Gitee, GitLab
- CONTRIBUTING.md with detailed guidelines
- ISSUE_TEMPLATE for bugs and features
- PULL_REQUEST_TEMPLATE
- FUNDING.yml

### Changed
- README.md completely rewritten with professional branding
- All UI strings externalized to resources files
- Enhanced SEO keywords in README

### Fixed
- Hardcoded Chinese text replaced with string resources
- Inconsistent naming across modules

## [v0.5.0] - 2026-08-29

### Added
- Dual-end architecture finalized: Watch (main product) + Phone (guardian only)
- Wear OS: 5 complete screens calling `:core` module
  - HomeScreen: Real RiskEngine assessment + recent records
  - EntryScreen: Picker-based vital entry for 6 types
  - SosScreen: 120dp emergency button with family notification
  - WorkoutScreen: Steps tracking + weekly count + duration picker
  - AiBriefScreen: Weekly report + risk level display
- Phone app: Modern Material 3 design for caregivers
  - DashboardScreen: Status banner + vital cards + trends
  - ReportScreen: Trends/weekly/sleep tabs
  - FamilyScreen: Remote monitoring (local mode honest empty state)
  - DevicesScreen: PPG/BLE/IoT device management
  - MoreScreen: Unified function hub
- Shared `:core` module (Kotlin-JVM pure library)
  - RiskEngine rule engine
  - Stats/VitalsMath statistical models
  - HealthReport generation
  - FhirExporter for medical integration
  - Data models: VitalRecord, VitalType, Workout

### Changed
- Phone app repositioned as pure guardian end (no elderly self-use screens)
- Removed EntryScreen, WorkoutScreen, SosScreen from phone module (eliminated dead code)
- Algorithm gate verified: **34/34 tests passing**
  - RiskEngine: 6 tests
  - Features: 10 tests
  - Stats: 18 tests

### Security
- Release signing configured with certificate `CN=Silema AreYouDead`
- Generated signed APKs:
  - `silema-v0.5.0-release.apk` (Phone, 12.9MB)
  - `silema-wear-v0.5.0-release.apk` (Watch, 16.9MB)

### Known Limitations
- Watch SOS real delivery to family: Requires future Bluetooth `DataClient` sync
- Remote real-time monitoring: `FamilyScreen` local mode honest empty state, needs enterprise backend

## [v0.4.x] - Previous Versions

### Core Features Established
- RiskEngine with 4-level medical threshold triage
- Statistical models: z-score, trend regression, MAP, SI, PP
- Camera PPG heart rate measurement
- Bluetooth LE device support (0x180D, 0x1810, 0x1822)
- Health Connect integration
- FHIR R4 export
- GPS activity tracking
- Weekly health reports
- Sleep tracking
- Stress index calculation
- Smart reminders (WorkManager)
- One-tap SOS
- Elderly-first UI design
- Offline-first architecture
- Pluggable remote backend

---

## Version Numbering

This project uses semantic versioning: `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes (e.g., API changes, data model migrations)
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes and minor improvements

## Release Notes Format

Each release should include:
1. **Added**: New features
2. **Changed**: Changes to existing functionality
3. **Deprecated**: Soon-to-be removed features
4. **Removed**: Removed features
5. **Fixed**: Bug fixes
6. **Security**: Security improvements

For more details, see [CONTRIBUTING.md](CONTRIBUTING.md).
