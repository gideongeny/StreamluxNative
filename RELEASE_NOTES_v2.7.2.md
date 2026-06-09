# StreamLux v2.7.2 Release Notes

## Overview
Stability release: fixes download-dialog crashes, VidVault reliability on Android, and settings/server alignment. The app no longer closes when CDN links are missing.

## Bug Fixes

### Downloads — No More Crashes
- Download dialog wrapped with safe error handling; empty CDN results show a message instead of closing the app.
- Rewarded-ad gate failures no longer block downloads — dialog opens even if ads fail to load.
- Scrollable download dialog so Option 1 and Option 2 are always reachable on small screens.

### VidVault (Option 2) — Android
- Replaced unstable in-app WebView with **Chrome Custom Tabs** (in-app browser toolbar).
- Avoids WebView crashes and iframe-style ad hijacking seen on the website.
- TMDB title still pre-filled via `vidvault.ru/movie/{id}` or `/tv/{id}/{season}/{episode}`.
- Titles remain saved to StreamLux Library when download is requested.

### Settings — Default Video Server
- Settings now lists **5 core servers only**: VidKing, VidEasy, VidSrc.me, VidSrc.to, SuperEmbed.
- Extra fallbacks (VidNest, TouStream, etc.) stay in the **player server bar** only — not in Settings.
- Invalid saved server index safely falls back to VidKing.

## Web (deployed separately)
- VidVault modal renders via portal (no clipping inside episode lists).
- VidVault opens in a new tab to avoid ad redirects from iframe embedding.

## Build Info
- **Version Code:** 272
- **Version Name:** 2.7.2
- **Artifacts:** `app-release.apk`, `app-release.aab`
- **Paths:**
  - `app/build/outputs/apk/release/app-release.apk`
  - `app/build/outputs/bundle/release/app-release.aab`
