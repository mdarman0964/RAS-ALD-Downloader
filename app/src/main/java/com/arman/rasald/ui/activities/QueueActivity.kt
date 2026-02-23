package com.arman.rasald.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arman.rasald.R
import com.arman.rasald.data.entity.DownloadItem
import com.arman.rasald.data.entity.DownloadStatus
import com.arman.rasald.databinding.ActivityQueueBinding
import com.arman.rasald.ui.adapters.DownloadAdapter
import com.arman.rasald.ui.viewmodels.DownloadViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Download Queue Management Activity
 */
class QueueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQueueBinding
    private val viewModel: DownloadViewModel by viewModel()
    private lateinit var queueAdapter: DownloadAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSwipeToDelete()
        setupObservers()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.download_queue)
    }

    private fun setupRecyclerView() {
        queueAdapter = DownloadAdapter(
            onItemClick = { download ->
                showDownloadDetails(download)
            },
            onPauseClick = { download ->
                viewModel.pauseDownload(download.id)
            },
            onResumeClick = { download ->
                viewModel.resumeDownload(download.id)
            },
            onCancelClick = { download ->
                showCancelDialog(download)
            },
            onRetryClick = { download ->
                viewModel.retryDownload(download.id)
            },
            onDeleteClick = { download ->
                showDeleteDialog(download)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@QueueActivity)
            adapter = queueAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val download = queueAdapter.currentList[position]
                showDeleteDialog(download)
                queueAdapter.notifyItemChanged(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeDownloads.collectLatest { downloads ->
                    updateUI(downloads)
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnPauseAll.setOnClickListener {
            viewModel.pauseAllDownloads()
            Toasty.info(this, getString(R.string.all_downloads_paused), Toast.LENGTH_SHORT).show()
        }

        binding.btnResumeAll.setOnClickListener {
            viewModel.resumeAllDownloads()
            Toasty.info(this, getString(R.string.all_downloads_resumed), Toast.LENGTH_SHORT).show()
        }

        binding.btnCancelAll.setOnClickListener {
            showCancelAllDialog()
        }
    }

    private fun updateUI(downloads: List<DownloadItem>) {
        binding.progressBar.visibility = android.view.View.GONE

        if (downloads.isEmpty()) {
            binding.recyclerView.visibility = android.view.View.GONE
            binding.emptyView.visibility = android.view.View.VISIBLE
            binding.buttonsLayout.visibility = android.view.View.GONE
        } else {
            binding.recyclerView.visibility = android.view.View.VISIBLE
            binding.emptyView.visibility = android.view.View.GONE
            binding.buttonsLayout.visibility = android.view.View.VISIBLE
            queueAdapter.submitList(downloads)
        }

        val downloading = downloads.count { it.status == DownloadStatus.DOWNLOADING }
        val queued = downloads.count { it.status == DownloadStatus.QUEUED }
        val pending = downloads.count { it.status == DownloadStatus.PENDING }

        supportActionBar?.subtitle = getString(
            R.string.queue_status_format,
            downloading,
            queued,
            pending
        )
    }

    private fun showDownloadDetails(download: DownloadItem) {
        val message = buildString {
            appendLine("Title: ${download.title}")
            appendLine("Status: ${download.status}")
            appendLine("Progress: ${download.progress}%")
            download.speed?.let { appendLine("Speed: $it") }
            download.eta?.let { appendLine("ETA: $it") }
            if (download.retryCount > 0) {
                appendLine("Retry count: ${download.retryCount}")
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.download_details)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showCancelDialog(download: DownloadItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cancel_download)
            .setMessage(R.string.cancel_download_confirmation)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.cancelDownload(download.id)
                Toasty.info(this, getString(R.string.download_cancelled), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showDeleteDialog(download: DownloadItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.remove_from_queue)
            .setMessage(R.string.remove_queue_confirmation)
            .setPositiveButton(R.string.remove) { _, _ ->
                viewModel.deleteDownload(download)
                Toasty.success(this, getString(R.string.removed), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCancelAllDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cancel_all_downloads)
            .setMessage(R.string.cancel_all_confirmation)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.cancelAllActiveDownloads()
                Toasty.info(this, getString(R.string.all_downloads_cancelled), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_queue, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_retry_failed -> {
                viewModel.retryAllFailedDownloads()
                Toasty.success(this, getString(R.string.retrying_failed_downloads), Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_clear_completed -> {
                viewModel.clearCompletedDownloads()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
