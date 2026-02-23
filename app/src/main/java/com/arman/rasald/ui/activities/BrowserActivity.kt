package com.arman.rasald.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arman.rasald.R
import com.arman.rasald.databinding.ActivityBrowserBinding
import es.dmoral.toasty.Toasty

/**
 * Built-in YouTube Browser Activity
 */
class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding

    companion object {
        private const val YOUTUBE_BASE_URL = "https://m.youtube.com"
        private val ALLOWED_DOMAINS = listOf(
            "youtube.com",
            "m.youtube.com",
            "www.youtube.com",
            "youtu.be"
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupWebView()
        loadUrl(YOUTUBE_BASE_URL)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.youtube_browser)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36"
                allowFileAccess = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    updateNavigationButtons()
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    
                    // Check if it's a YouTube video URL
                    if (isVideoUrl(url)) {
                        showDownloadDialog(url)
                        return true
                    }
                    
                    return false
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Toasty.error(this@BrowserActivity, getString(R.string.loading_error), Toast.LENGTH_SHORT).show()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                }
            }

            setDownloadListener { url, _, _, _, _ ->
                showDownloadDialog(url)
            }
        }

        // Navigation buttons
        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            }
        }

        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            }
        }

        binding.btnRefresh.setOnClickListener {
            binding.webView.reload()
        }

        binding.btnHome.setOnClickListener {
            loadUrl(YOUTUBE_BASE_URL)
        }

        binding.btnDownload.setOnClickListener {
            val currentUrl = binding.webView.url
            if (currentUrl != null && isVideoUrl(currentUrl)) {
                showDownloadDialog(currentUrl)
            } else {
                Toasty.warning(this, getString(R.string.not_a_video_page), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUrl(url: String) {
        binding.webView.loadUrl(url)
    }

    private fun isVideoUrl(url: String): Boolean {
        return url.contains("/watch") || url.contains("youtu.be/")
    }

    private fun showDownloadDialog(url: String) {
        val intent = Intent(this, DownloadActivity::class.java).apply {
            putExtra("url", url)
        }
        startActivity(intent)
    }

    private fun updateNavigationButtons() {
        binding.btnBack.isEnabled = binding.webView.canGoBack()
        binding.btnForward.isEnabled = binding.webView.canGoForward()
        binding.btnBack.alpha = if (binding.btnBack.isEnabled) 1.0f else 0.5f
        binding.btnForward.alpha = if (binding.btnForward.isEnabled) 1.0f else 0.5f
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
