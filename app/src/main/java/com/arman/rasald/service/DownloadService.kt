package com.arman.rasald.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arman.rasald.R
import com.arman.rasald.RasAldApplication
import com.arman.rasald.data.database.AppDatabase
import com.arman.rasald.data.entity.DownloadItem
import com.arman.rasald.data.entity.DownloadStatus
import com.arman.rasald.ui.activities.MainActivity
import com.arman.rasald.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Download Service for background downloads
 */
class DownloadService : Service() {

    private val binder = DownloadBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ytdlpHelper: YtdlpHelper by lazy { YtdlpHelper(this) }
    private val preferenceManager: PreferenceManager by lazy { PreferenceManager(this) }
    
    private val downloadJobs = ConcurrentHashMap<Long, Job>()
    private val _downloadEvents = MutableSharedFlow<DownloadEvent>()
    val downloadEvents: SharedFlow<DownloadEvent> = _downloadEvents.asSharedFlow()

    private val database by lazy { (application as RasAldApplication).database }
    private val downloadDao by lazy { database.downloadDao() }

    // Flows
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadItem>> = downloadDao.getActiveDownloads()
    val completedDownloads: Flow<List<DownloadItem>> = downloadDao.getCompletedDownloads()
    val failedDownloads: Flow<List<DownloadItem>> = downloadDao.getFailedDownloads()
    val totalDownloadedSize: Flow<Long> = flow { emit(downloadDao.getTotalDownloadedSize() ?: 0L) }
    val completedCount: Flow<Int> = flow { emit(downloadDao.getCompletedCount()) }

    companion object {
        const val ACTION_START_DOWNLOAD = "action_start_download"
        const val ACTION_PAUSE_DOWNLOAD = "action_pause_download"
        const val ACTION_RESUME_DOWNLOAD = "action_resume_download"
        const val ACTION_CANCEL_DOWNLOAD = "action_cancel_download"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }

    inner class DownloadBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("DownloadService created")
        startForegroundService()
        processPendingDownloads()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    serviceScope.launch {
                        downloadDao.getDownloadById(downloadId)?.let {
                            startDownload(it)
                        }
                    }
                }
            }
            ACTION_PAUSE_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    pauseDownload(downloadId)
                }
            }
            ACTION_RESUME_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    serviceScope.launch {
                        downloadDao.getDownloadById(downloadId)?.let {
                            resumeDownload(it.id)
                        }
                    }
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    cancelDownload(downloadId)
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createServiceNotification()
        startForeground(RasAldApplication.NOTIFICATION_ID_DOWNLOAD, notification)
    }

    private fun createServiceNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, RasAldApplication.CHANNEL_DOWNLOAD_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.download_service_running))
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun processPendingDownloads() {
        serviceScope.launch {
            val pending = downloadDao.getNextPendingDownload()
            pending?.let {
                if (shouldStartDownload()) {
                    startDownload(it)
                }
            }
        }
    }

    private fun shouldStartDownload(): Boolean {
        if (preferenceManager.isWifiOnly()) {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo?.type == android.net.ConnectivityManager.TYPE_WIFI
        }
        return true
    }

    suspend fun addDownload(download: DownloadItem) {
        val id = downloadDao.insert(download)
        _downloadEvents.emit(DownloadEvent.DownloadAdded)
        
        // Start download immediately if conditions are met
        if (shouldStartDownload()) {
            downloadDao.getDownloadById(id)?.let {
                startDownload(it)
            }
        }
    }

    fun startDownload(download: DownloadItem) {
        if (downloadJobs.containsKey(download.id)) return

        val job = serviceScope.launch {
            try {
                downloadDao.updateStatus(download.id, DownloadStatus.DOWNLOADING)
                downloadDao.update(download.copy(startedAt = java.util.Date()))

                val options = DownloadOptions(
                    quality = download.quality,
                    format = download.format,
                    extractAudio = download.downloadType == com.arman.rasald.data.entity.DownloadType.AUDIO,
                    audioFormat = download.audioFormat ?: "mp3",
                    downloadSubtitle = download.subtitleUrl != null,
                    subtitleLanguages = download.subtitleLanguage?.let { listOf(it) } ?: listOf("en"),
                    writeThumbnail = preferenceManager.isWriteThumbnail(),
                    writeInfoJson = preferenceManager.isWriteInfoJson()
                )

                val outputPath = preferenceManager.getDownloadPath()

                ytdlpHelper.download(
                    url = download.url,
                    outputPath = outputPath,
                    options = options,
                    onProgress = { progress ->
                        serviceScope.launch {
                            val downloadedSize = (progress.percent / 100 * download.fileSize).toLong()
                            downloadDao.updateProgress(
                                download.id,
                                progress.percent.toInt(),
                                downloadedSize,
                                formatSpeed(progress.speed),
                                formatEta(progress.eta)
                            )
                            updateNotification(download, progress.percent.toInt())
                        }
                    }
                ).fold(
                    onSuccess = { filePath ->
                        val file = File(filePath)
                        downloadDao.updateCompleted(
                            download.id,
                            DownloadStatus.COMPLETED,
                            filePath,
                            file.length(),
                            System.currentTimeMillis()
                        )
                        _downloadEvents.emit(DownloadEvent.DownloadCompleted(download.title))
                        showCompletionNotification(download)
                    },
                    onFailure = { error ->
                        handleDownloadError(download, error)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Download error")
                handleDownloadError(download, e)
            } finally {
                downloadJobs.remove(download.id)
                processNextDownload()
            }
        }

        downloadJobs[download.id] = job
    }

    private suspend fun handleDownloadError(download: DownloadItem, error: Throwable) {
        val retryCount = download.retryCount + 1
        val maxRetry = preferenceManager.getMaxRetry()

        if (retryCount < maxRetry && preferenceManager.isAutoRetry()) {
            downloadDao.update(download.copy(retryCount = retryCount))
            delay(5000) // Wait 5 seconds before retry
            downloadDao.getDownloadById(download.id)?.let {
                startDownload(it)
            }
        } else {
            downloadDao.updateFailed(download.id, DownloadStatus.FAILED, error.message)
            _downloadEvents.emit(DownloadEvent.DownloadFailed(download.title, error.message ?: "Unknown error"))
            showErrorNotification(download, error.message ?: "Unknown error")
        }
    }

    private fun processNextDownload() {
        serviceScope.launch {
            val activeCount = downloadDao.getActiveDownloads().first().count {
                it.status == DownloadStatus.DOWNLOADING
            }
            val maxConcurrent = preferenceManager.getConcurrentDownloads()

            if (activeCount < maxConcurrent) {
                downloadDao.getNextQueuedDownload()?.let {
                    startDownload(it)
                }
            }
        }
    }

    fun pauseDownload(id: Long) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        serviceScope.launch {
            downloadDao.updateStatus(id, DownloadStatus.PAUSED)
        }
    }

    fun resumeDownload(id: Long) {
        serviceScope.launch {
            downloadDao.updateStatus(id, DownloadStatus.QUEUED)
            downloadDao.getDownloadById(id)?.let {
                startDownload(it)
            }
        }
    }

    fun cancelDownload(id: Long) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        serviceScope.launch {
            downloadDao.updateStatus(id, DownloadStatus.CANCELLED)
        }
    }

    suspend fun deleteDownload(download: DownloadItem) {
        downloadJobs[download.id]?.cancel()
        downloadJobs.remove(download.id)

        // Delete file if exists
        download.filePath?.let { path ->
            File(path).delete()
        }

        downloadDao.delete(download)
    }

    fun pauseAllDownloads() {
        downloadJobs.forEach { (id, job) ->
            job.cancel()
            serviceScope.launch {
                downloadDao.updateStatus(id, DownloadStatus.PAUSED)
            }
        }
        downloadJobs.clear()
    }

    fun resumeAllDownloads() {
        serviceScope.launch {
            downloadDao.queueAllPending()
            processNextDownload()
        }
    }

    fun cancelAllActiveDownloads() {
        downloadJobs.forEach { (id, job) ->
            job.cancel()
            serviceScope.launch {
                downloadDao.updateStatus(id, DownloadStatus.CANCELLED)
            }
        }
        downloadJobs.clear()
    }

    fun retryAllFailedDownloads() {
        serviceScope.launch {
            downloadDao.retryAllFailed()
            processNextDownload()
        }
    }

    fun clearCompletedDownloads() {
        serviceScope.launch {
            downloadDao.deleteByStatus(DownloadStatus.COMPLETED)
        }
    }

    fun searchDownloads(query: String): Flow<List<DownloadItem>> {
        return downloadDao.searchDownloads(query)
    }

    suspend fun getDownloadById(id: Long): DownloadItem? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun isDownloaded(videoId: String): Boolean {
        return downloadDao.isDownloaded(videoId)
    }

    suspend fun isDownloading(videoId: String): Boolean {
        return downloadDao.isDownloading(videoId)
    }

    private fun updateNotification(download: DownloadItem, progress: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            download.id.toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, RasAldApplication.CHANNEL_DOWNLOAD_ID)
            .setContentTitle(download.title)
            .setContentText("$progress% - ${download.speed ?: ""}")
            .setSmallIcon(R.drawable.ic_download)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        notificationManager.notify(download.id.toInt(), notification)
    }

    private fun showCompletionNotification(download: DownloadItem) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val pendingIntent = PendingIntent.getActivity(
            this,
            download.id.toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, RasAldApplication.CHANNEL_GENERAL_ID)
            .setContentTitle(getString(R.string.download_complete))
            .setContentText(download.title)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(RasAldApplication.NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun showErrorNotification(download: DownloadItem, error: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, RasAldApplication.CHANNEL_GENERAL_ID)
            .setContentTitle(getString(R.string.download_failed))
            .setContentText("${download.title}: $error")
            .setSmallIcon(R.drawable.ic_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(RasAldApplication.NOTIFICATION_ID_FAILED, notification)
    }

    private fun formatSpeed(bytesPerSecond: Double): String {
        val kbps = bytesPerSecond / 1024.0
        val mbps = kbps / 1024.0
        return when {
            mbps >= 1 -> String.format("%.2f MB/s", mbps)
            kbps >= 1 -> String.format("%.2f KB/s", kbps)
            else -> String.format("%.2f B/s", bytesPerSecond)
        }
    }

    private fun formatEta(seconds: Int): String {
        if (seconds <= 0) return "Unknown"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        serviceScope.cancel()
    }

    // Events
    sealed class DownloadEvent {
        object DownloadAdded : DownloadEvent()
        data class DownloadCompleted(val title: String) : DownloadEvent()
        data class DownloadFailed(val title: String, val error: String) : DownloadEvent()
    }
}
