package org.fmdx.app.telemetry

import android.content.Context
import android.os.Handler
import androidx.core.net.toUri
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class GpsWebViewHelper(
    private val context: Context,
    private val onLog: (String) -> Unit = {},
    private val onGps: (lat: String, lon: String, alt: String, mode: String) -> Unit = { _, _, _, _ -> }
) {
    companion object {
        private const val TAG = "GpsWebViewHelper"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingStopCallback: Runnable? = null

    @Volatile
    private var webView: WebView? = null

    fun start(serverAddress: String, watchMillis: Long = 60_000L) {
        if (webView != null) {
            logI("GPS watcher: already running, start ignored.")
            return
        }

        val baseUrl = normalizeUrl(serverAddress)
        val hostKey = normalizeHostKey(serverAddress)

        logI("GPS watcher: load $baseUrl (hostKey=$hostKey)")

        mainHandler.post {
            internalStop(destroy = true)

            webView = WebView(context.applicationContext).apply {
                visibility = View.GONE
                settings.domStorageEnabled = true

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        logI("GPS watcher: page loaded $url")
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        logW("GPS watcher: WebView error: $error")
                    }
                }
            }

            webView?.loadUrl(baseUrl)
            pendingStopCallback?.let { mainHandler.removeCallbacks(it) }
            pendingStopCallback = Runnable { internalStop(destroy = true) }
            mainHandler.postDelayed(pendingStopCallback!!, watchMillis + 2000L)
        }
    }

    fun stop() {
        pendingStopCallback?.let {
            mainHandler.removeCallbacks(it)
            pendingStopCallback = null
        }
        mainHandler.post { internalStop(destroy = true) }
    }

    private fun logI(msg: String) {
        Log.i("GpsWebViewHelper", msg)
        onLog(msg)
    }

    private fun logW(msg: String) {
        Log.w("GpsWebViewHelper", msg)
        onLog(msg)
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        val base = when {
            trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true) -> trimmed
            trimmed.startsWith("ws://", true) -> "http://" + trimmed.removePrefix("ws://")
            trimmed.startsWith("wss://", true) -> "https://" + trimmed.removePrefix("wss://")
            else -> "http://$trimmed"
        }
        return if (base.endsWith("/")) base else "$base/"
    }

    private fun normalizeHostKey(raw: String): String {
        val trimmed = raw.trim()
        val base = when {
            trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true) -> trimmed
            trimmed.startsWith("ws://", true) -> "http://" + trimmed.removePrefix("ws://")
            trimmed.startsWith("wss://", true) -> "https://" + trimmed.removePrefix("wss://")
            else -> "http://$trimmed"
        }
        return try {
            val uri = base.toUri()
            val host = uri.host ?: trimmed
            val port = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
            "$host:$port"
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun internalStop(destroy: Boolean) {
        // Drop any pending fallback timeout — if we're stopping early (manual stop, error,
        // service shutdown) we don't want a delayed Runnable to fire and re-destroy the
        // WebView later, potentially racing with another start() call.
        pendingStopCallback?.let {
            mainHandler.removeCallbacks(it)
            pendingStopCallback = null
        }
        if (destroy) {
            try {
                webView?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy WebView", e)
            }
            webView = null
        }
    }
}
