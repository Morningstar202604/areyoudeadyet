## Working rules for this repository

* Dependency updates: search the whole repository for every occurrence of a dependency (build files, lockfiles, CI workflows, docs) before bumping. A partial bump — declaration updated but lockfile or a pinned action left behind — is the most common cause of "works locally, CI fails". Keep lockfiles in the same commit as the declaration. Move version-coupled toolchain upgrades together in one commit.
* Refactoring: pull latest main first, work on a fresh branch, keep commits atomic with messages that state the why, and always run the full check suite before pushing (for this repo: `./gradlew test && ./gradlew ktlintCheck`). A branch left behind main cannot be merged under the repository's branch protection.
* Merge conflicts: resolve conflicts in the working tree against the latest main; never force-push shared branches; never resolve a conflict by blindly taking either side — re-read both sides and keep both changes when they are both valid.
* Versioning: releases follow X.Y.Z starting at 0.0.0. Last digit = fixes, middle digit = feature work, first digit stays 0 until a stable release is declared. Bump the version in code, CHANGELOG.md and the tag in the same change.

---

# Contributing to Silema · Are You Dead Yet?

Thank you for your interest in contributing to our elderly health guardian project! This document provides guidelines and instructions for contributing.

## 🌟 Quick Start

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/YOUR_USERNAME/areyoudeadyet.git`
3. **Create a branch**: `git checkout -b feature/your-feature-name`
4. **Make changes** following the guidelines below
5. **Test** your changes (see Testing section)
6. **Commit** with clear messages
7. **Push** to your fork
8. **Submit a Pull Request**

## 📋 Development Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK Platform 34 (Android 14)
- Git

### Build Commands
```bash
# Phone app (Guardian)
./gradlew :app:assembleDebug

# Wear OS app (Main Product)
./gradlew :wear:assembleDebug

# Run algorithm tests
./gradlew :core:test
```

## 🧪 Testing Requirements

All PRs must pass the algorithm test suite:
```bash
./gradlew :core:test
```

Current status: **34/34 tests passing** covering:
- RiskEngine rules (6 tests)
- Feature calculations (10 tests)
- Statistical models (18 tests)

## 💻 Coding Standards

### Kotlin Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex algorithms (especially medical formulas)
- Keep functions small and focused

### Architecture Guidelines
- **`:core` module**: Pure Kotlin-JVM library, no Android dependencies
  - RiskEngine, Stats, VitalsMath, HealthReport, FhirExporter
  - Data models: VitalRecord, VitalType, Workout, etc.
- **`:app` module**: Phone guardian app (Android 8.0+, minSdk 26)
  - Modern Material 3 design
  - Caregiver-focused interface
- **`:wear` module**: Wear OS main product (Wear OS 4.0+, minSdk 30)
  - Elderly-first UI: large fonts, high contrast, big buttons
  - 5 screens: Home, Entry, SOS, Workout, AI Brief

### Important Rules
1. **Never modify core algorithms** without thorough testing and medical validation
2. **Keep `:core` pure**: No Android framework dependencies
3. **Respect offline-first**: All features must work without network
4. **Privacy first**: User data stays on device unless explicitly synced

## 🌍 Internationalization (i18n)

The app supports **English**, **简体中文**, and **日本語**.

### Adding New Strings
1. Add English string to `res/values/strings.xml`
2. Add Chinese translation to `res/values-zh/strings.xml`
3. Add Japanese translation to `res/values-ja/strings.xml`

### String Naming Convention
- Use descriptive prefixes: `dashboard_`, `entry_`, `sos_`, `common_`
- Example: `dashboard_status_normal`, `entry_save`, `common_cancel`

## 🎨 UI/UX Guidelines

### Phone App (Guardian)
- Modern Material 3 design
- Clean, professional look for caregivers
- Not elderly-styled (that's the watch's job)

### Wear OS App (Main Product)
- **Large fonts**: Minimum 16sp for body text
- **High contrast**: Ensure readability in bright sunlight
- **Big buttons**: Minimum 76dp touch targets
- **Simple navigation**: Max 3 taps to any feature
- **Voice announcements**: Auto-announce danger levels

## 🔬 Algorithm Contributions

We welcome improvements to:
- Medical threshold adjustments (with citations)
- New combination rules
- Signal processing enhancements (PPG)
- Statistical model refinements

### Required Documentation
For algorithm changes, provide:
1. Medical/scientific justification
2. Peer-reviewed references
3. Test cases demonstrating improvement
4. Backward compatibility analysis

## 📱 Device Protocol Support

We're actively seeking contributions for:
- Glucose meters (Bluetooth LE)
- Smart thermometers
- Digital scales
- Fall detection sensors
- Additional wearable brands

### Protocol Implementation
1. Study the device's Bluetooth GATT specification
2. Implement parser in `:app:ble` or `:wear:ble`
3. Add test cases with real device data
4. Document pairing procedure

## 🚀 Release Process

1. Update version in `build.gradle.kts`
2. Update `CHANGELOG.md`
3. Run full test suite
4. Build release APKs:
   ```bash
   ./gradlew :app:assembleRelease
   ./gradlew :wear:assembleRelease
   ```
5. Sign APKs with release keystore
6. Create GitHub release with changelog
7. Push to all three remotes (GitHub, Gitee, GitLab)

## 📝 Commit Message Guidelines

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Tests
- `chore`: Maintenance

Example:
```
feat(wear): add Japanese localization to SOS screen

- Add string resources for ja locale
- Update SosScreen.kt to use stringResource()
- Verify button text displays correctly

Closes #42
```

## 🤝 Code Review Process

1. At least one maintainer must approve
2. All CI checks must pass
3. Algorithm tests must pass (34/34)
4. No breaking changes to public APIs without migration plan
5. Documentation updated if needed

## 📞 Getting Help

- **Questions?** Open an issue with label `question`
- **Bugs?** Use the bug report template
- **Features?** Use the feature request template
- **Chat?** Join our community discussions

## 🙏 Recognition

Contributors will be:
- Listed in README.md contributors section
- Credited in release notes
- Invited to core team for significant contributions

## 📜 License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

**Thank you for helping make elderly care better through technology!** ❤️
