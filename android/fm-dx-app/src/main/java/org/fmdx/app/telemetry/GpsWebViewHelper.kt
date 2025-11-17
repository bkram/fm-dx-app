package org.fmdx.app.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Locale

class GpsWebViewHelper(
    private val context: Context,
    private val onLog: (String) -> Unit = {},
    private val onGps: (lat: String, lon: String, alt: String, mode: String) -> Unit = { _, _, _, _ -> },
    private val prefsName: String = "gps_cache"
) {

    private val mainHandler = Handler(Looper.getMainLooper())

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
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                val bridge = GpsJsBridge(
                    hostKey = hostKey,
                    saveGlobal = { la, lo, al, md -> saveGpsToPrefs(la, lo, al, md) },
                    saveForHost = { la, lo, al, md ->
                        saveGpsToPrefsForHost(
                            hostKey,
                            la,
                            lo,
                            al,
                            md
                        )
                    },
                    onLog = { onLog(it) },
                    onGpsCallback = { la, lo, al, md ->
                        GpsStore.update(GpsData(la, lo, al, md))
                        onGps(la, lo, al, md)
                    },
                    stopWebView = { internalStop(true) }
                )

                addJavascriptInterface(bridge, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        evaluateJavascript(buildInjectedJs(watchMillis), null)
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
            mainHandler.postDelayed({ internalStop(destroy = true) }, watchMillis + 2000L)
        }
    }

    fun stop() {
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
            val uri = Uri.parse(base)
            val host = uri.host ?: trimmed
            val port = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
            "$host:$port"
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun prefs(): SharedPreferences =
        context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private inline fun SharedPreferences.edit(
        commit: Boolean = false,
        block: SharedPreferences.Editor.() -> Unit
    ) {
        val editor = edit()
        editor.block()
        if (commit) editor.commit() else editor.apply()
    }

    private fun saveGpsToPrefs(lat: String, lon: String, alt: String, mode: String) {
        prefs().edit {
            putString("qth_lat", lat)
            putString("qth_lon", lon)
            putString("qth_alt", alt)
            putString("qth_mode", mode)
        }
    }

    private fun saveGpsToPrefsForHost(
        hostKey: String,
        lat: String,
        lon: String,
        alt: String,
        mode: String
    ) {
        prefs().edit {
            putString("qth_lat_$hostKey", lat)
            putString("qth_lon_$hostKey", lon)
            putString("qth_alt_$hostKey", alt)
            putString("qth_mode_$hostKey", mode)
        }
    }

    private fun internalStop(destroy: Boolean) {
        try {
            webView?.evaluateJavascript(
                "(function(){try{window.__gpsStop&&window.__gpsStop();}catch(e){}})();",
                null
            )
        } catch (_: Exception) {
        }
        if (destroy) {
            try {
                webView?.destroy()
            } catch (_: Exception) {
            }
            webView = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildInjectedJs(watchMillis: Long): String = """
(function(){
  try{
    var qthLat = localStorage.getItem('qthLatitude') || "";
    var qthLon = localStorage.getItem('qthLongitude') || "";
    var qthAlt = localStorage.getItem('qthAltitude') || "";
    var qthMode = localStorage.getItem('qthMode') || "";
    if(qthLat || qthLon){
      Android.onStaticQth(qthLat, qthLon, qthAlt, qthMode);
    }
  }catch(e){}

  try{
    if(window.__gpsStop){window.__gpsStop();}
  }catch(e){}

  window.__gpsStop = function(){
    try{
      window.__gpsSocket && window.__gpsSocket.close();
    }catch(e){}
  };

  try{
    var proto = location.protocol === "https:" ? "wss://" : "ws://";
    var wsUrl = proto + location.host + "/data_plugins";
    var socket = new WebSocket(wsUrl);
    window.__gpsSocket = socket;

    socket.addEventListener("message", function(evt){
      try{
        var data = JSON.parse(evt.data);
        if(data.type === "GPS" && data.value && data.value.status === "active"){
          var v = data.value;
          Android.onGps(v.lat || "", v.lon || "", v.alt || "", v.mode || "");
        }
      }catch(ex){}
    });

    setTimeout(function(){
      try{ socket.close(); }catch(e){}
    }, ${watchMillis} );
  }catch(e){}
})();
""".trimIndent()

    private inner class GpsJsBridge(
        private val hostKey: String,
        private val saveGlobal: (String, String, String, String) -> Unit,
        private val saveForHost: (String, String, String, String) -> Unit,
        private val onLog: (String) -> Unit,
        private val onGpsCallback: (String, String, String, String) -> Unit,
        private val stopWebView: () -> Unit
    ) {

        @JavascriptInterface
        fun onStaticQth(lat: String, lon: String, alt: String, mode: String) {
            val sanitizedLat = lat.trim()
            val sanitizedLon = lon.trim()
            if (sanitizedLat.isNotEmpty() || sanitizedLon.isNotEmpty()) {
                val roundedLat = round6(sanitizedLat)
                val roundedLon = round6(sanitizedLon)
                val roundedAlt = round1(alt)
                saveGlobal(roundedLat, roundedLon, roundedAlt, mode.trim())
                saveForHost(roundedLat, roundedLon, roundedAlt, mode.trim())
                onGpsCallback(roundedLat, roundedLon, roundedAlt, mode.trim())
                onLog("Static QTH: lat=$roundedLat lon=$roundedLon alt=$roundedAlt mode=$mode")
            }
        }

        @JavascriptInterface
        fun onGps(lat: String, lon: String, alt: String, mode: String) {
            val roundedLat = round6(lat)
            val roundedLon = round6(lon)
            val roundedAlt = round1(alt)
            saveGlobal(roundedLat, roundedLon, roundedAlt, mode.trim())
            saveForHost(roundedLat, roundedLon, roundedAlt, mode.trim())
            onGpsCallback(roundedLat, roundedLon, roundedAlt, mode.trim())
            onLog("Live GPS: lat=$roundedLat lon=$roundedLon alt=$roundedAlt mode=$mode")
            stopWebView()
        }

        private fun round6(value: String): String =
            value.toDoubleOrNull()?.let { String.format(Locale.US, "%.6f", it) } ?: value

        private fun round1(value: String): String =
            value.toDoubleOrNull()?.let { String.format(Locale.US, "%.1f", it) } ?: value
    }
}
