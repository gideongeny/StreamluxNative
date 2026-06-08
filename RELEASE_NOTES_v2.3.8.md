# StreamLux Native v2.3.8 (30)

**versionCode:** 30 · **versionName:** 2.3.8

## Highlights

### Sports Arena redesign
- **Grid match cards** like Streamed.pk reference: team badges, split-color backgrounds, LIVE pill, stream count, Watch CTA.
- **Streamed.pk** as primary live provider — team badge URLs, source count, VS vs single-event detection.
- ESPN scores/logos merged as enrichment; best fixture wins by badge + stream quality score.

### Explore Channels LIVE 24/7
- Sports tab carousel now uses **Live TV Sports channels** (SuperSport, Sky Sports, ESPN, DAZN, etc.) — same catalog as Live TV tab.
- Tap opens in-app player (YouTube / HLS / iframe).

### Search fix
- Search uses **TmdbRepository** (Vercel → Firebase → direct TMDB) — fixes **HTTP 503** when Firebase gateway is down.

## Provider note (web + app)
- **Streamed.pk** — live fixtures, team badges, stream sources (best for match cards + stream count).
- **ESPN** — reliable scores and official logos.
- **WatchFooty** — playback links on web arena.

## Build

`StreamLuxNative/app/build/outputs/bundle/release/app-release.aab`

## Play Console notes

```
• Sports: premium grid cards with team badges, LIVE status, and stream counts
• Explore Channels pulls 24/7 sports networks from Live TV catalog
• Search fixed (no more HTTP 503 on movie/TV search)
• Streamed.pk live matches with club badges and backgrounds
```
