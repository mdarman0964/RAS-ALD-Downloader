package com.arman.rasald.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.arman.rasald.R
import com.arman.rasald.data.entity.DownloadItem
import com.arman.rasald.data.entity.DownloadStatus
import com.arman.rasald.databinding.ActivityHistoryBinding
import com.arman.rasald.ui.adapters.DownloadAdapter
import com.arman.rasald.ui.viewmodels.DownloadViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Download History Activity
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: DownloadViewModel by viewModel()
    private lateinit var historyAdapter: DownloadAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.download_history)
    }

    private fun setupRecyclerView() {
        historyAdapter = DownloadAdapter(
            onItemClick = { download ->
                showDownloadDetails(download)
            },
            onPlayClick = { download ->
                if (download.status == DownloadStatus.COMPLETED) {
                    playVideo(download)
                }
            },
            onDeleteClick = { download ->
                showDeleteDialog(download)
            },
            onShareClick = { download ->
                shareDownload(download)
            },
            onRetryClick = { download ->
                viewModel.retryDownload(download.id)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completedDownloads.collectLatest { downloads ->
                    updateUI(downloads)
                }
            }
        }
    }

    private fun updateUI(downloads: List<DownloadItem>) {
        binding.progressBar.visibility = android.view.View.GONE

        if (downloads.isEmpty()) {
            binding.recyclerView.visibility = android.view.View.GONE
            binding.emptyView.visibility = android.view.View.VISIBLE
        } else {
            binding.recyclerView.visibility = android.view.View.VISIBLE
            binding.emptyView.visibility = android.view.View.GONE
            historyAdapter.submitList(downloads)
        }

        supportActionBar?.subtitle = getString(R.string.items_count, downloads.size)
    }

    private fun showDownloadDetails(download: DownloadItem) {
        val message = buildString {
            appendLine("Title: ${download.title}")
            appendLine("Uploader: ${download.uploader ?: "Unknown"}")
            appendLine("Quality: ${download.quality}")
            appendLine("Format: ${download.format}")
            if (download.fileSize > 0) {
                appendLine("Size: ${formatFileSize(download.fileSize)}")
            }
            appendLine("Downloaded: ${download.completedAt}")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.download_details)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .setNeutralButton(R.string.open) { _, _ ->
                openDownloadedFile(download)
            }
            .show()
    }

    private fun showDeleteDialog(download: DownloadItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_from_history)
            .setMessage(R.string.delete_history_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteDownload(download)
                Toasty.success(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun playVideo(download: DownloadItem) {
        download.filePath?.let { path ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("video_path", path)
                putExtra("video_title", download.title)
            }
            startActivity(intent)
        }
    }

    private fun openDownloadedFile(download: DownloadItem) {
        download.filePath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, getMimeType(file.extension))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.open_with)))
            }
        }
    }

    private fun shareDownload(download: DownloadItem) {
        download.filePath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = getMimeType(file.extension)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, download.title)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
            }
        }
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            "wav" -> "audio/wav"
            else -> "*/*"
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { searchDownloads(it) }
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_all -> {
                showClearAllDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearAllDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_all_history)
            .setMessage(R.string.clear_history_confirmation)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearCompletedDownloads()
                Toasty.success(this, getString(R.string.history_cleared), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun searchDownloads(query: String) {
        lifecycleScope.launch {
            viewModel.searchDownloads(query).collectLatest { downloads ->
                historyAdapter.submitList(downloads)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
