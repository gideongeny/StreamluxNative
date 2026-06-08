# StreamLux Native v2.3.2 (24)

**Release date:** May 2026  
**versionCode:** 24 · **versionName:** 2.3.2

## What's new

### Sports arena
- Live fixtures merged from ESPN scoreboards, WatchFooty, SofaScore, and **CricHd** auto-updated sports TV feeds ([CricHd playlists](https://github.com/abusaeeidx/CricHd-playlists-Auto-Update-permanent)).
- Match cards restored with **full-bleed cinematic cover art** (channel logos, venue shots, stadium fallbacks).
- **streaming-ticker**-style league coverage: Premier League, La Liga, Serie A, Bundesliga, UCL, NBA, NFL, MLB, NHL, F1, UFC.

### Live TV — Kenya
- **Citizen TV**, **NTV**, **KTN**, **TV47**, **K24**, and **Ramogi TV** via YouTube live embeds (web app; native Live TV uses existing catalog).

### API & stability (from v2.3.1)
- Firebase gateway routing for TMDB and APIs.
- Batched TMDB home loads.
- Piped music improvements.
- Sports multi-provider fallbacks.

## Install
- **Google Play:** upload `app-release.aab` from `app/build/outputs/bundle/release/`.
- **Sideload:** `app/build/outputs/apk/release/app-release.apk`.

## Notes for reviewers
- Sports streams may link to third-party embeds; content is aggregated, not hosted by StreamLux.
- Kenyan TV entries use official YouTube live streams where available.
