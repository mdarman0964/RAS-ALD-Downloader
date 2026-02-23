package com.arman.rasald.utils

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.PyObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * yt-dlp Helper Class for Video Download Operations
 * Uses Chaquopy Python integration
 */
class YtdlpHelper(private val context: Context) {

    private val python: Python = Python.getInstance()
    private val ytdlpModule: PyObject = python.getModule("yt_dlp")

    companion object {
        const val TAG = "YtdlpHelper"
    }

    /**
     * Extract video information without downloading
     */
    suspend fun extractInfo(url: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            val ydlOpts = mapOf(
                "quiet" to true,
                "no_warnings" to true,
                "extract_flat" to false,
                "skip_download" to true
            )

            val result = ytdlpModule.callAttr(
                "YoutubeDL",
                ydlOpts.toPyDict()
            ).callAttr("extract_info", url, false)

            result?.let { parseVideoInfo(it.toString()) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract info for URL: $url")
            null
        }
    }

    /**
     * Extract playlist information
     */
    suspend fun extractPlaylistInfo(url: String): PlaylistInfo? = withContext(Dispatchers.IO) {
        try {
            val ydlOpts = mapOf(
                "quiet" to true,
                "no_warnings" to true,
                "extract_flat" to true,
                "skip_download" to true
            )

            val result = ytdlpModule.callAttr(
                "YoutubeDL",
                ydlOpts.toPyDict()
            ).callAttr("extract_info", url, false)

            result?.let { parsePlaylistInfo(it.toString()) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract playlist info for URL: $url")
            null
        }
    }

    /**
     * Get available formats for a video
     */
    suspend fun getFormats(url: String): List<VideoFormat>> = withContext(Dispatchers.IO) {
        try {
            val info = extractInfo(url) ?: return@withContext emptyList()
            
            val formats = mutableListOf<VideoFormat>()
            
            // Video + Audio formats
            info.formats?.forEach { format ->
                if (format.vcodec != "none" && format.acodec != "none") {
                    formats.add(format)
                }
            }
            
            // Audio only formats
            info.formats?.forEach { format ->
                if (format.vcodec == "none" && format.acodec != "none") {
                    formats.add(format)
                }
            }
            
            formats.sortedByDescending { it.quality }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get formats for URL: $url")
            emptyList()
        }
    }

    /**
     * Get available subtitles
     */
    suspend fun getSubtitles(url: String): Map<String, List<SubtitleInfo>> = withContext(Dispatchers.IO) {
        try {
            val info = extractInfo(url) ?: return@withContext emptyMap()
            info.subtitles ?: emptyMap()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get subtitles for URL: $url")
            emptyMap()
        }
    }

    /**
     * Download video with progress callback
     */
    suspend fun download(
        url: String,
        outputPath: String,
        options: DownloadOptions,
        onProgress: (DownloadProgress) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val downloadDir = File(outputPath)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val ydlOpts = buildDownloadOptions(options, outputPath, onProgress)

            val ydl = ytdlpModule.callAttr("YoutubeDL", ydlOpts.toPyDict())
            
            ydl.callAttr("download", listOf(url).toPyList())

            // Find downloaded file
            val downloadedFile = findDownloadedFile(outputPath, options)
            
            if (downloadedFile != null && downloadedFile.exists()) {
                Result.success(downloadedFile.absolutePath)
            } else {
                Result.failure(Exception("Download completed but file not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Download failed for URL: $url")
            Result.failure(e)
        }
    }

    /**
     * Build yt-dlp options
     */
    private fun buildDownloadOptions(
        options: DownloadOptions,
        outputPath: String,
        onProgress: (DownloadProgress) -> Unit
    ): Map<String, Any?> {
        val opts = mutableMapOf<String, Any?>()

        // Basic options
        opts["outtmpl"] = "$outputPath/%(title)s.%(ext)s"
        opts["quiet"] = true
        opts["no_warnings"] = false
        opts["progress_hooks"] = listOf(createProgressHook(onProgress))

        // Format selection
        opts["format"] = when {
            options.extractAudio -> "bestaudio/best"
            options.quality == "best" -> "best"
            options.quality == "worst" -> "worst"
            else -> "best[height<=${options.quality.replace("p", "")}][ext=${options.format}]/best[height<=${options.quality.replace("p", "")}]/best"
        }

        // Audio extraction
        if (options.extractAudio) {
            opts["postprocessors"] = listOf(
                mapOf(
                    "key" to "FFmpegExtractAudio",
                    "preferredcodec" to options.audioFormat,
                    "preferredquality" to options.audioQuality
                )
            )
        }

        // Subtitles
        if (options.downloadSubtitle) {
            opts["writesubtitles"] = true
            opts["writeautomaticsub"] = options.writeAutoSub
            opts["subtitleslangs"] = options.subtitleLanguages
            opts["subtitlesformat"] = options.subtitleFormat
            if (options.embedSubtitle) {
                opts["postprocessors"] = (opts["postprocessors"] as? List ?: emptyList()) +
                    mapOf("key" to "FFmpegEmbedSubtitle")
            }
        }

        // Thumbnail
        if (options.writeThumbnail) {
            opts["writethumbnail"] = true
        }

        // Metadata
        if (options.writeInfoJson) {
            opts["writeinfojson"] = true
        }

        // Cookie
        if (options.cookieFile != null) {
            opts["cookiefile"] = options.cookieFile
        }

        // Proxy
        if (options.proxy != null) {
            opts["proxy"] = options.proxy
        }

        return opts
    }

    private fun createProgressHook(onProgress: (DownloadProgress) -> Unit): PyObject {
        val hook = python.getModule("builtins").callAttr(
            "compile",
            """
            def hook(d):
                if d['status'] == 'downloading':
                    progress = {
                        'status': 'downloading',
                        'downloaded_bytes': d.get('downloaded_bytes', 0),
                        'total_bytes': d.get('total_bytes') or d.get('total_bytes_estimate', 0),
                        'speed': d.get('speed', 0),
                        'eta': d.get('eta', 0),
                        'percent': (d.get('downloaded_bytes', 0) / d.get('total_bytes', 1)) * 100 if d.get('total_bytes') else 0
                    }
                    print(f"PROGRESS:{progress}")
                elif d['status'] == 'finished':
                    print("PROGRESS:{'status': 'finished'}")
            hook
            """.trimIndent(),
            "<string>",
            "exec"
        )
        return hook
    }

    private fun findDownloadedFile(outputPath: String, options: DownloadOptions): File? {
        val dir = File(outputPath)
        if (!dir.exists()) return null

        val extension = if (options.extractAudio) options.audioFormat else options.format
        return dir.listFiles()?.find { it.extension.equals(extension, ignoreCase = true) }
    }

    private fun parseVideoInfo(jsonString: String): VideoInfo {
        val json = JSONObject(jsonString)
        return VideoInfo(
            id = json.optString("id"),
            title = json.optString("title"),
            description = json.optString("description"),
            thumbnail = json.optString("thumbnail"),
            duration = json.optLong("duration"),
            uploader = json.optString("uploader"),
            uploadDate = json.optString("upload_date"),
            viewCount = json.optLong("view_count"),
            likeCount = json.optLong("like_count"),
            webpageUrl = json.optString("webpage_url"),
            formats = parseFormats(json.optJSONArray("formats")),
            subtitles = parseSubtitles(json.optJSONObject("subtitles")),
            thumbnailUrls = parseThumbnails(json.optJSONArray("thumbnails"))
        )
    }

    private fun parsePlaylistInfo(jsonString: String): PlaylistInfo {
        val json = JSONObject(jsonString)
        val entries = json.optJSONArray("entries")
        val videos = mutableListOf<PlaylistVideo>()

        entries?.let {
            for (i in 0 until it.length()) {
                val entry = it.optJSONObject(i)
                videos.add(
                    PlaylistVideo(
                        id = entry?.optString("id") ?: "",
                        title = entry?.optString("title") ?: "",
                        duration = entry?.optLong("duration") ?: 0,
                        uploader = entry?.optString("uploader") ?: ""
                    )
                )
            }
        }

        return PlaylistInfo(
            id = json.optString("id"),
            title = json.optString("title"),
            description = json.optString("description"),
            uploader = json.optString("uploader"),
            videoCount = json.optInt("playlist_count", videos.size),
            videos = videos
        )
    }

    private fun parseFormats(jsonArray: org.json.JSONArray?): List<VideoFormat> {
        val formats = mutableListOf<VideoFormat>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                val format = it.optJSONObject(i)
                formats.add(
                    VideoFormat(
                        formatId = format?.optString("format_id") ?: "",
                        ext = format?.optString("ext") ?: "",
                        quality = format?.optString("quality") ?: "",
                        width = format?.optInt("width") ?: 0,
                        height = format?.optInt("height") ?: 0,
                        filesize = format?.optLong("filesize") ?: 0,
                        filesizeApprox = format?.optLong("filesize_approx") ?: 0,
                        vcodec = format?.optString("vcodec") ?: "",
                        acodec = format?.optString("acodec") ?: "",
                        abr = format?.optDouble("abr") ?: 0.0,
                        vbr = format?.optDouble("vbr") ?: 0.0,
                        fps = format?.optInt("fps") ?: 0,
                        asr = format?.optInt("asr") ?: 0
                    )
                )
            }
        }
        return formats
    }

    private fun parseSubtitles(jsonObject: org.json.JSONObject?): Map<String, List<SubtitleInfo>> {
        val subtitles = mutableMapOf<String, List<SubtitleInfo>>()
        jsonObject?.let {
            val keys = it.keys()
            while (keys.hasNext()) {
                val lang = keys.next()
                val subs = mutableListOf<SubtitleInfo>()
                val langArray = it.optJSONArray(lang)
                langArray?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val sub = arr.optJSONObject(i)
                        subs.add(
                            SubtitleInfo(
                                url = sub?.optString("url") ?: "",
                                ext = sub?.optString("ext") ?: ""
                            )
                        )
                    }
                }
                subtitles[lang] = subs
            }
        }
        return subtitles
    }

    private fun parseThumbnails(jsonArray: org.json.JSONArray?): List<String> {
        val thumbnails = mutableListOf<String>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                val thumb = it.optJSONObject(i)
                thumb?.optString("url")?.let { url ->
                    if (url.isNotEmpty()) thumbnails.add(url)
                }
            }
        }
        return thumbnails
    }

    // Extension functions for Python conversion
    private fun Map<String, Any?>.toPyDict(): PyObject {
        val dict = python.builtins.callAttr("dict")
        forEach { (key, value) ->
            dict.callAttr("__setitem__", key, value?.toPyObject())
        }
        return dict
    }

    private fun List<*>.toPyList(): PyObject {
        val list = python.builtins.callAttr("list")
        forEach { item ->
            list.callAttr("append", item?.toPyObject())
        }
        return list
    }

    private fun Any?.toPyObject(): PyObject? {
        return when (this) {
            null -> null
            is String -> python.builtins.callAttr("str", this)
            is Int -> python.builtins.callAttr("int", this)
            is Long -> python.builtins.callAttr("int", this)
            is Double -> python.builtins.callAttr("float", this)
            is Boolean -> python.builtins.callAttr("bool", this)
            is Map<*, *> -> (this as Map<String, Any?>).toPyDict()
            is List<*> -> this.toPyList()
            else -> python.builtins.callAttr("str", this.toString())
        }
    }
}

// Data Classes

data class VideoInfo(
    val id: String,
    val title: String,
    val description: String?,
    val thumbnail: String?,
    val duration: Long,
    val uploader: String?,
    val uploadDate: String?,
    val viewCount: Long,
    val likeCount: Long,
    val webpageUrl: String,
    val formats: List<VideoFormat>?,
    val subtitles: Map<String, List<SubtitleInfo>>?,
    val thumbnailUrls: List<String>?
)

data class VideoFormat(
    val formatId: String,
    val ext: String,
    val quality: String,
    val width: Int,
    val height: Int,
    val filesize: Long,
    val filesizeApprox: Long,
    val vcodec: String,
    val acodec: String,
    val abr: Double,
    val vbr: Double,
    val fps: Int,
    val asr: Int
)

data class SubtitleInfo(
    val url: String,
    val ext: String
)

data class PlaylistInfo(
    val id: String,
    val title: String,
    val description: String?,
    val uploader: String?,
    val videoCount: Int,
    val videos: List<PlaylistVideo>
)

data class PlaylistVideo(
    val id: String,
    val title: String,
    val duration: Long,
    val uploader: String
)

data class DownloadOptions(
    val quality: String = "720p",
    val format: String = "mp4",
    val extractAudio: Boolean = false,
    val audioFormat: String = "mp3",
    val audioQuality: String = "192",
    val downloadSubtitle: Boolean = false,
    val subtitleLanguages: List<String> = listOf("en"),
    val subtitleFormat: String = "srt",
    val writeAutoSub: Boolean = false,
    val embedSubtitle: Boolean = false,
    val writeThumbnail: Boolean = false,
    val writeInfoJson: Boolean = false,
    val cookieFile: String? = null,
    val proxy: String? = null
)

data class DownloadProgress(
    val status: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Double,
    val eta: Int,
    val percent: Double
)
