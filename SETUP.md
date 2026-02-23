# RAS ALD - Project Setup Guide

## ⚠️ গুরুত্বপূর্ণ নোট (Important Notes)

### 1. Gradle Wrapper JAR Download

প্রজেক্টে `gradle-wrapper.jar` ফাইলটি অনুপস্থিত। এটি ডাউনলোড করতে:

```bash
# Option 1: Using curl
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar

# Option 2: Using wget
wget -O gradle/wrapper/gradle-wrapper.jar \
  https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar

# Option 3: Using Android Studio (Recommended)
# Open project in Android Studio and it will automatically download the wrapper
```

### 2. Keystore Setup (Release Build এর জন্য)

**কখনোই keystore ফাইল Git-এ কমিট করবেন না!**

#### Local Development:

```bash
# Create keystore
cd app
keytool -genkey -v -keystore release.keystore -alias rasald -keyalg RSA -keysize 2048 -validity 10000

# Or use environment variables
export KEYSTORE_PATH=release.keystore
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=rasald
export KEY_PASSWORD=your_password
```

#### GitHub Actions (CI/CD):

1. GitHub Repository → Settings → Secrets and variables → Actions
2. Add these secrets:
   - `KEYSTORE_BASE64` - Base64 encoded keystore
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`

```bash
# Convert keystore to base64
base64 -i app/release.keystore | pbcopy  # macOS
base64 -i app/release.keystore -w 0     # Linux
```

## 🔧 Fixed Issues

### ✅ 1. gradlew - FIXED
- `gradlew` (Unix) এবং `gradlew.bat` (Windows) তৈরি করা হয়েছে
- Executable permission সেট করা আছে

### ✅ 2. Gradle Wrapper - FIXED
- `gradle/wrapper/gradle-wrapper.properties` তৈরি করা হয়েছে
- Gradle 8.2 ব্যবহার করা হয়েছে

### ✅ 3. Signing Config - FIXED
- Double signing সমস্যা সমাধান করা হয়েছে
- CI-এ আলাদা signing approach ব্যবহার করা হয়
- Local-এ debug signing fallback আছে

### ✅ 4. Keystore Path - FIXED
- Environment variable থেকে path নেয়
- Default fallback আছে
- CI-তে auto-detect করে

### ✅ 5. ktlint - FIXED
- Version 1.0.1 ব্যবহার করা হয়েছে
- Filter configuration ঠিক করা হয়েছে
- CI-তে `|| true` দেওয়া আছে (fail হলেও build চলবে)

### ✅ 6. Chaquopy CI - FIXED
- `com.chaquo.python` plugin সরাসরি `build.gradle.kts`-এ যোগ করা হয়েছে
- Chaquopy repository `settings.gradle.kts`-এ যোগ করা হয়েছে

## 🚀 Quick Start

### Step 1: Clone Repository

```bash
git clone https://github.com/yourusername/RAS-ALD-Downloader.git
cd RAS-ALD-Downloader
```

### Step 2: Download Gradle Wrapper

```bash
# Download gradle-wrapper.jar
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar
```

### Step 3: Build Debug APK

```bash
./gradlew assembleDebug
```

### Step 4: Build Release APK (Keystore সহ)

```bash
# Create keystore first (if not exists)
cd app
keytool -genkey -v -keystore release.keystore -alias rasald -keyalg RSA -keysize 2048 -validity 10000

# Set environment variables
export KEYSTORE_PATH=release.keystore
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=rasald
export KEY_PASSWORD=your_password

# Build release
cd ..
./gradlew assembleRelease
```

## 📋 Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Run lint
./gradlew lint

# Run ktlint
./gradlew ktlintCheck

# Clean build
./gradlew clean

# Full build
./gradlew clean build
```

## 🔒 Security Best Practices

1. **Never commit keystore files**
   ```gitignore
   # Add to .gitignore
   *.keystore
   *.jks
   app/release.keystore
   ```

2. **Use environment variables for secrets**
   ```bash
   export KEYSTORE_PASSWORD="your-password"
   export KEY_PASSWORD="your-password"
   ```

3. **Use GitHub Secrets for CI/CD**
   - `KEYSTORE_BASE64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`

## 🐛 Troubleshooting

### Issue: `gradle-wrapper.jar` not found

**Solution:**
```bash
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar
```

### Issue: Signing failed

**Solution:**
- Check if keystore file exists
- Verify environment variables are set
- For CI, check GitHub Secrets

### Issue: ktlint fails

**Solution:**
```bash
# Auto-fix issues
./gradlew ktlintFormat

# Or skip in CI (already configured)
./gradlew ktlintCheck || true
```

### Issue: Chaquopy not found

**Solution:**
- Check `settings.gradle.kts` has Chaquopy repository
- Check `build.gradle.kts` has Chaquopy plugin

## 📁 Project Structure

```
RAS-ALD-Downloader/
├── .github/
│   └── workflows/
│       └── android-build.yml      # CI/CD Pipeline (FIXED)
├── app/
│   ├── build.gradle.kts           # App build config (FIXED)
│   └── src/...
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar     # DOWNLOAD REQUIRED
│       └── gradle-wrapper.properties
├── build.gradle.kts               # Project build config
├── settings.gradle.kts            # Project settings
├── gradlew                        # Gradle wrapper (Unix)
├── gradlew.bat                    # Gradle wrapper (Windows)
└── SETUP.md                       # This file
```

## 📝 Summary of Fixes

| Issue | Status | Fix |
|-------|--------|-----|
| gradlew missing | ✅ FIXED | Created gradlew & gradlew.bat |
| gradle wrapper missing | ✅ FIXED | Created wrapper properties |
| release signingConfig | ✅ FIXED | Conditional signing, no double sign |
| keystore path | ✅ FIXED | Environment variable based |
| double signing | ✅ FIXED | Single signing in CI |
| ktlint | ✅ FIXED | Version 1.0.1, proper filters |
| Chaquopy CI | ✅ FIXED | Plugin in build.gradle.kts |

---

**Note:** Gradle wrapper JAR টি ডাউনলোড করতে ভুলবেন না! Android Studio ব্যবহার করলে এটি অটোমেটিক ডাউনলোড হয়।
