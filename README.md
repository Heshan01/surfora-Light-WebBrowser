# 🌊 Surfora Light

A lightweight, native Android web browser inspired by Safari's clean design — featuring a bottom address bar, a visual tab grid, safe browsing protection, bookmarks & history, and a private browsing mode.

🔗 **Try it now:** https://surforalightapk.vercel.app


## ✨ Features

* **Bottom Address Bar** — Reachable, thumb-friendly navigation styled after Safari
* **Tab Grid** — Visual overview to manage multiple open tabs at a glance
* **Private Browsing** — An isolated incognito mode that keeps tabs, history, and data separate
* **Bookmarks & History** — Save favorite pages and revisit your browsing history, with a one-tap **Clear History and Website Data** option
* **Safe Browsing** — Built-in protection that flags known malicious domains
* **Search Engine Choice** — Switch between Google, DuckDuckGo, and Bing
* **Appearance Modes** — System, Light, and Dark theme support
* **Local Data Storage** — Bookmarks and history persisted locally with Room

## 🛠 Tech Stack

* **Kotlin** + **Jetpack Compose (Material 3)** for a modern declarative UI
* **Room** for local persistence (bookmarks & history)
* **Android WebView** as the browsing engine
* **Retrofit / OkHttp / Moshi** for networking
* **Coil** for image loading
* **Roborazzi** for screenshot testing

## 📋 Prerequisites

* Android Studio (latest stable)
* Android SDK with `compileSdk 36`
* Minimum SDK: `24` (Android 7.0+)

## 🚀 Run Locally

1. Clone this repository:

```bash
git clone https://github.com/<your-username>/surfora-light.git
cd surfora-light
```

2. Open the project in **Android Studio** and allow Gradle to sync.

3. If you're creating a release build, update the signing configuration in `app/build.gradle.kts` as needed.

4. Run the application on an Android emulator or a physical device.

## 📁 Project Structure

```text
app/src/main/java/com/example/
├── data/
│   ├── dao/                  # Room DAOs
│   ├── database/             # Room database setup
│   ├── entity/               # Bookmark & HistoryEntry entities
│   └── repository/           # Data repository layer
├── ui/
│   ├── BrowserScreen.kt           # Main browser UI
│   ├── BrowserWebView.kt          # WebView wrapper
│   ├── BookmarksHistoryScreen.kt  # Bookmarks & History UI
│   ├── SettingsScreen.kt          # Settings UI
│   ├── TabSwitcherScreen.kt       # Tab Grid UI
│   └── theme/                     # App theme
├── viewmodel/
│   └── BrowserViewModel.kt        # Core browser state & logic
└── MainActivity.kt
```

## 📄 License

This project currently has no explicit license. Add a `LICENSE` file if you plan to open source it.
