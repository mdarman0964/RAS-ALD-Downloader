plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.chaquo.python")
}

android {
    namespace = "com.arman.rasald"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.arman.rasald"
        minSdk = 24
        targetSdk = 34
        versionCode = 200
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supported architectures
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    // Signing configuration - ONLY for local development
    // CI/CD will handle release signing separately
    signingConfigs {
        create("release") {
            // These values should come from environment variables or local.properties
            // DO NOT commit keystore files to git
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            
            // Only enable if keystore exists
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use release signing config only if keystore exists
            signingConfig = if (file(System.getenv("KEYSTORE_PATH") ?: "release.keystore").exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DUMMY.SF"
            excludes += "META-INF/DUMMY.RSA"
        }
    }

    // ExoPlayer requires Java 8
    lint {
        disable += "ObsoleteLintCustomCheck"
        abortOnError = false
    }
    
    // Chaquopy Python configuration
    chaquopy {
        defaultConfig {
            version = "3.11"
            pip {
                install("yt-dlp==2024.2.22")
                install("requests")
            }
        }
        productFlavors { }
        sourceSets { }
    }
}

dependencies {
    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ExoPlayer
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-dash:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.19.1")

    // Chaquopy Python runtime
    implementation("com.chaquo.python.runtime:chaquopy_java:15.0.1")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // WorkManager (Background tasks)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Preferences/DataStore
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // JSON Parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // File Utils
    implementation("commons-io:commons-io:2.15.1")

    // WebView
    implementation("androidx.webkit:webkit:1.10.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Progress/Loading
    implementation("com.github.ybq:Android-SpinKit:1.4.0")

    // Material Dialogs
    implementation("com.afollestad.material-dialogs:core:3.3.0")
    implementation("com.afollestad.material-dialogs:input:3.3.0")

    // Toasty (Custom Toast)
    implementation("com.github.GrenderG:Toasty:1.5.2")

    // Lottie Animation
    implementation("com.airbnb.android:lottie:6.3.0")

    // Timber (Logging)
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}

// ktlint configuration - FIXED for CI compatibility
ktlint {
    version.set("1.0.1")
    android.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        include("**/src/**/*.kt")
    }
}
