# StreamLux Native — Release Notes v2.5.0

**Build Date:** June 2, 2026  
**Version Code:** 38  
**Version Name:** 2.5.0  
**Application ID:** `com.streamlux.app.mobile`  
**Min SDK:** 24 (Android 7.0)  
**Target SDK:** 35 (Android 15)

---

## ✅ Build Artifact

| Property | Value |
|---|---|
| **File** | `app-release.aab` |
| **Size** | ~18.57 MB |
| **Location** | `app/build/outputs/bundle/release/app-release.aab` |
| **Signed** | ✅ Yes — with `release-key.jks` (`streamlux-key`) |
| **Build Type** | Release (R8 minified + ProGuard) |

---

## 🆕 What's New in v2.5.0

### 🔊 Audio Fallback — "Servers / Fix Audio" Feature
- **Root Cause Fixed:** Android WebView cannot decode AC3/Dolby audio tracks served by certain embed providers (e.g. VidKing). The video plays but audio is silent.
- **Solution:** Added a **"Servers / Fix Audio"** dropdown button in the player overlay (visible when controls are shown by tapping the screen).
- Users can instantly switch between 4 server sources without leaving the player:
  1. **VidEasy (Primary)** — `player.videasy.net` — the primary FMovies+ engine
  2. **VidSrc.me (Fallback 1)** — `vidsrc.me` — alternative with AAC audio
  3. **VidSrc.to (Fallback 2)** — `vidsrc.to` — another reliable fallback
  4. **SuperEmbed (Fallback 3)** — `multiembed.mov` — last-resort fallback

### 🛡️ Always-On Fallback Architecture
- `GenericUrlFactory.kt` now supports 4 named server keys: `VIDEASY`, `VIDSRC_ME`, `VIDSRC_TO`, `SUPEREMBED`
- `VideoPlayerViewModel.kt` now exposes a `serverList: StateFlow<List<ServerSource>>` and a `switchServer(index: Int)` function
- `VideoPlayerScreen.kt` dynamically shows the server menu only when there are multiple servers available (online mode only — hidden for offline playback)

---

## 🐛 Bug Fixes
- Fixed the Gradle wrapper version mismatch (upgraded from 8.2.1 → 8.4 to meet Android Gradle Plugin requirements)
- Resolved missing `local.properties` configuration for SDK and signing

---

## 📋 Known Warnings (Non-Breaking)
These are deprecation warnings only — they do not affect functionality:
- `ArrowBack` icon: use `Icons.AutoMirrored.Filled.ArrowBack` in a future update
- `statusBarColor` setter: deprecated in newer Android APIs
- Several unused parameter warnings in `SportsScreen`, `MusicScreen`, `LibraryScreen`

---

## 📦 Upload to Google Play

1. Go to [Google Play Console](https://play.google.com/console)
2. Select **StreamLux** → **Release** → **Production** (or Internal Testing)
3. Click **Create new release**
4. Upload: `app/build/outputs/bundle/release/app-release.aab`
5. Set version name **2.5.0** and release notes
6. Submit for review

---

## 🔐 Signing Info
| Property | Value |
|---|---|
| Keystore | `release-key.jks` |
| Key Alias | `streamlux-key` |
| Keystore Type | PKCS12 |
| SHA-256 Fingerprint | `BB:D7:66:00:EB:40:50:14:16:23:CC:25:CD:5F:EA:DD:F4:52:0F:BC:DD:CC:4B:29:94:18:5A:EF:4B:AC:1B:F3` |

> ⚠️ **Keep your keystore file and passwords safe!** Losing the keystore means you cannot update the app on Google Play.
