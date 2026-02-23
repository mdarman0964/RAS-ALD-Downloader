package com.arman.rasald.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.arman.rasald.R
import com.arman.rasald.data.entity.DownloadItem
import com.arman.rasald.data.entity.DownloadStatus
import com.arman.rasald.data.entity.DownloadType
import com.arman.rasald.databinding.ActivityDownloadBinding
import com.arman.rasald.ui.adapters.FormatAdapter
import com.arman.rasald.ui.viewmodels.DownloadViewModel
import com.arman.rasald.utils.*
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * Download Activity - Configure and start downloads
 */
class DownloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadBinding
    private val viewModel: DownloadViewModel by viewModel()
    private val ytdlpHelper: YtdlpHelper by inject()
    private val preferenceManager: PreferenceManager by inject()

    private var videoInfo: VideoInfo? = null
    private var playlistInfo: PlaylistInfo? = null
    private var selectedFormat: VideoFormat? = null
    private var selectedQuality: String = preferenceManager.getDefaultQuality()
    private var selectedVideoFormat: String = preferenceManager.getDefaultFormat()
    private var extractAudio: Boolean = preferenceManager.isExtractAudio()
    private var downloadSubtitle: Boolean = false
    private var selectedSubtitleLanguages: List<String> = listOf("en")

    private lateinit var formatAdapter: FormatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra("url") ?: run {
            Toasty.error(this, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupUI()
        setupListeners()
        extractVideoInfo(url)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.configure_download)
    }

    private fun setupUI() {
        // Quality spinner
        val qualities = resources.getStringArray(R.array.video_qualities)
        val qualityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, qualities)
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerQuality.adapter = qualityAdapter
        binding.spinnerQuality.setSelection(qualities.indexOf(selectedQuality).coerceAtLeast(0))

        // Format spinner
        val formats = resources.getStringArray(R.array.video_formats)
        val formatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, formats)
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFormat.adapter = formatAdapter
        binding.spinnerFormat.setSelection(formats.indexOf(selectedVideoFormat.uppercase()).coerceAtLeast(0))

        // Audio format spinner
        val audioFormats = resources.getStringArray(R.array.audio_formats)
        val audioFormatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, audioFormats)
        audioFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAudioFormat.adapter = audioFormatAdapter

        // Format RecyclerView
        formatAdapter = FormatAdapter { format ->
            selectedFormat = format
            updateSelectedFormatUI(format)
        }
        binding.rvFormats.apply {
            layoutManager = LinearLayoutManager(this@DownloadActivity)
            adapter = formatAdapter
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnDownload.setOnClickListener {
                startDownload()
            }

            btnPreview.setOnClickListener {
                previewVideo()
            }

            checkboxAudioOnly.setOnCheckedChangeListener { _, isChecked ->
                extractAudio = isChecked
                updateUIForAudioOnly(isChecked)
            }

            checkboxSubtitle.setOnCheckedChangeListener { _, isChecked ->
                downloadSubtitle = isChecked
                binding.spinnerSubtitleLanguage.isEnabled = isChecked
            }

            checkboxPlaylist.setOnCheckedChangeListener { _, isChecked ->
                updatePlaylistUI(isChecked)
            }
        }
    }

    private fun extractVideoInfo(url: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Check if it's a playlist
                if (url.contains("playlist") || url.contains("list=")) {
                    playlistInfo = ytdlpHelper.extractPlaylistInfo(url)
                    playlistInfo?.let { info ->
                        showPlaylistInfo(info)
                    }
                }

                // Extract video info
                videoInfo = ytdlpHelper.extractInfo(url)
                videoInfo?.let { info ->
                    showVideoInfo(info)
                    loadFormats(info)
                    loadSubtitles(info)
                } ?: run {
                    Toasty.error(this@DownloadActivity, getString(R.string.failed_to_extract_info), Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract info")
                Toasty.error(this@DownloadActivity, getString(R.string.error_occurred), Toast.LENGTH_LONG).show()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun showVideoInfo(info: VideoInfo) {
        binding.apply {
            tvTitle.text = info.title
            tvUploader.text = info.uploader ?: getString(R.string.unknown_uploader)
            tvDuration.text = formatDuration(info.duration)
            tvViews.text = formatViewCount(info.viewCount)

            // Load thumbnail
            info.thumbnail?.let { url ->
                Glide.with(this@DownloadActivity)
                    .load(url)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .error(R.drawable.ic_video_placeholder)
                    .into(ivThumbnail)
            }
        }
    }

    private fun showPlaylistInfo(info: PlaylistInfo) {
        binding.apply {
            checkboxPlaylist.visibility = View.VISIBLE
            checkboxPlaylist.text = getString(R.string.download_playlist_format, info.videoCount, info.title)
        }
    }

    private fun loadFormats(info: VideoInfo) {
        info.formats?.let { formats ->
            // Filter and sort formats
            val videoFormats = formats.filter { it.vcodec != "none" && it.acodec != "none" }
                .sortedByDescending { it.height }
            
            formatAdapter.submitList(videoFormats)
        }
    }

    private fun loadSubtitles(info: VideoInfo) {
        info.subtitles?.let { subs ->
            if (subs.isNotEmpty()) {
                binding.checkboxSubtitle.visibility = View.VISIBLE
                binding.spinnerSubtitleLanguage.visibility = View.VISIBLE

                val languages = subs.keys.toList()
                val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
                langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSubtitleLanguage.adapter = langAdapter
            }
        }
    }

    private fun updateSelectedFormatUI(format: VideoFormat) {
        binding.tvSelectedFormat.text = buildString {
            append("${format.quality} (${format.ext})")
            if (format.filesize > 0) {
                append(" - ${formatFileSize(format.filesize)}")
            }
        }
    }

    private fun updateUIForAudioOnly(isAudioOnly: Boolean) {
        binding.apply {
            spinnerQuality.isEnabled = !isAudioOnly
            rvFormats.isEnabled = !isAudioOnly
            spinnerAudioFormat.isEnabled = isAudioOnly
        }
    }

    private fun updatePlaylistUI(isPlaylist: Boolean) {
        // Update UI based on playlist selection
    }

    private fun startDownload() {
        val info = videoInfo ?: return
        val url = intent.getStringExtra("url") ?: return

        val isPlaylist = binding.checkboxPlaylist.isChecked && playlistInfo != null

        val downloadItem = DownloadItem(
            videoId = info.id,
            title = info.title,
            description = info.description,
            thumbnailUrl = info.thumbnail,
            duration = info.duration,
            uploader = info.uploader,
            url = url,
            downloadType = if (isPlaylist) DownloadType.PLAYLIST else if (extractAudio) DownloadType.AUDIO else DownloadType.VIDEO,
            quality = selectedQuality,
            format = if (extractAudio) binding.spinnerAudioFormat.selectedItem.toString() else selectedVideoFormat,
            audioFormat = if (extractAudio) binding.spinnerAudioFormat.selectedItem.toString() else null,
            status = DownloadStatus.PENDING,
            playlistId = if (isPlaylist) playlistInfo?.id else null,
            playlistTitle = if (isPlaylist) playlistInfo?.title else null
        )

        viewModel.addDownload(downloadItem)

        Toasty.success(this, getString(R.string.download_added_to_queue), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun previewVideo() {
        videoInfo?.webpageUrl?.let { url ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("video_url", url)
                putExtra("is_preview", true)
            }
            startActivity(intent)
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }

    private fun formatViewCount(count: Long): String {
        return when {
            count >= 1_000_000_000 -> String.format("%.1fB views", count / 1_000_000_000.0)
            count >= 1_000_000 -> String.format("%.1fM views", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK views", count / 1_000.0)
            else -> "$count views"
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
