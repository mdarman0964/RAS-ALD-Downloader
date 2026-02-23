package com.arman.rasald.ui.activities

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arman.rasald.R
import com.arman.rasald.databinding.ActivityPlayerBinding
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import es.dmoral.toasty.Toasty
import timber.log.Timber

/**
 * Video Player Activity using ExoPlayer
 * Supports local files and online streaming
 */
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var playbackPosition = 0L
    private var currentWindow = 0
    private var playWhenReady = true

    private var videoUrl: String? = null
    private var videoPath: String? = null
    private var videoTitle: String? = null
    private var isPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get intent extras
        videoUrl = intent.getStringExtra("video_url")
        videoPath = intent.getStringExtra("video_path")
        videoTitle = intent.getStringExtra("video_title")
        isPreview = intent.getBooleanExtra("is_preview", false)

        setupFullscreen()
        setupPlayerView()
    }

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Util.SDK_INT >= Util.SDK_INT) {
            val controller = WindowInsetsControllerCompat(window, binding.root)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupPlayerView() {
        binding.playerView.apply {
            setShowBuffering(com.google.android.exoplayer2.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setShowShuffleButton(false)
            setShowMultiWindowTimeBar(true)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            
            // Custom controller setup
            setControllerVisibilityListener { visibility ->
                // Handle controller visibility
            }
        }

        // Back button
        binding.playerView.findViewById<ImageButton>(R.id.exo_back)?.setOnClickListener {
            finish()
        }

        // Fullscreen toggle
        binding.playerView.findViewById<ImageButton>(R.id.exo_fullscreen)?.setOnClickListener {
            toggleFullscreen()
        }
    }

    private fun initializePlayer() {
        if (player != null) return

        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
            .also { exoPlayer ->
                binding.playerView.player = exoPlayer

                // Build media source
                val mediaItem = when {
                    videoPath != null -> MediaItem.fromUri(Uri.parse(videoPath))
                    videoUrl != null -> MediaItem.fromUri(Uri.parse(videoUrl))
                    else -> {
                        Toasty.error(this, getString(R.string.invalid_video_source), Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }
                }

                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentWindow, playbackPosition)
                exoPlayer.prepare()

                // Player listener
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                binding.progressBar.visibility = View.GONE
                            }
                            Player.STATE_BUFFERING -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            Player.STATE_ENDED -> {
                                // Video ended
                            }
                            Player.STATE_IDLE -> {
                                // Idle state
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        // Handle play/pause
                    }

                    override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                        Timber.e(error, "Player error")
                        Toasty.error(this@PlayerActivity, getString(R.string.playback_error), Toast.LENGTH_LONG).show()
                    }
                })
            }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentWindow = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(object : Player.Listener {})
            exoPlayer.release()
        }
        player = null
    }

    private fun toggleFullscreen() {
        val currentOrientation = requestedOrientation
        requestedOrientation = if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.playbackParameters = PlaybackParameters(speed)
    }

    fun getPlaybackSpeed(): Float {
        return player?.playbackParameters?.speed ?: 1f
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24 || player == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onBackPressed() {
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            super.onBackPressed()
        }
    }
}
