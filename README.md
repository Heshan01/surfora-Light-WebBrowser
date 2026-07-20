# 🌊 Surfora Light

A lightweight, native Android web browser inspired by Safari's clean design — featuring a bottom address bar, a visual tab grid, safe browsing protection, bookmarks & history, and a private browsing mode.

🔗 **Try it now:** [surforalightapk.vercel.app](https://surforalightapk.vercel.app)

<div align="center">
  <img width="1200" height="475" alt="Surfora Light" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

## ✨ Features

- **Bottom Address Bar** — Reachable, thumb-friendly navigation styled after Safari
- **Tab Grid** — Visual overview to manage multiple open tabs at a glance
- **Private Browsing** — An isolated incognito mode that keeps tabs, history, and data separate
- **Bookmarks & History** — Save favorite pages and revisit your browsing history, with a one-tap "clear history and website data" option
- **Safe Browsing** — Built-in protection that flags known malicious domains
- **Search Engine Choice** — Switch between Google, DuckDuckGo, and Bing
- **Appearance Modes** — System, Light, and Dark theme support
- **Local Data Storage** — Bookmarks and history persisted locally with Room

## 🛠 Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3) for a fully declarative UI
- **Room** for local persistence (bookmarks & history)
- **Android WebView** as the browsing engine
- **Firebase** (AI/App Check) integration
- **Retrofit / OkHttp / Moshi** for networking
- **Coil** for image loading
- **Roborazzi** for screenshot testing

## 📋 Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- Android SDK with `compileSdk 36` / `minSdk 24`
- A Gemini API key (for AI-related features)

## 🚀 Run Locally

1. Clone this repository:
   ```bash
   git clone https://github.com/<your-username>/surfora-light.git
   cd surfora-light
   ```
2. Open the project in **Android Studio** and let it sync/import (it will resolve any Gradle plugin mismatches automatically).
3. Create a `.env` file in the project root and set your Gemini API key:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```
   (see `.env.example` for the expected format)
4. For a release build, remove or update the debug signing line in `app/build.gradle.kts`:
   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```
5. Run the app on an emulator or physical device (min SDK 24 / Android 7.0+).

## 📁 Project Structure

```
app/src/main/java/com/example/
├── data/
│   ├── dao/              # Room DAOs
│   ├── database/         # Room database setup
│   ├── entity/            # Bookmark & HistoryEntry entities
│   └── repository/        # Data repository layer
├── ui/
│   ├── BrowserScreen.kt           # Main browser UI
│   ├── BrowserWebView.kt          # WebView wrapper
│   ├── BookmarksHistoryScreen.kt  # Bookmarks & history UI
│   ├── SettingsScreen.kt          # Settings UI
│   ├── TabSwitcherScreen.kt       # Tab grid UI
│   └── theme/                     # App theming
├── viewmodel/
│   └── BrowserViewModel.kt        # Core browser state & logic
└── MainActivity.kt
```

## 📄 License

This project currently has no explicit license. Add a `LICENSE` file if you plan to open source it.