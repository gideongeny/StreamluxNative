# StreamLux v2.6.8 Release Notes

## Bug Fixes
- **Google Sign-In Crash**: Fixed an issue where "serverClientId should not be empty" error appeared when trying to sign in with Google by properly injecting the Web Client ID fallback.
- **Google Logo Orientation**: Fixed the manually drawn Google logo in the Auth Screen which appeared rotated/upside-down. It now uses the official vector drawable.
- **Video Source Black Screens**: Removed strict URL overriding rules in the webview, granting video sources complete freedom to execute redirects and load necessary ad scripts. This resolves the persistent black screens on many embed sources.

## Performance Improvements
- **Detail Screen Loading Speed**: Drastically reduced the time it takes to load the Movie and TV details screens. The app now directly uses the provided API key first rather than waiting for multiple hardcoded fallback credentials to timeout.
