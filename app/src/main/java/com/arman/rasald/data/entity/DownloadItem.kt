package com.arman.rasald.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.arman.rasald.utils.Converters
import java.util.Date

/**
 * Download Item Entity for Room Database
 * Stores all download information
 */
@Entity(tableName = "downloads")
@TypeConverters(Converters::class)
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Video Information
    val videoId: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Long = 0,
    val uploader: String? = null,
    val uploadDate: String? = null,
    
    // Download Configuration
    val url: String,
    val downloadType: DownloadType = DownloadType.VIDEO,
    val quality: String = "720p",
    val format: String = "mp4",
    val audioFormat: String? = null,
    
    // File Information
    val filePath: String? = null,
    val fileSize: Long = 0,
    val downloadedSize: Long = 0,
    
    // Status
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val speed: String? = null,
    val eta: String? = null,
    
    // Subtitle
    val subtitleUrl: String? = null,
    val subtitleLanguage: String? = null,
    val subtitleFormat: String? = null,
    
    // Playlist Info
    val playlistId: String? = null,
    val playlistIndex: Int = 0,
    val playlistTitle: String? = null,
    
    // Timestamps
    val createdAt: Date = Date(),
    val startedAt: Date? = null,
    val completedAt: Date? = null,
    
    // Retry
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    
    // Trim (for video trimmer feature)
    val startTime: Long? = null,
    val endTime: Long? = null,
    
    // Batch download
    val batchId: String? = null,
    val batchPosition: Int = 0
)

enum class DownloadType {
    VIDEO,
    AUDIO,
    SUBTITLE,
    PLAYLIST
}

enum class DownloadStatus {
    PENDING,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    MERGING,
    CONVERTING
}

enum class VideoQuality(val value: String, val height: Int) {
    Q144P("144p", 144),
    Q240P("240p", 240),
    Q360P("360p", 360),
    Q480P("480p", 480),
    Q720P("720p", 720),
    Q1080P("1080p", 1080),
    Q1440P("1440p", 1440),
    Q2160P("4K", 2160),
    Q4320P("8K", 4320),
    BEST("best", 0),
    WORST("worst", 0)
}

enum class AudioFormat(val extension: String) {
    MP3("mp3"),
    M4A("m4a"),
    OPUS("opus"),
    WAV("wav"),
    AAC("aac"),
    FLAC("flac")
}

enum class VideoFormat(val extension: String) {
    MP4("mp4"),
    WEBM("webm"),
    MKV("mkv"),
    FLV("flv"),
    AVI("avi"),
    MOV("mov")
}

enum class SubtitleFormat(val extension: String) {
    SRT("srt"),
    VTT("vtt"),
    ASS("ass"),
    LRC("lrc")
}
