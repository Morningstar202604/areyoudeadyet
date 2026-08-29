# Multi-Platform Deployment Guide

This guide explains how to deploy Silema · Are You Dead Yet? to all three code hosting platforms.

## 🌐 Platforms

| Platform | URL | Status | Notes |
|----------|-----|--------|-------|
| **GitHub** | https://github.com/Morningstar202604/areyoudeadyet | ✅ Active | Primary platform |
| **Gitee** | https://gitee.com/Morningstar202604/areyoudeadyet | ⏳ Setup Required | For China mainland users |
| **GitCode** | https://gitcode.com/Morningstar202604/areyoudeadyet | ⏳ Setup Required | CSDN integrated |

## 📋 Initial Setup (One-Time)

### 1. GitHub (Already Configured)
```bash
# Already set up as 'origin'
git remote -v
# origin  https://github.com/Morningstar202604/areyoudeadyet.git
```

### 2. Gitee (码云)
1. Visit https://gitee.com/repositories/new
2. Create repository:
   - Name: `areyoudeadyet`
   - Visibility: Public
   - **DO NOT** initialize with README (we already have one)
3. After creation, push:
```bash
git push gitee main
```

### 3. GitCode
1. Visit https://gitcode.com/projects/new
2. Create project:
   - Name: `areyoudeadyet`
   - Visibility: Public
   - **DO NOT** initialize with README
3. After creation, push:
```bash
git push gitcode main
```

## 🚀 Daily Deployment

### Option 1: Push to All Platforms at Once

**Windows (PowerShell):**
```powershell
.\push-all.ps1
```

**Linux/macOS/Git Bash:**
```bash
./push-all.sh
```

### Option 2: Push Individually

```bash
# GitHub (primary)
git push origin main

# Gitee (China mirror)
git push gitee main

# GitCode (CSDN integration)
git push gitcode main
```

## 🔧 Remote Configuration Check

Verify your remotes are configured correctly:
```bash
git remote -v
```

Expected output:
```
origin    https://github.com/Morningstar202604/areyoudeadyet.git (fetch)
origin    https://github.com/Morningstar202604/areyoudeadyet.git (push)
gitee     https://gitee.com/Morningstar202604/areyoudeadyet.git (fetch)
gitee     https://gitee.com/Morningstar202604/areyoudeadyet.git (push)
gitcode   https://gitcode.com/Morningstar202604/areyoudeadyet.git (fetch)
gitcode   https://gitcode.com/Morningstar202604/areyoudeadyet.git (push)
```

## 🔐 Authentication

### GitHub
- Uses personal access token or SSH key
- Configure in Git credentials manager

### Gitee
- May require personal access token for HTTPS
- Generate at: https://gitee.com/profile/personal_access_tokens
- Or use SSH keys

### GitCode
- Integrated with CSDN account
- Use CSDN credentials or personal access token
- Generate at: https://gitcode.com/-/profile/personal_access_tokens

## 📦 Release Process

1. **Update version** in `build.gradle.kts` files
2. **Update CHANGELOG.md** with new release notes
3. **Run tests**: `./gradlew runAlgoTests` (must pass 34/34)
4. **Build release APKs**:
   ```bash
   ./gradlew :app:assembleRelease
   ./gradlew :wear:assembleRelease
   ```
5. **Sign APKs** with release keystore
6. **Create GitHub Release** with changelog and APKs
7. **Push to all platforms**:
   ```bash
   ./push-all.sh
   ```

## 🔄 Syncing Between Platforms

If one platform falls behind:

```bash
# Pull latest from GitHub
git pull origin main

# Push to other platforms
git push gitee main
git push gitcode main
```

## 🐛 Troubleshooting

### "Repository not found"
- Ensure you've created the repository on the platform first
- Check the URL is correct: `git remote -v`

### "Authentication failed"
- Update credentials: `git remote set-url <platform> <new-url-with-token>`
- Or configure SSH keys

### "Updates were rejected"
- Pull first: `git pull --rebase <platform> main`
- Then push again

### Slow push to Gitee/GitCode
- Normal for international connections
- Consider using VPN or waiting for better network conditions

## 📊 Platform-Specific Features

### GitHub
- Actions CI/CD (already configured)
- Issues & Projects
- Discussions
- Releases with assets

### Gitee
- Better access speed in China
- Gitee Pages for documentation
- Integrated with Chinese developer community

### GitCode
- Integrated with CSDN blog platform
- Good for reaching Chinese developers
- Automatic article generation from commits

## 🎯 Best Practices

1. **Always push to GitHub first** (primary platform)
2. **Verify GitHub Actions pass** before pushing to mirrors
3. **Use the push scripts** to ensure consistency
4. **Keep CHANGELOG.md updated** for each release
5. **Tag releases** on GitHub: `git tag v0.5.0 && git push origin --tags`

---

**Last Updated:** 2026-08-29  
**Maintainer:** Morningstar202604
