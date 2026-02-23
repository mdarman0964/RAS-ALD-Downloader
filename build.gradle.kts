// Top-level build file for RAS ALD - YouTube Downloader
// Developer: [ARMAN]
// Version: 2.0

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.22-1.0.17")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// Task to clean build
 tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

// Custom task for version info
tasks.register("printVersion") {
    doLast {
        println("RAS ALD - YouTube Downloader")
        println("Version: 2.0")
        println("Developer: [ARMAN]")
        println("Build Date: ${java.util.Date()}")
    }
}
