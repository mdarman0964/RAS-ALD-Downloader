package com.arman.rasald.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arman.rasald.R
import com.arman.rasald.data.entity.DownloadItem
import com.arman.rasald.data.entity.DownloadStatus
import com.arman.rasald.databinding.ItemDownloadBinding
import com.bumptech.glide.Glide

/**
 * Download Adapter for RecyclerView
 */
class DownloadAdapter(
    private val onItemClick: (DownloadItem) -> Unit = {},
    private val onPlayClick: (DownloadItem) -> Unit = {},
    private val onPauseClick: (DownloadItem) -> Unit = {},
    private val onResumeClick: (DownloadItem) -> Unit = {},
    private val onCancelClick: (DownloadItem) -> Unit = {},
    private val onRetryClick: (DownloadItem) -> Unit = {},
    private val onDeleteClick: (DownloadItem) -> Unit = {},
    private val onShareClick: (DownloadItem) -> Unit = {}
) : ListAdapter<DownloadItem, DownloadAdapter.DownloadViewHolder>(DownloadDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DownloadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DownloadViewHolder(
        private val binding: ItemDownloadBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(download: DownloadItem) {
            binding.apply {
                // Title and uploader
                tvTitle.text = download.title
                tvUploader.text = download.uploader ?: root.context.getString(R.string.unknown_uploader)

                // Thumbnail
                if (!download.thumbnailUrl.isNullOrEmpty()) {
                    Glide.with(root.context)
                        .load(download.thumbnailUrl)
                        .placeholder(R.drawable.ic_video_placeholder)
                        .error(R.drawable.ic_video_placeholder)
                        .into(ivThumbnail)
                } else {
                    ivThumbnail.setImageResource(R.drawable.ic_video_placeholder)
                }

                // Status and progress
                updateStatus(download)

                // Quality and format
                tvQuality.text = "${download.quality} • ${download.format.uppercase()}"

                // File size
                if (download.fileSize > 0) {
                    tvSize.text = formatFileSize(download.fileSize)
                    tvSize.visibility = View.VISIBLE
                } else {
                    tvSize.visibility = View.GONE
                }

                // Click listeners
                root.setOnClickListener { onItemClick(download) }
                btnPlay.setOnClickListener { onPlayClick(download) }
                btnPause.setOnClickListener { onPauseClick(download) }
                btnResume.setOnClickListener { onResumeClick(download) }
                btnCancel.setOnClickListener { onCancelClick(download) }
                btnRetry.setOnClickListener { onRetryClick(download) }
                btnDelete.setOnClickListener { onDeleteClick(download) }
                btnShare.setOnClickListener { onShareClick(download) }
            }
        }

        private fun updateStatus(download: DownloadItem) {
            binding.apply {
                when (download.status) {
                    DownloadStatus.PENDING -> {
                        progressBar.visibility = View.GONE
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = root.context.getString(R.string.pending)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_pending))
                        btnPlay.visibility = View.GONE
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.VISIBLE
                        btnRetry.visibility = View.GONE
                        btnShare.visibility = View.GONE
                    }
                    DownloadStatus.QUEUED -> {
                        progressBar.visibility = View.GONE
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = root.context.getString(R.string.queued)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_queued))
                        btnPlay.visibility = View.GONE
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.VISIBLE
                        btnRetry.visibility = View.GONE
                        btnShare.visibility = View.GONE
                    }
                    DownloadStatus.DOWNLOADING -> {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = download.progress
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = buildString {
                            append("${download.progress}%")
                            download.speed?.let { append(" • $it") }
                            download.eta?.let { append(" • ETA: $it") }
                        }
                        tvStatus.setTextColor(root.context.getColor(R.color.status_downloading))
                        btnPlay.visibility = View.GONE
                        btnPause.visibility = View.VISIBLE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.VISIBLE
                        btnRetry.visibility = View.GONE
                        btnShare.visibility = View.GONE
                    }
                    DownloadStatus.PAUSED -> {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = download.progress
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = root.context.getString(R.string.paused_format, download.progress)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_paused))
                        btnPlay.visibility = View.GONE
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.VISIBLE
                        btnCancel.visibility = View.VISIBLE
                        btnRetry.visibility = View.GONE
                        btnShare.visibility = View.GONE
                    }
                    DownloadStatus.COMPLETED -> {
                        progressBar.visibility = View.GONE
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = root.context.getString(R.string.completed)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_completed))
                        btnPlay.visibility = View.VISIBLE
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.GONE
                        btnRetry.visibility = View.GONE
                        btnShare.visibility = View.VISIBLE
                    }
                    DownloadStatus.FAILED -> {
                        progressBar.visibility = View.GONE
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = root.context.getString(R.string.failed)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_failed))
                        btnPlay.visibility = View.GONE
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.GONE
                        btnRetry.visibility = View.VISIBLE
                        btnShare.visibility = View.GONE
                    }
                    DownloadStatus.CANCELLED -> {
                        progressBar.visibility = View.GONE
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = root.context.getString(R.string.cancelled)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_cancelled))
                        btnPlay.visibility = View.GONE
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.GONE
                        btnRetry.visibility = View.VISIBLE
                        btnShare.visibility = View.GONE
                    }
                    DownloadStatus.MERGING -> {
                        progressBar.visibility = View.VISIBLE
                        progressBar.isIndeterminate = true
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = root.context.getString(R.string.merging)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_merging))
                        btnPlay.visibility = View.GONE
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.GONE
                        btnRetry.visibility = View.GONE
                        btnShare.visibility = View.GONE
                    }
                    DownloadStatus.CONVERTING -> {
                        progressBar.visibility = View.VISIBLE
                        progressBar.isIndeterminate = true
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = root.context.getString(R.string.converting)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_converting))
                        btnPlay.visibility = View.GONE
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.GONE
                        btnRetry.visibility = View.GONE
                        btnShare.visibility = View.GONE
                    }
                }
            }
        }

        private fun formatFileSize(size: Long): String {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1 -> String.format("%.2f GB", gb)
                mb >= 1 -> String.format("%.2f MB", mb)
                kb >= 1 -> String.format("%.2f KB", kb)
                else -> "$size B"
            }
        }
    }

    class DownloadDiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem == newItem
        }
    }
}
