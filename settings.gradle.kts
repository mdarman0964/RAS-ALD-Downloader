pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Chaquopy repository for Python support
        maven { url = uri("https://chaquo.com/maven") }
    }
    plugins {
        id("com.chaquo.python") version "15.0.1" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Chaquopy repository for Python runtime
        maven { url = uri("https://chaquo.com/maven") }
    }
}

rootProject.name = "RAS-ALD-Downloader"
include(":app")
