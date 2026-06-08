# StreamLux Native v2.3.4 (26)

**versionCode:** 26 · **versionName:** 2.3.4  
**Application ID:** `com.streamlux.app.mobile`

## Highlights

### TMDB resilience (3-key fallback chain)
- Home Movies & TV no longer depend on a single gateway.
- Fetch order: Firebase `/api/tmdb` → Vercel `/api/tmdb` → Cloud Functions gateway → **direct TMDB** with automatic key rotation.
- Built-in credential pool: two new API key + Bearer pairs, plus existing key and optional `local.properties` override.
- Reduces empty home rows and endless skeleton loaders when the gateway is down or rate-limited.

### Kenya Live TV (black screen fix)
- Kenyan news channels (Citizen, NTV, KTN, TV47, K24, Ramogi) load from hosted `kenyan_live_tv.json` and play in the **native YouTube player**.
- Global catalog still merges underneath; YouTube channels are listed first.
- Live playback routing: YouTube → HLS (`.m3u8`) → WebView (iframe embeds), so iframe URLs are no longer forced through HLS.

### Stability
- Progressive home loading unchanged; TMDB failures now fall through to the next source instead of returning empty lists.

## From v2.3.3
- TMDB gateway caching, Kenya YouTube on web, sports arena, Piped music, batched home sections.

## Build artifacts

| Artifact | Path |
|----------|------|
| **AAB (Play Store)** | `StreamLuxNative/app/build/outputs/bundle/release/app-release.aab` |
| **APK (optional sideload)** | `StreamLuxNative/app/build/outputs/apk/release/app-release.apk` |

## Play Console

**Short description (≤80 chars):**  
`TMDB triple fallback, Kenya news on YouTube, Live TV & home fixes.`

**Release notes (user-facing):**
```
• Movies & TV home loads more reliably with automatic TMDB backup keys
• Kenya Live TV: Citizen, NTV, KTN, TV47, K24, Ramogi play in-app (YouTube)
• Live TV: correct player for YouTube, HLS, and web channels
• General stability when the API gateway is unavailable
```

## Build command

From `StreamLuxNative/` (requires `local.properties` signing + secrets):

```powershell
.\gradlew.bat bundleRelease
```

## Upload checklist

1. Upload `app-release.aab` to Play Console → Production or Internal testing.
2. Set version **2.3.4 (26)**.
3. Paste release notes above.
4. Confirm Firebase Hosting serves `kenyan_live_tv.json` (already deployed to streamlux-67a84.web.app).

## Env (do not commit)

Optional in `StreamLuxNative/local.properties`:

- `TMDB_API_KEY` / signing `storeFile`, `storePassword`, `keyAlias`, `keyPassword`

Built-in TMDB fallbacks work without `local.properties` TMDB keys.
