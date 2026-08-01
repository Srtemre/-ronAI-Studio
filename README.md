# HTML to APK Studio

A professional Android application that converts HTML, ZIP archives, website URLs,
folders, or raw pasted HTML into a complete Android WebView project — and
optionally builds the APK when the host environment provides the Android SDK.

> Built with Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore and Navigation Compose.

---

## Features

### Conversion modes
- **Fast Mode** — pick a source, name your app, generate.
- **Expert Mode** — full control over WebView, permissions, signing and advanced options.

### Source types
| Source      | Behaviour                                                                 |
|-------------|---------------------------------------------------------------------------|
| HTML File   | Single `.html` file becomes the entry point.                              |
| ZIP Archive | Walks the archive and copies every file under `app/src/main/assets/`.    |
| Folder      | Uses the Storage Access Framework to recursively walk a picked tree.      |
| Website URL | Uses Jsoup to download the page and absolutize links.                    |
| Paste HTML  | You type/paste HTML directly; no file picker involved.                    |

### Project manager
- Search, sort (name / modified), filter (All / Favorites / Recent).
- Rename, duplicate, delete, favorite, import (JSON), export (JSON).

### Settings
- Theme (Dark / Light / System), 6 accent presets, optional Material You dynamic color.
- Language: English, Turkish — instant switching without Activity restart.
- Build defaults (min/target SDK, default orientation, ProGuard on/off).
- Cache size inspector with one-tap clear.
- About screen with version info.

### Generation output
Each generated project lives under `filesDir/generated/<id>_<name>/project/` and
contains a full Android Studio project:

```
project/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── .gitignore
├── README.md
├── gradle/wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/                ← imported source assets
│       └── java/<package>/
│           ├── <AppName>App.kt    ← Application class
│           ├── MainActivity.kt    ← WebView host with all settings applied
│           └── WebViewScreen.kt   ← Extensibility placeholder
│       └── res/
│           ├── values/{strings,colors,themes}.xml
│           ├── drawable/{ic_launcher_background,ic_launcher_foreground}.xml
│           └── mipmap-anydpi-v26/{ic_launcher,ic_launcher_round}.xml
```

---

## Architecture

```
com.htmltoapk.studio
├── HtmlToApkApp                  // @HiltAndroidApp Application
├── MainActivity                  // single-activity Compose host
├── core/
│   ├── di/                       // Hilt modules (DB, DataStore, Repo, Dispatchers, Services)
│   ├── util/                     // FileUtil, TimeUtil, ValidationUtil, DocumentTreeHelper, AppJson
│   └── result/                   // sealed Result wrapper
├── data/
│   ├── local/{entity, dao, converter, AppDatabase}    // Room
│   ├── datastore/SettingsDataStore                     // Preferences DataStore
│   ├── repository/{ProjectRepositoryImpl, SettingsRepositoryImpl}
│   └── model/                    // domain DTOs: ProjectConfig, WebViewConfig, …
├── domain/
│   ├── repository/               // interfaces
│   └── usecase/
├── generator/                    // emits a full Android Studio project tree
├── importer/                     // HTML / ZIP / Folder / URL / Paste
├── builder/ApkBuilder            // optional Gradle wrapper invocation
└── ui/
    ├── theme/                    // Color / Theme / Type / Shape / SettingsViewModel
    ├── navigation/               // AppNavGraph, Destinations
    ├── components/               // RoundedCard, LabeledTextField, ToggleRow, ChoiceChips, …
    ├── home/  projects/  settings/  editor/
```

Pattern: **MVVM + Repository**, `StateFlow` everywhere, Hilt for DI, Coroutines
for async work, Room for projects, DataStore for settings.

---

## Building the app

Open the project in Android Studio (Hedgehog or newer) and press **Run**.

Or from the command line:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Requirements:
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with platform 34

---

## Building APKs on-device

`ApkBuilder` looks for `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) and the Gradle
wrapper inside the generated project. When either is unavailable — the common
case on a phone — the builder returns `BuildOutcome.Unavailable` and the UI
instructs the user to open the generated project on a desktop machine.

When the host environment does provide both:
1. A `local.properties` file is written pointing at the SDK.
2. `gradlew assembleRelease` is invoked with signing config in env vars.
3. The resulting APK at `app/build/outputs/apk/release/app-release.apk` is returned.

---

## Permissions used by the studio itself

| Permission                | Reason                                                            |
|---------------------------|-------------------------------------------------------------------|
| INTERNET                  | Needed when fetching a website URL as a source.                   |
| ACCESS_NETWORK_STATE      | Check connectivity before URL fetch.                              |
| POST_NOTIFICATIONS        | Build progress notifications (Android 13+).                       |
| READ_EXTERNAL_STORAGE     | Reading source files on API ≤ 32 (legacy).                       |

The **generated** app's permissions are user-controlled in Expert Mode.

---

## License

This project is provided as-is for use as a personal/commercial development tool.
Open-source dependencies retain their respective licenses (AndroidX, Hilt, Room,
DataStore, Coil, Accompanist, Jsoup, Apache Commons Compress).
