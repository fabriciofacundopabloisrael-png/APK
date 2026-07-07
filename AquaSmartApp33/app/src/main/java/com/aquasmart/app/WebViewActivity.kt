package com.aquasmart.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.aquasmart.app.databinding.ActivityWebviewBinding

/**
 * Envoltorio simple para la página web que YA existe en el ESP32.
 * No recrea ninguna pantalla: solo carga la URL del portal dentro de
 * un WebView integrado en la app, para no tener que salir a Chrome.
 */
class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebviewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = getString(R.string.webview_titulo)
        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)
        binding.toolbar.setNavigationOnClickListener { finish() }

        configurarWebView(url)
        binding.btnReintentar.setOnClickListener { binding.webView.reload() }
    }

    private fun configurarWebView(url: String) {
        with(binding.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                binding.contenedorError.visibility = View.GONE
                binding.barraProgreso.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.barraProgreso.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    binding.contenedorError.visibility = View.VISIBLE
                }
            }
        }

        binding.webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.barraProgreso.progress = newProgress
            }
        }

        binding.webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}
