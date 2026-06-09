# StreamLux v2.7.1 Release Notes

## Overview
Version 2.7.1 completes the download experience introduced in 2.7.0. Direct CDN and VidVault now work as clear **Option 1** and **Option 2**, and VidVault downloads sync to Android notifications and the StreamLux library.

## What's New

### Option 1 + Option 2 Download Flow
- **Option 1 — Direct CDN:** scans Vyla servers for direct download links (1080p, 720p, etc.).
- **Option 2 — VidVault Portal:** always available in the same dialog — even when Option 1 finds links.
- If Option 1 fails, a clear message is shown with a one-tap path to VidVault instead of crashing or leaving the app.

### VidVault In-App Downloads
- VidVault runs inside a full-screen in-app WebView (not the system browser).
- TMDB movie/TV details are pre-filled on `https://vidvault.ru/`.
- Tapping download on VidVault triggers Android **DownloadManager**:
  - Progress appears in the **notification panel**.
  - The title is recorded in the **StreamLux Library** with `downloading` status.
- Blob/data downloads from the site are also handled in-app.

### Sports & Playback (carried from 2.7.0)
- Sports matches open **https://streamlux.vercel.app/sports/arena/{matchId}** in the in-app WebView.
- **VidKing** default with 16 fallback streaming servers.

## Bug Fixes
- Highlight videos on the sports page now play in-app via the WebView player.
- Download dialog wrapped in try/catch so empty CDN results never crash the app.

## Build Info
- **Version Code:** 271
- **Version Name:** 2.7.1
- **Package:** `com.streamlux.app.mobile`
- **Artifacts:** `app-release.apk` and `app-release.aab`
- **Output paths:**
  - `app/build/outputs/apk/release/app-release.apk`
  - `app/build/outputs/bundle/release/app-release.aab`

## Install Notes
- **APK:** sideload directly on Android phones/tablets/TV.
- **AAB:** upload to Google Play Console for store distribution.
