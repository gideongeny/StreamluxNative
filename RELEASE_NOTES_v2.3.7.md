# StreamLux Native v2.3.7 (29)

**versionCode:** 29 · **versionName:** 2.3.7

## Fixes

### Detail page crash
- Fixed app closing when opening a movie/TV detail screen (Compose crash from `LazyRow` inside a vertically scrolling `Column`).
- Detail TMDB calls now use the same **3-key fallback repository** as home (credits, similar, videos, seasons).
- Added retry UI when detail fetch fails.

### Home categories / genres
- Movies tab now loads **all genre rows** (Action, Comedy, Horror, Sci-Fi, Drama, etc.) with progressive updates as data arrives.
- Trending hero still uses the top row; remaining rows scroll below (you should see 12+ sections, not only 4).
- Epic Collections no longer open a broken detail page when tapped.

## Build

`StreamLuxNative/app/build/outputs/bundle/release/app-release.aab`

## Play Console notes

```
• Fixed crash when opening movie and TV show details
• More home categories: Action, Comedy, Horror, Sci-Fi, Drama, and more
• More reliable detail pages with TMDB backup keys
```
