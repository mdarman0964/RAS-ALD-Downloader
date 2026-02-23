package com.arman.rasald.ui.viewmodels

import androidx.lifecycle.*
import com.arman.rasald.data.entity.DownloadItem
import com.arman.rasald.data.entity.DownloadStatus
import com.arman.rasald.service.DownloadService
import com.arman.rasald.utils.PreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Download ViewModel
 * Manages download operations and UI state
 */
class DownloadViewModel(
    private val downloadService: DownloadService,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    // Events
    private val _downloadEvent = MutableLiveData<DownloadEvent>()
    val downloadEvent: LiveData<DownloadEvent> = _downloadEvent

    // All downloads
    val allDownloads: StateFlow<List<DownloadItem>> = downloadService.allDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Active downloads (downloading, queued, pending)
    val activeDownloads: StateFlow<List<DownloadItem>> = downloadService.activeDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Completed downloads
    val completedDownloads: StateFlow<List<DownloadItem>> = downloadService.completedDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Failed downloads
    val failedDownloads: StateFlow<List<DownloadItem>> = downloadService.failedDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Statistics
    val totalDownloadedSize: StateFlow<Long> = downloadService.totalDownloadedSize
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val completedCount: StateFlow<Int> = downloadService.completedCount
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            downloadService.downloadEvents.collect { event ->
                _downloadEvent.value = event
            }
        }
    }

    // Add download
    fun addDownload(download: DownloadItem) {
        viewModelScope.launch {
            try {
                downloadService.addDownload(download)
                _downloadEvent.value = DownloadEvent.DownloadStarted
            } catch (e: Exception) {
                Timber.e(e, "Failed to add download")
                _downloadEvent.value = DownloadEvent.ShowError(e.message ?: "Unknown error")
            }
        }
    }

    // Add multiple downloads (batch)
    fun addDownloads(downloads: List<DownloadItem>) {
        viewModelScope.launch {
            try {
                downloads.forEach { downloadService.addDownload(it) }
                _downloadEvent.value = DownloadEvent.ShowSuccess("${downloads.size} downloads added to queue")
            } catch (e: Exception) {
                Timber.e(e, "Failed to add batch downloads")
                _downloadEvent.value = DownloadEvent.ShowError(e.message ?: "Unknown error")
            }
        }
    }

    // Pause download
    fun pauseDownload(id: Long) {
        viewModelScope.launch {
            downloadService.pauseDownload(id)
        }
    }

    // Resume download
    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            downloadService.resumeDownload(id)
        }
    }

    // Cancel download
    fun cancelDownload(id: Long) {
        viewModelScope.launch {
            downloadService.cancelDownload(id)
        }
    }

    // Retry download
    fun retryDownload(id: Long) {
        viewModelScope.launch {
            downloadService.retryDownload(id)
        }
    }

    // Delete download
    fun deleteDownload(download: DownloadItem) {
        viewModelScope.launch {
            downloadService.deleteDownload(download)
        }
    }

    // Pause all downloads
    fun pauseAllDownloads() {
        viewModelScope.launch {
            downloadService.pauseAllDownloads()
        }
    }

    // Resume all downloads
    fun resumeAllDownloads() {
        viewModelScope.launch {
            downloadService.resumeAllDownloads()
        }
    }

    // Cancel all active downloads
    fun cancelAllActiveDownloads() {
        viewModelScope.launch {
            downloadService.cancelAllActiveDownloads()
        }
    }

    // Retry all failed downloads
    fun retryAllFailedDownloads() {
        viewModelScope.launch {
            downloadService.retryAllFailedDownloads()
        }
    }

    // Clear completed downloads
    fun clearCompletedDownloads() {
        viewModelScope.launch {
            downloadService.clearCompletedDownloads()
        }
    }

    // Search downloads
    fun searchDownloads(query: String): Flow<List<DownloadItem>> {
        return downloadService.searchDownloads(query)
    }

    // Get download by ID
    suspend fun getDownloadById(id: Long): DownloadItem? {
        return downloadService.getDownloadById(id)
    }

    // Check if video is downloaded
    suspend fun isDownloaded(videoId: String): Boolean {
        return downloadService.isDownloaded(videoId)
    }

    // Check if video is downloading
    suspend fun isDownloading(videoId: String): Boolean {
        return downloadService.isDownloading(videoId)
    }

    // Events
    sealed class DownloadEvent {
        data class ShowError(val message: String) : DownloadEvent()
        data class ShowSuccess(val message: String) : DownloadEvent()
        object DownloadStarted : DownloadEvent()
        object DownloadCompleted : DownloadEvent()
    }
}
