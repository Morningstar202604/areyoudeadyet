# Deployment Guide

This guide explains how to build and release Silema · Are You Dead Yet? on GitHub.

## Platform

| Platform | URL | Status |
|----------|-----|--------|
| **GitHub** | https://github.com/Morningstar202604/areyoudeadyet | Active (primary) |

## CI/CD

GitHub Actions runs `ci.yml` on every push to `main` and on every pull request:

- Sets up JDK 17 and the Android SDK
- Caches Gradle wrapper and dependencies
- Builds and runs the test suite (algo + unit tests)

## Release Process

1. **Update version** in `build.gradle.kts` files (`app`, `core`, `wear`)
2. **Update CHANGELOG.md** with new release notes
3. **Run tests locally**: `./gradlew runAlgoTests` (must pass)
4. **Build release APKs**:

   ```bash
   ./gradlew :app:assembleRelease
   ./gradlew :wear:assembleRelease
   ```

5. **Sign APKs** with the release keystore
6. **Create a GitHub Release** with the changelog and attach both APKs
7. **Push the release tag**:

   ```bash
   git push origin main --tags
   ```

## Authentication

- GitHub HTTPS pushes use a personal access token or SSH key
- Configure your Git credential manager once:
  `git config --global credential.helper store`

## Troubleshooting

### "Updates were rejected"
- Pull first: `git pull --rebase origin main`, then push again.

### "Authentication failed"
- Update credentials: `git remote set-url origin <new-url>`, or configure SSH keys.

---

**Maintainer:** Morningstar202604