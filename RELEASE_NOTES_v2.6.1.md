# StreamLux Native — Release Document

**Version:** 2.6.1  
**Version Code:** 40  
**Release Date:** June 2, 2026  
**Build Status:** ✅ BUILD SUCCESSFUL  

---

## 📦 Build Artifact

| Property | Value |
|---|---|
| **File** | `app-release.aab` |
| **Location** | `app/build/outputs/bundle/release/app-release.aab` |
| **Size** | 18.57 MB |
| **Signed** | ✅ Yes — `release-key.jks` / alias `streamlux-key` |
| **Min SDK** | 24 (Android 7.0 Nougat) |
| **Target SDK** | 35 (Android 15) |
| **Application ID** | `com.streamlux.app.mobile` |
| **Build Type** | Release (R8 minified + ProGuard) |

---

## 🆕 Changelog

### v2.6.1 — Crash Fix + Fallback Server System

#### 🐛 Critical Bug Fix
- **Fixed player crash** when opening any Movie, TV Show, or Live TV stream.
  - Root Cause: `collectAsState()` and `remember {}` were called conditionally inside an `if` block in `VideoPlayerScreen.kt`, violating Jetpack Compose Rules of Hooks.
  - Fix: Hoisted all state declarations to top level of the composable.

#### 🔊 New Feature: Servers / Fix Audio Fallback
- Added a **"Servers / Fix Audio"** button in the player overlay.
- Solves silent audio on sources like VidKing (AC3/Dolby codec not supported by Android WebView).
- **4 server sources available:**

| Server | Provider |
|---|---|
| VidEasy (Primary) | `player.videasy.net` |
| VidSrc.me (Fallback 1) | `vidsrc.me` |
| VidSrc.to (Fallback 2) | `vidsrc.to` |
| SuperEmbed (Fallback 3) | `multiembed.mov` |

---

## 🔐 Signing Info

| Property | Value |
|---|---|
| Keystore | `release-key.jks` |
| Key Alias | `streamlux-key` |
| SHA-256 | `BB:D7:66:00:EB:40:50:14:16:23:CC:25:CD:5F:EA:DD:F4:52:0F:BC:DD:CC:4B:29:94:18:5A:EF:4B:AC:1B:F3` |

---

## 📤 Google Play Upload

1. Go to [Google Play Console](https://play.google.com/console)
2. Select **StreamLux** → **Release → Production**
3. Click **Create new release**
4. Upload: `app/build/outputs/bundle/release/app-release.aab`
5. Set release name: **2.6.1**
6. Paste release notes (below) → **Start rollout**

### Store Release Notes
```
v2.6.1 — Bug Fixes & Improvements

• Fixed a crash that occurred when opening Movies, TV Shows, or Live TV
• Added "Servers / Fix Audio" button in the player — tap the screen while watching to switch between 4 different streaming servers if you encounter audio issues
• Improved player stability and reliability
```

---

## 📁 Files Changed

| File | Change |
|---|---|
| `VideoPlayerScreen.kt` | Fixed Compose hooks crash; added server dropdown UI |
| `VideoPlayerViewModel.kt` | Added `serverList` StateFlow and `switchServer()` |
| `GenericUrlFactory.kt` | Added 4 named server providers |
| `app/build.gradle.kts` | versionCode 40, versionName 2.6.1 |
| `gradle-wrapper.properties` | Updated Gradle to 8.4 |
