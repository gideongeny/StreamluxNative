# StreamLux Native 🚀

StreamLux is a high-performance, cross-platform media ecosystem designed for the modern era of streaming. This repository contains the **Native Android Application**, built from the ground up using state-of-the-art technologies to provide a seamless, premium, and "clean shell" experience for users worldwide.

![StreamLux Banner](https://raw.githubusercontent.com/gideongeny/STREAMLUX/main/public/logo192.png)

## ✨ Core Features

- **📺 Live TV Hub:** Dynamic streaming of global channels with premium card aesthetics and HLS/Iframe support.
- **⚽ Sports Arena:** Real-time match filtering, upcoming fixture tracking, and match expiry logic for a clutter-free experience.
- **🎬 Movie & Series Intelligence:** Aggressive 48-hour TMDB caching to maximize availability and performance.
- **🤖 GeniusAI Assistant:** Integrated chatbot powered by Groq AI for media discovery and personalized recommendations.
- **🎵 Music Hub:** A full-featured music player with playback service support for background audio.
- **📱 Unified Dashboard:** Glassmorphism UI/UX with dark-mode first design and smooth micro-animations.

## 🛠️ Technology Stack

- **UI:** Jetpack Compose (100% Kotlin)
- **Architecture:** MVVM with Clean Architecture principles
- **DI:** Hilt (Dagger)
- **Networking:** Retrofit2, OkHttp3 (with aggressive caching)
- **Media:** AndroidX Media3 (ExoPlayer)
- **Database:** Room (for offline library & history)
- **Image Loading:** Coil-Compose
- **Sign In:** Firebase Auth & Google Sign-In

## 🚀 Getting Started

To protect your API credentials, this project uses a `local.properties` injection system.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/gideongeny/StreamluxNative.git
    cd StreamluxNative
    ```
2.  **Setup Secrets:**
    - Copy the `local.properties.example` file to a new file named `local.properties`.
    - Fill in your API keys (TMDB, Groq, Firebase, etc.).
3.  **Build and Run:**
    - Open the project in **Android Studio (Iguana or later)**.
    - Sync Gradle and run the app on your emulator or physical device.

## 📦 Production Builds

To generate a clean production release:
```bash
./gradlew clean assembleRelease
```
The APK will be located in `app/build/outputs/apk/release/`.

## 🛡️ License

StreamLux Native is provided as a "Clean Shell" application. It does not contain hardcoded copyrighted streams. All content is fetched dynamically via user-provided or remote JSON configurations.

---
*Created with ❤️ by Gideon Geny*
