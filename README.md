# RAS ALD - YouTube Downloader

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="App Icon">
</p>

<p align="center">
  <b>Version 2.0</b> | Developed by <b>[ARMAN]</b>
</p>

<p align="center">
  <a href="https://github.com/yourusername/RAS-ALD-Downloader/actions">
    <img src="https://github.com/yourusername/RAS-ALD-Downloader/workflows/RAS%20ALD%20-%20Android%20CI/CD/badge.svg" alt="Build Status">
  </a>
  <a href="https://github.com/yourusername/RAS-ALD-Downloader/releases">
    <img src="https://img.shields.io/github/v/release/yourusername/RAS-ALD-Downloader" alt="Latest Release">
  </a>
  <a href="https://github.com/yourusername/RAS-ALD-Downloader/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  </a>
</p>

---

## 📱 পরিচিতি (Introduction)

**RAS ALD** একটি শক্তিশালী YouTube ভিডিও ডাউনলোডার অ্যাপ যা আপনাকে সহজেই YouTube ভিডিও, প্লেলিস্ট, এবং সাবটাইটেল ডাউনলোড করতে সাহায্য করে। এটি yt-dlp ইঞ্জিন ব্যবহার করে যা সর্বশেষ YouTube API সমর্থন করে।

---

## ✨ মূল ফিচার (Key Features)

### ✅ কনফার্মড ফিচার (Confirmed Features)

| # | Feature | Description |
|---|---------|-------------|
| 1 | **Room Database** | SQLite ইতিহাস সংরক্ষণ - সব ডাউনলোড ট্র্যাক করুন |
| 2 | **ExoPlayer** | ইন-অ্যাপ ভিডিও প্রিভিউ - ডাউনলোড করার আগে দেখুন |
| 3 | **Playlist Download** | সম্পূর্ণ প্লেলিস্ট ডাউনলোড |
| 4 | **Subtitle Download** | .srt/.vtt ফরম্যাটে সাবটাইটেল |

### 🆕 অতিরিক্ত ফিচার (Additional Features)

| # | Feature | Status |
|---|---------|--------|
| 5 | **Batch Download** | একসাথে ১০টা ভিডিও |
| 6 | **Background Download** | ব্যাকগ্রাউন্ডে ডাউনলোড + নোটিফিকেশন |
| 7 | **Download Queue** | ডাউনলোড কিউ ম্যানেজমেন্ট |
| 8 | **WiFi Only Mode** | শুধু WiFi-তে ডাউনলোড |
| 9 | **Video to Audio** | MP3 কনভার্টার |
| 10 | **Built-in Browser** | ইন-বিল্ট YouTube ব্রাউজার |
| 11 | **Dark Mode** | ডার্ক মোড সাপোর্ট |
| 12 | **Auto-Retry** | ব্যর্থ ডাউনলোড অটো-রিট্রাই |
| 13 | **Download Scheduler** | ডাউনলোড শিডিউলার |
| 14 | **Video Trimmer** | ভিডিও ট্রিমার (start-end time) |
| 15 | **Multi-Language** | বাংলা + ইংরেজি |
| 16 | **Quality Selection** | 144p থেকে 4K পর্যন্ত |

---

## ⚙️ টেকনিক্যাল স্পেসিফিকেশন (Technical Specifications)

```
┌─────────────────────────────────────────┐
│  Database:        Room (SQLite)         │
│  Media Player:    ExoPlayer 2.19.1      │
│  Download Engine: yt-dlp 2024.02.22     │
│  Language:        Kotlin                │
│  Min SDK:         API 24 (Android 7.0)  │
│  Target SDK:      API 34 (Android 14)   │
└─────────────────────────────────────────┘
```

---

## 📂 সাপোর্টেড ফরম্যাট (Supported Formats)

### ভিডিও (Video)
- **MP4** - 144p থেকে 4K পর্যন্ত
- **WEBM** - উচ্চ কোয়ালিটি ভিডিও
- **MKV** - মাল্টিমিডিয়া কন্টেইনার

### অডিও (Audio)
- **MP3** - সর্বাধিক জনপ্রিয়
- **M4A** - Apple ফরম্যাট
- **OPUS** - উচ্চ কোয়ালিটি
- **WAV** - লসলেস অডিও

### সাবটাইটেল (Subtitle)
- **SRT** - SubRip ফরম্যাট
- **VTT** - WebVTT ফরম্যাট
- **ASS** - Advanced SubStation Alpha

---

## 🔒 পারমিশন (Permissions)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

---

## 🚀 রিলিজ টার্গেট (Release Targets)

| Version | Features | Status |
|---------|----------|--------|
| **v1.0** | বেসিক ডাউনলোড + ইতিহাস | ✅ Complete |
| **v2.0** | Playlist + Subtitle + ExoPlayer | 🚧 In Progress |
| **v3.0** | Batch + Background + Browser | 📋 Planned |

---

## 📥 ইনস্টলেশন (Installation)

### GitHub Release থেকে (Recommended)

1. [Latest Release](https://github.com/yourusername/RAS-ALD-Downloader/releases) এ যান
2. APK ফাইল ডাউনলোড করুন
3. "Install from Unknown Sources" এনেবল করুন
4. APK ইনস্টল করুন

### সোর্স থেকে বিল্ড (For Developers)

```bash
# Clone the repository
git clone https://github.com/yourusername/RAS-ALD-Downloader.git

# Navigate to project directory
cd RAS-ALD-Downloader

# Build with Gradle
./gradlew assembleDebug

# Or build release
./gradlew assembleRelease
```

---

## 🏗️ প্রজেক্ট স্ট্রাকচার (Project Structure)

```
RAS-ALD-Downloader/
├── .github/
│   └── workflows/
│       └── android-build.yml      # CI/CD Pipeline
├── app/
│   ├── src/main/
│   │   ├── java/com/arman/rasald/
│   │   │   ├── data/
│   │   │   │   ├── database/      # Room Database
│   │   │   │   ├── dao/           # Data Access Objects
│   │   │   │   └── entity/        # Data Entities
│   │   │   ├── ui/
│   │   │   │   ├── activities/    # Activities
│   │   │   │   ├── adapters/      # RecyclerView Adapters
│   │   │   │   └── viewmodels/    # ViewModels
│   │   │   ├── service/           # Download Service
│   │   │   └── utils/             # Utility Classes
│   │   ├── res/                   # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🔄 GitHub Actions CI/CD

প্রজেক্টটিতে সম্পূর্ণ CI/CD পাইপলাইন রয়েছে যা নিম্নলিখিত কাজগুলো করে:

### Jobs

| Job | Description |
|-----|-------------|
| **🔍 lint** | Code quality check with ktlint |
| **🧪 unit-test** | Run unit tests |
| **🔨 build-debug** | Build debug APK |
| **📦 build-release** | Build and sign release APK |
| **🚀 create-release** | Create GitHub release with APK |
| **🔒 security-scan** | Trivy vulnerability scan |
| **📊 apk-stats** | APK size analysis |

### Workflow Triggers

- **Push** to `main` or `develop` branch
- **Pull Request** to `main` branch
- **Tag** push (creates release)

---

## 🛠️ ডেভেলপমেন্ট (Development)

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK API 34
- Kotlin 1.9.22

### Setup

1. Android Studio তে প্রজেক্ট ওপেন করুন
2. `gradle sync` করুন
3. রান বাটনে ক্লিক করুন

### Dependencies

```kotlin
// Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")

// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// ExoPlayer
implementation("com.google.android.exoplayer:exoplayer:2.19.1")

// yt-dlp (Python)
implementation("com.chaquo.python:gradle:15.0.1")
```

---

## 🤝 কন্ট্রিবিউশন (Contributing)

কন্ট্রিবিউশন স্বাগতম! নিম্নলিখিত পদ্ধতি অনুসরণ করুন:

1. Fork করুন
2. Feature branch তৈরি করুন (`git checkout -b feature/amazing-feature`)
3. Commit করুন (`git commit -m 'Add amazing feature'`)
4. Push করুন (`git push origin feature/amazing-feature`)
5. Pull Request তৈরি করুন

---

## 📄 লাইসেন্স (License)

এই প্রজেক্টটি MIT License-এর অধীনে লাইসেন্সকৃত। বিস্তারিত জানতে [LICENSE](LICENSE) ফাইল দেখুন।

---

## 🙏 কৃতজ্ঞতা (Acknowledgments)

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) - ডাউনলোড ইঞ্জিন
- [ExoPlayer](https://github.com/google/ExoPlayer) - মিডিয়া প্লেয়ার
- [Chaquopy](https://chaquo.com/chaquopy/) - Python for Android
- [Material Design 3](https://m3.material.io/) - UI Design

---

## 📞 যোগাযোগ (Contact)

- **Developer:** [ARMAN]
- **Email:** your.email@example.com
- **GitHub:** [@yourusername](https://github.com/yourusername)

---

<p align="center">
  <b>Made with ❤️ in Bangladesh</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python">
</p>
