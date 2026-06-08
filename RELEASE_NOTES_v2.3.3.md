# StreamLux Native v2.3.3 (25)

**versionCode:** 25 · **versionName:** 2.3.3

## Highlights

### TMDB (free tier, ~20k daily users)
- New API key + read token configured for gateway proxying.
- Longer server-side cache (hours–days per endpoint) so most users hit CDN/cache, not TMDB directly.
- Stale fallback when rate-limited — home and browse stay online.

### Kenya Live TV (YouTube)
- **Citizen TV**, **NTV**, **KTN**, **TV47**, **K24**, **Ramogi TV** use a dedicated YouTube player (`youtube-nocookie`, muted autoplay for mobile WebView).
- **Open in YouTube** fallback if embed is blocked.
- Channels sorted at the top of Live TV.

### From v2.3.2
- Sports cinematic cards, CricHd live feeds, Piped music, batched TMDB home.

## Upload
- **AAB:** `app/build/outputs/bundle/release/app-release.aab`
- **Play Console short description:**  
  `Kenya news live on YouTube, TMDB cache for 20k users, sports arena & Live TV fixes.`

## Env (do not commit)
Set `TMDB_API_KEY` and `TMDB_BEARER_TOKEN` in Vercel + Firebase `functions/.env`.
