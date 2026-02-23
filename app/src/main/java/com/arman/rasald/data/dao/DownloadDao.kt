package com.arman.rasald.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.arman.rasald.data.entity.DownloadItem
import com.arman.rasald.data.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Download operations
 */
@Dao
interface DownloadDao {

    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(downloads: List<DownloadItem>): List<Long>

    // Update operations
    @Update
    suspend fun update(download: DownloadItem)

    @Update
    suspend fun updateAll(downloads: List<DownloadItem>)

    @Query("UPDATE downloads SET progress = :progress, downloadedSize = :downloadedSize, speed = :speed, eta = :eta WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int, downloadedSize: Long, speed: String?, eta: String?)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus)

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateFailed(id: Long, status: DownloadStatus = DownloadStatus.FAILED, errorMessage: String?)

    @Query("UPDATE downloads SET status = :status, filePath = :filePath, fileSize = :fileSize, completedAt = :completedAt WHERE id = :id")
    suspend fun updateCompleted(id: Long, status: DownloadStatus = DownloadStatus.COMPLETED, filePath: String?, fileSize: Long, completedAt: Long)

    // Delete operations
    @Delete
    suspend fun delete(download: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun deleteByStatus(status: DownloadStatus)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()

    // Query operations
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloadsLive(): LiveData<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadItem?

    @Query("SELECT * FROM downloads WHERE videoId = :videoId ORDER BY createdAt DESC")
    fun getDownloadsByVideoId(videoId: String): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatusLive(status: DownloadStatus): LiveData<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status IN (:statuses) ORDER BY createdAt DESC")
    fun getDownloadsByStatuses(vararg statuses: DownloadStatus): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE playlistId = :playlistId ORDER BY playlistIndex ASC")
    fun getDownloadsByPlaylist(playlistId: String): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE batchId = :batchId ORDER BY batchPosition ASC")
    fun getDownloadsByBatch(batchId: String): Flow<List<DownloadItem>>

    // Search
    @Query("SELECT * FROM downloads WHERE title LIKE '%' || :query || '%' OR uploader LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchDownloads(query: String): Flow<List<DownloadItem>>

    // Active downloads
    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADING', 'QUEUED', 'MERGING', 'CONVERTING') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADING', 'QUEUED', 'MERGING', 'CONVERTING') ORDER BY createdAt DESC")
    fun getActiveDownloadsLive(): LiveData<List<DownloadItem>>

    // Completed downloads
    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadItem>>

    // Failed downloads
    @Query("SELECT * FROM downloads WHERE status = 'FAILED' ORDER BY createdAt DESC")
    fun getFailedDownloads(): Flow<List<DownloadItem>>

    // Statistics
    @Query("SELECT COUNT(*) FROM downloads")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'FAILED'")
    suspend fun getFailedCount(): Int

    @Query("SELECT SUM(fileSize) FROM downloads WHERE status = 'COMPLETED'")
    suspend fun getTotalDownloadedSize(): Long?

    @Query("SELECT * FROM downloads WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextPendingDownload(): DownloadItem?

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextQueuedDownload(): DownloadItem?

    // Queue management
    @Query("UPDATE downloads SET status = 'QUEUED' WHERE status = 'PENDING'")
    suspend fun queueAllPending()

    @Query("UPDATE downloads SET status = 'PENDING' WHERE status = 'QUEUED'")
    suspend fun unqueueAll()

    @Query("UPDATE downloads SET status = 'CANCELLED' WHERE status IN ('DOWNLOADING', 'QUEUED', 'PENDING')")
    suspend fun cancelAllActive()

    // Retry failed downloads
    @Query("UPDATE downloads SET status = 'PENDING', retryCount = 0, errorMessage = NULL WHERE status = 'FAILED'")
    suspend fun retryAllFailed()

    // Batch operations
    @Query("UPDATE downloads SET status = 'PENDING' WHERE batchId = :batchId")
    suspend fun retryBatch(batchId: String)

    @Query("DELETE FROM downloads WHERE batchId = :batchId")
    suspend fun deleteBatch(batchId: String)

    // Check if exists
    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE videoId = :videoId AND status = 'COMPLETED')")
    suspend fun isDownloaded(videoId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE videoId = :videoId AND status IN ('DOWNLOADING', 'QUEUED', 'PENDING'))")
    suspend fun isDownloading(videoId: String): Boolean
}
