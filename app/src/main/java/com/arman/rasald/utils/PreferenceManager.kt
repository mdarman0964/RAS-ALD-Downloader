package com.arman.rasald.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.arman.rasald.R
import java.io.File

/**
 * Preference Manager for App Settings
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val appContext = context.applicationContext

    companion object {
        // Keys
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_DOWNLOAD_PATH = "download_path"
        const val KEY_DEFAULT_QUALITY = "default_quality"
        const val KEY_DEFAULT_FORMAT = "default_format"
        const val KEY_WIFI_ONLY = "wifi_only"
        const val KEY_AUTO_RETRY = "auto_retry"
        const val KEY_MAX_RETRY = "max_retry"
        const val KEY_CONCURRENT_DOWNLOADS = "concurrent_downloads"
        const val KEY_NOTIFICATION_SOUND = "notification_sound"
        const val KEY_LANGUAGE = "language"
        const val KEY_SHOW_THUMBNAIL = "show_thumbnail"
        const val KEY_KEEP_HISTORY = "keep_history"
        const val KEY_DELETE_ON_COMPLETE = "delete_on_complete"
        const val KEY_EXTRACT_AUDIO = "extract_audio"
        const val KEY_AUDIO_QUALITY = "audio_quality"
        const val KEY_EMBED_SUBTITLE = "embed_subtitle"
        const val KEY_WRITE_THUMBNAIL = "write_thumbnail"
        const val KEY_WRITE_INFO_JSON = "write_info_json"

        // Default values
        const val DEFAULT_QUALITY = "720p"
        const val DEFAULT_FORMAT = "mp4"
        const val DEFAULT_AUDIO_FORMAT = "mp3"
        const val DEFAULT_CONCURRENT_DOWNLOADS = 3
        const val DEFAULT_MAX_RETRY = 3
        const val DEFAULT_LANGUAGE = "bn"
    }

    fun init() {
        PreferenceManager.setDefaultValues(appContext, R.xml.preferences, false)
        if (isFirstLaunch()) {
            setFirstLaunch(false)
            setupDefaultDownloadPath()
        }
    }

    private fun setupDefaultDownloadPath() {
        val defaultPath = File(appContext.getExternalFilesDir(null), "Downloads").absolutePath
        prefs.edit { putString(KEY_DOWNLOAD_PATH, defaultPath) }
    }

    // First Launch
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    fun setFirstLaunch(value: Boolean) = prefs.edit { putBoolean(KEY_FIRST_LAUNCH, value) }

    // Dark Mode
    fun getDarkMode(): String = prefs.getString(KEY_DARK_MODE, "system") ?: "system"
    fun setDarkMode(mode: String) = prefs.edit { putString(KEY_DARK_MODE, mode) }
    fun isDarkModeEnabled(): Boolean = getDarkMode() == "dark"

    // Download Path
    fun getDownloadPath(): String {
        return prefs.getString(KEY_DOWNLOAD_PATH, null)
            ?: File(appContext.getExternalFilesDir(null), "Downloads").absolutePath
    }
    fun setDownloadPath(path: String) = prefs.edit { putString(KEY_DOWNLOAD_PATH, path) }

    // Default Quality
    fun getDefaultQuality(): String = prefs.getString(KEY_DEFAULT_QUALITY, DEFAULT_QUALITY) ?: DEFAULT_QUALITY
    fun setDefaultQuality(quality: String) = prefs.edit { putString(KEY_DEFAULT_QUALITY, quality) }

    // Default Format
    fun getDefaultFormat(): String = prefs.getString(KEY_DEFAULT_FORMAT, DEFAULT_FORMAT) ?: DEFAULT_FORMAT
    fun setDefaultFormat(format: String) = prefs.edit { putString(KEY_DEFAULT_FORMAT, format) }

    // WiFi Only
    fun isWifiOnly(): Boolean = prefs.getBoolean(KEY_WIFI_ONLY, false)
    fun setWifiOnly(enabled: Boolean) = prefs.edit { putBoolean(KEY_WIFI_ONLY, enabled) }

    // Auto Retry
    fun isAutoRetry(): Boolean = prefs.getBoolean(KEY_AUTO_RETRY, true)
    fun setAutoRetry(enabled: Boolean) = prefs.edit { putBoolean(KEY_AUTO_RETRY, enabled) }

    // Max Retry
    fun getMaxRetry(): Int = prefs.getInt(KEY_MAX_RETRY, DEFAULT_MAX_RETRY)
    fun setMaxRetry(count: Int) = prefs.edit { putInt(KEY_MAX_RETRY, count) }

    // Concurrent Downloads
    fun getConcurrentDownloads(): Int = prefs.getInt(KEY_CONCURRENT_DOWNLOADS, DEFAULT_CONCURRENT_DOWNLOADS)
    fun setConcurrentDownloads(count: Int) = prefs.edit { putInt(KEY_CONCURRENT_DOWNLOADS, count) }

    // Notification Sound
    fun isNotificationSoundEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_SOUND, true)
    fun setNotificationSound(enabled: Boolean) = prefs.edit { putBoolean(KEY_NOTIFICATION_SOUND, enabled) }

    // Language
    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    fun setLanguage(language: String) = prefs.edit { putString(KEY_LANGUAGE, language) }
    fun isBengali(): Boolean = getLanguage() == "bn"

    // Show Thumbnail
    fun isShowThumbnail(): Boolean = prefs.getBoolean(KEY_SHOW_THUMBNAIL, true)
    fun setShowThumbnail(enabled: Boolean) = prefs.edit { putBoolean(KEY_SHOW_THUMBNAIL, enabled) }

    // Keep History
    fun isKeepHistory(): Boolean = prefs.getBoolean(KEY_KEEP_HISTORY, true)
    fun setKeepHistory(enabled: Boolean) = prefs.edit { putBoolean(KEY_KEEP_HISTORY, enabled) }

    // Delete on Complete
    fun isDeleteOnComplete(): Boolean = prefs.getBoolean(KEY_DELETE_ON_COMPLETE, false)
    fun setDeleteOnComplete(enabled: Boolean) = prefs.edit { putBoolean(KEY_DELETE_ON_COMPLETE, enabled) }

    // Extract Audio
    fun isExtractAudio(): Boolean = prefs.getBoolean(KEY_EXTRACT_AUDIO, false)
    fun setExtractAudio(enabled: Boolean) = prefs.edit { putBoolean(KEY_EXTRACT_AUDIO, enabled) }

    // Audio Quality
    fun getAudioQuality(): String = prefs.getString(KEY_AUDIO_QUALITY, "192") ?: "192"
    fun setAudioQuality(quality: String) = prefs.edit { putString(KEY_AUDIO_QUALITY, quality) }

    // Embed Subtitle
    fun isEmbedSubtitle(): Boolean = prefs.getBoolean(KEY_EMBED_SUBTITLE, false)
    fun setEmbedSubtitle(enabled: Boolean) = prefs.edit { putBoolean(KEY_EMBED_SUBTITLE, enabled) }

    // Write Thumbnail
    fun isWriteThumbnail(): Boolean = prefs.getBoolean(KEY_WRITE_THUMBNAIL, false)
    fun setWriteThumbnail(enabled: Boolean) = prefs.edit { putBoolean(KEY_WRITE_THUMBNAIL, enabled) }

    // Write Info JSON
    fun isWriteInfoJson(): Boolean = prefs.getBoolean(KEY_WRITE_INFO_JSON, false)
    fun setWriteInfoJson(enabled: Boolean) = prefs.edit { putBoolean(KEY_WRITE_INFO_JSON, enabled) }

    // Clear all preferences
    fun clearAll() {
        prefs.edit { clear() }
    }

    // Get all preferences as map
    fun getAllPreferences(): Map<String, *> = prefs.all
}
