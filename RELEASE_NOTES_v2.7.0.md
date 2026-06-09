# StreamLux v2.7.0 Release Notes

## Overview
Version 2.7.0 strengthens playback reliability, download resilience, and sports routing. The native app now mirrors the web experience with VidKing-first streaming, a VidVault in-app fallback for downloads, and expanded server fallbacks.

## New Features

### Multi-Server Playback Fallbacks
- **VidKing** is now the default streaming server.
- Added **16 fallback servers**: VidEasy, VidSrc.me, VidSrc.to, VidNest, TouStream, VidLink, VidFast, AutoEmbed, MovieAPI, SmashyStream, AnimeHub, Fsonic, Miruro, MeowTV, and SuperEmbed.
- Switch servers from the player without leaving the app.

### Download Resilience (VidVault WebView)
- When direct CDN download links are unavailable, users see a clear message instead of a crash or forced browser exit.
- **Open VidVault Portal** launches `https://vidvault.ru/` inside an in-app WebView with TMDB ID pre-filled.
- Movies and TV episodes are saved to the local library when a download is requested, even if links are missing.

### Sports Arena Redirect
- All sports match taps now open the live arena on **https://streamlux.vercel.app/sports/arena/{matchId}** inside the app WebView player.

## Web App (Film Watch)
- Control bar now shows a **Server** label beside source buttons.
- Subtitles control is icon-only so it is no longer confused with server selection.

## Build Info
- **Version Code:** 270
- **Version Name:** 2.7.0
- **Artifacts:** `app-release.apk` and `app-release.aab`
- **Output paths:**
  - `app/build/outputs/apk/release/app-release.apk`
  - `app/build/outputs/bundle/release/app-release.aab`
