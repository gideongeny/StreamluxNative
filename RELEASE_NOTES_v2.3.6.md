# StreamLux Native v2.3.6 (28)

**versionCode:** 28 · **versionName:** 2.3.6  
**Application ID:** `com.streamlux.app.mobile`

## Highlights

### Kenya / YouTube Live TV — Error 152 fix
- Replaced the native YouTube SDK iframe player with a **WebView embed** that matches the web app (`youtube-nocookie.com`, muted autoplay, correct `origin`).
- Embed loads inside HTML with base URL `https://streamlux-67a84.web.app` so YouTube allowlists match production hosting.
- If a channel still blocks embedding, the player shows **Open on YouTube** and a bottom bar with **Reload stream** / **YouTube app** (same UX as web Live TV).

### Home (from v2.3.5)
- TMDB **direct-first** with 3-key rotation and gateway fallbacks.
- Home retry UI when all sources fail.

## Play Console

**Short description (≤80 chars):**  
`Kenya Live TV embed fix, YouTube app fallback, TMDB home reliability.`

**Release notes (user-facing):**
```
• Kenya Live TV: Citizen, NTV, KTN and more — fixed “video unavailable” (error 152)
• Open Kenyan news streams in the YouTube app if embed is blocked on your device
• Movies & TV home loads more reliably (TMDB backup keys + retry)
• Live TV: correct player for YouTube, HLS, and web channels
```

## Build artifacts

| Artifact | Path |
|----------|------|
| **AAB (Play Store)** | `StreamLuxNative/app/build/outputs/bundle/release/app-release.aab` |

## Build command

From `StreamLuxNative/`:

```powershell
.\gradlew.bat bundleRelease
```

## Upload checklist

1. Upload `app-release.aab` to Play Console.
2. Set version **2.3.6 (28)**.
3. Paste release notes above.
4. Smoke-test: Live TV → Kenya → Citizen TV / NTV (embed + YouTube app button).
