package com.arman.rasald.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.arman.rasald.R
import com.arman.rasald.data.entity.DownloadItem
import com.arman.rasald.data.entity.DownloadStatus
import com.arman.rasald.databinding.ActivityMainBinding
import com.arman.rasald.ui.adapters.DownloadAdapter
import com.arman.rasald.ui.viewmodels.DownloadViewModel
import com.arman.rasald.utils.PreferenceManager
import com.arman.rasald.utils.YtdlpHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * Main Activity - Entry point of RAS ALD Downloader
 * Developer: [ARMAN]
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: DownloadViewModel by viewModel()
    private val preferenceManager: PreferenceManager by inject()
    private val ytdlpHelper: YtdlpHelper by inject()

    private lateinit var downloadAdapter: DownloadAdapter

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true -> {
                // Permission granted
                checkManageStoragePermission()
            }
            else -> {
                // Permission denied
                showPermissionDeniedDialog()
            }
        }
    }

    // Storage manager launcher
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Toasty.success(this, getString(R.string.storage_permission_granted), Toast.LENGTH_SHORT).show()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupObservers()
        checkPermissions()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupRecyclerView() {
        downloadAdapter = DownloadAdapter(
            onItemClick = { download ->
                when (download.status) {
                    DownloadStatus.COMPLETED -> openDownloadedFile(download)
                    else -> showDownloadDetails(download)
                }
            },
            onPlayClick = { download ->
                if (download.status == DownloadStatus.COMPLETED) {
                    playVideo(download)
                }
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
            },
            onShareClick = { download ->
                shareDownload(download)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = downloadAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddDownloadDialog()
        }

        binding.fabBrowser.setOnClickListener {
            startActivity(Intent(this, BrowserActivity::class.java))
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allDownloads.collectLatest { downloads ->
                    updateUI(downloads)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeDownloads.collectLatest { activeDownloads ->
                    updateActiveDownloadsCount(activeDownloads.size)
                }
            }
        }

        viewModel.downloadEvent.observe(this) { event ->
            when (event) {
                is DownloadViewModel.DownloadEvent.ShowError -> {
                    Toasty.error(this, event.message, Toast.LENGTH_LONG).show()
                }
                is DownloadViewModel.DownloadEvent.ShowSuccess -> {
                    Toasty.success(this, event.message, Toast.LENGTH_SHORT).show()
                }
                is DownloadViewModel.DownloadEvent.DownloadStarted -> {
                    Toasty.info(this, getString(R.string.download_started), Toast.LENGTH_SHORT).show()
                }
                is DownloadViewModel.DownloadEvent.DownloadCompleted -> {
                    Toasty.success(this, getString(R.string.download_completed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI(downloads: List<DownloadItem>) {
        binding.progressBar.isVisible = false

        if (downloads.isEmpty()) {
            binding.recyclerView.isVisible = false
            binding.emptyView.isVisible = true
        } else {
            binding.recyclerView.isVisible = true
            binding.emptyView.isVisible = false
            downloadAdapter.submitList(downloads)
        }
    }

    private fun updateActiveDownloadsCount(count: Int) {
        if (count > 0) {
            binding.tvActiveDownloads.text = getString(R.string.active_downloads_count, count)
            binding.tvActiveDownloads.isVisible = true
        } else {
            binding.tvActiveDownloads.isVisible = false
        }
    }

    private fun checkPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                checkManageStoragePermission()
            }
            else -> {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // Permission already granted
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                        showPermissionRationale()
                    }
                    else -> {
                        requestPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        )
                    }
                }
            }
        }
    }

    private fun checkManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        }
    }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.storage_permission_rationale)
            .setPositiveButton(R.string.grant) { _, _ ->
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.storage_permission_denied_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddDownloadDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_download, null)
        val editTextUrl = dialogView.findViewById<TextInputEditText>(R.id.etUrl)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_download)
            .setView(dialogView)
            .setPositiveButton(R.string.download) { _, _ ->
                val url = editTextUrl.text?.toString()?.trim()
                if (!url.isNullOrEmpty()) {
                    if (isValidUrl(url)) {
                        startDownloadActivity(url)
                    } else {
                        Toasty.warning(this, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.paste) { _, _ ->
                // Paste from clipboard
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    editTextUrl.setText(clip.getItemAt(0).text)
                }
            }
            .show()
    }

    private fun startDownloadActivity(url: String) {
        val intent = Intent(this, DownloadActivity::class.java).apply {
            putExtra("url", url)
        }
        startActivity(intent)
    }

    private fun handleIntent(intent: Intent) {
        when {
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                sharedText?.let {
                    if (isValidUrl(it)) {
                        startDownloadActivity(it)
                    }
                }
            }
            intent.data != null -> {
                val url = intent.data.toString()
                if (isValidUrl(url)) {
                    startDownloadActivity(url)
                }
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        val youtubeRegex = Regex("^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+$")
        return youtubeRegex.matches(url)
    }

    private fun showDownloadDetails(download: DownloadItem) {
        val message = buildString {
            appendLine("Title: ${download.title}")
            appendLine("Status: ${download.status}")
            appendLine("Progress: ${download.progress}%")
            download.speed?.let { appendLine("Speed: $it") }
            download.eta?.let { appendLine("ETA: $it") }
            if (download.fileSize > 0) {
                appendLine("Size: ${formatFileSize(download.fileSize)}")
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
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showDeleteDialog(download: DownloadItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_download)
            .setMessage(R.string.delete_download_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteDownload(download)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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

    private fun playVideo(download: DownloadItem) {
        download.filePath?.let { path ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("video_path", path)
                putExtra("video_title", download.title)
            }
            startActivity(intent)
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
            "srt" -> "application/x-subrip"
            "vtt" -> "text/vtt"
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
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.action_queue -> {
                startActivity(Intent(this, QueueActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_clear_completed -> {
                viewModel.clearCompletedDownloads()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about)
            .setMessage(
                """
                ${getString(R.string.app_name)}
                ${getString(R.string.version)}: 2.0
                ${getString(R.string.developer)}: [ARMAN]
                
                ${getString(R.string.about_description)}
                """.trimIndent()
            )
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
