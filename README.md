# MediaFlow Downloader

MediaFlow is a modern, production-grade Android utility for saving permitted, publicly accessible media from YouTube and Instagram. It respects platform Terms of Service, copyright laws, and access controls while providing a seamless user experience.

---

## 🌟 Key Features

1. **URL Analysis & Security Sanitization**
   - Strict allowlist validation for YouTube (`watch`, `youtu.be`, `shorts`) and Instagram (`p`, `reel`, `tv`).
   - Automated removal of tracking parameters (`utm_*`, `si`, `igsh`, `fbclid`).
   - Clipboard link detection with privacy-preserving opt-in prompts.
   - Comprehensive Android Share Intent integration (`ACTION_SEND` with `text/plain`).

2. **Compliance & Content Rights Notice**
   - Mandatory first-run legal policy acknowledgment.
   - Zero bypass of DRM, login paywalls, or authentication.
   - Explicit user messaging for private or restricted content.

3. **Format & Quality Selection**
   - Video: 1080p FHD, 720p HD, 480p SD MP4.
   - Audio: AAC/M4A 256 kbps, 128 kbps.
   - Accurate bitrate, resolution, container, and file size estimation.

4. **Background Download Engine (WorkManager)**
   - CoroutineWorker running with Foreground Service notifications.
   - Real-time progress bar, download speed calculation (KB/s, MB/s), and dynamic ETA.
   - Pause, resume, retry with exponential backoff, and cancellation.
   - Network constraint handling (Wi-Fi only mode supported via DataStore preferences).

5. **Local Scoped Storage (MediaStore)**
   - Saved into standard Android Scoped Storage directories:
     - Videos: `Movies/MediaFlow/` (`MediaStore.Video.Media`)
     - Audio: `Music/MediaFlow/` (`MediaStore.Audio.Media`)
   - Zero broad storage permissions required on Android 10+ (API 29+).

6. **Media Library & Download History**
   - In-app media browser with List / Grid view toggles.
   - Open in external player, Share, Rename (with path traversal protection), and Delete.
   - Reactive Room database tracking status (`QUEUED`, `ANALYZING`, `DOWNLOADING`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELLED`, `PAUSED`).

7. **Design & Theming**
   - Material 3 Design with edge-to-edge layout (`enableEdgeToEdge()`).
   - Adaptive M3 NavigationBar with consistent iconography and active pills.
   - Full support for Dark, Light, and System themes.

---

## 🏗 Architecture

```
Android App (Jetpack Compose UI)
       │
       ▼
Presentation Layer (MVVM + ViewModels + Navigation Compose)
       │
       ▼
Domain Layer (Models, Repositories, UseCases)
       │
       ▼
Repository Layer (Media, Download, History, Storage, Settings)
  ├── Authorized Media Provider / MediaResolver (YouTube & Instagram)
  ├── AudioProcessor
  ├── Android WorkManager (DownloadWorker + Foreground Notification)
  ├── Room Database (MediaFlowDatabase + DownloadHistoryDao)
  ├── Scoped Storage (Android MediaStore API)
  └── Jetpack DataStore Preferences
```

---

## 🛠 Technology Stack

- **Language:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose with Material Design 3
- **Local Database:** Room 2.7.0 with KSP compiler
- **Preferences:** Jetpack DataStore Preferences 1.1.7
- **Background Tasks:** AndroidX WorkManager 2.10.0
- **Image Loading:** Coil Compose 2.7.0
- **Networking:** OkHttp & Retrofit
- **Testing:** JUnit4, Robolectric 4.16.1

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug | 2024.2+ or Android CLI
- JDK 17 or higher
- Android SDK with API 36 / 34 installed
- Minimum target device: Android 8.0 (API 26)

### Compile App
```bash
gradle assembleDebug
```

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```
