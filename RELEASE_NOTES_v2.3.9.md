# StreamLux Native v2.3.9 (31)

**versionCode:** 31 · **versionName:** 2.3.9

## Highlights

### WatchFooty Integration
- **Direct Scraper Integration**: Embedded WatchFooty's parallel match scraper inside `SportsService.kt` to load live and upcoming matches directly from `api.watchfooty.st` with custom headers.
- **Data Parity**: WatchFooty streams, live scores, team logos, and live minute details are now seamlessly merged with ESPN and Streamed.pk feeds on the native app, matching the website's robust data pipeline.

### General Match Click Redirection
- **Sports Hub Landing**: Updated click listeners on the compose `SportsScreen.kt` cards to launch the web sports landing page (`https://streamlux-67a84.web.app/sports`) instead of actual individual match pages, giving mobile users a unified starting point.

## Provider note (web + app)
- **WatchFooty** — Live matches, live scores, current minutes, and stream links are now fully integrated across both web and native app feeds.
- **ESPN** — High-fidelity scores and official logo data.
- **Streamed.pk** — Wide coverage of live fixtures with badge enrichment.

## Build

`StreamLuxNative/app/build/outputs/bundle/release/app-release.aab`

## Play Console notes

```
• WatchFooty live matches, scores, and minutes fully integrated into the sports feeds
• Redesigned match card clicks to open the StreamLux Web Sports Arena page
• Restructured data service for parallel coroutine fetching and caching
```
