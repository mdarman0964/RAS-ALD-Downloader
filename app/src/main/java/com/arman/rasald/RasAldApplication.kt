package com.arman.rasald

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import androidx.work.WorkManager
import com.arman.rasald.data.database.AppDatabase
import com.arman.rasald.utils.PreferenceManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import timber.log.Timber

/**
 * RAS ALD - YouTube Downloader Application Class
 * Developer: [ARMAN]
 * Version: 2.0
 */
class RasAldApplication : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_DOWNLOAD_ID = "download_channel"
        const val CHANNEL_GENERAL_ID = "general_channel"
        const val NOTIFICATION_ID_DOWNLOAD = 1001
        const val NOTIFICATION_ID_COMPLETE = 1002
        const val NOTIFICATION_ID_FAILED = 1003
        
        lateinit var instance: RasAldApplication
            private set
    }

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val preferenceManager: PreferenceManager by lazy { PreferenceManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Python (yt-dlp)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Initialize preferences
        preferenceManager.init()

        // Setup theme
        setupTheme()

        // Create notification channels
        createNotificationChannels()

        Timber.d("RAS ALD Application initialized")
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

    private fun setupTheme() {
        val darkMode = preferenceManager.getDarkMode()
        AppCompatDelegate.setDefaultNightMode(
            when (darkMode) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Download Channel
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_ID,
                getString(R.string.channel_download_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_download_description)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            // General Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL_ID,
                getString(R.string.channel_general_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_general_description)
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(downloadChannel, generalChannel))
        }
    }
}
