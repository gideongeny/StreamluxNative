package com.streamlux.app.ui.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.streamlux.app.MainActivity
import com.streamlux.app.ui.theme.PrimaryOrange
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.net.URI
import android.webkit.JavascriptInterface
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

private const val DESKTOP_CHROME_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

private val AD_DOMAINS = listOf(
    "bet", "pop", "fap", "traff", "doubleclick", "adservice", 
    "googlesyndication", "analytics", "quantserve", "scorecardresearch", "zedo", "adzerk", 
    "carbonads", "buysellads", "amazon-adsystem", "taboola", "outbrain", "adnxs", "smartadserver",
    "adexchangeclear", "onclick", "propeller"
)

private val WHITELIST_DOMAINS = listOf(
    "vidsrc", "vidplay", "googlevideo.com", "youtube.com", "2embed", "static", "tmdb", 
    "upcloud", "vidcloud", ".me", ".in", ".to", ".xyz", ".cc", ".cfd", ".ru", ".su", ".pm", ".net", "rabbitstream",
    "dlhd.so", "cdn-live.tv"
)

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

class BlobDownloadInterface(private val context: Context) {
    @JavascriptInterface
    fun downloadBlob(base64Data: String, fileName: String, mimeType: String) {
        try {
            val fileData = Base64.decode(base64Data.substringAfter("base64,"), Base64.DEFAULT)
            val file = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            FileOutputStream(file).use { it.write(fileData) }
            
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Blob Download Complete: $fileName", Toast.LENGTH_LONG).show()
            }
            
            // Trigger a scan so it shows up in file manager immediately
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        } catch (e: Exception) {
            Log.e("StreamLuxPlayer", "Blob download failed", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val currentServer by viewModel.currentServer.collectAsState()
    val context = LocalContext.current

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showReload by rememberSaveable { mutableStateOf(false) }
    var lastFailedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var debugCurrentUrl by remember { mutableStateOf("Ready...") }
    var lastJsMessage by remember { mutableStateOf("") }
    var lastBlockedUrl by remember { mutableStateOf("") }
    var isPiPMode by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        MainActivity.isPlayingVideo = true
        val activity = context.findActivity() as? ComponentActivity
        
        val pipReceiver = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isPiPMode = info.isInPictureInPictureMode
        }
        activity?.addOnPictureInPictureModeChangedListener(pipReceiver)
        
        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(pipReceiver)
            MainActivity.isPlayingVideo = false
        }
    }

    DisposableEffect(isFullscreen) {
        val activity = context as? ComponentActivity ?: return@DisposableEffect onDispose { }
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    var isLoaded by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(currentServer?.url) {
        currentServer?.url?.let { url ->
            if (url.isNotBlank()) {
                isLoaded = false
                val headers = HashMap<String, String>()
                headers["Referer"] = "https://streamlux-67a84.web.app/"
                headers["Origin"] = "https://streamlux-67a84.web.app"
                // STEALTH FINGERPRINT WIPE: Remove the Android App identity
                headers["X-Requested-With"] = "" 
                webViewRef?.loadUrl(url, headers)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val url = currentServer?.url ?: ""
        val isDirectStream = url.contains(".m3u8", ignoreCase = true) || 
                             url.contains(".mpd", ignoreCase = true) ||
                             url.contains(".m3u", ignoreCase = true)
                             
        val isNativeRequired = (viewModel.mediaType == "live" || viewModel.mediaType == "sports") && isDirectStream

        if (isNativeRequired) {
            NativeHlsPlayer(
                url = url,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                val activityContext = ctx.findActivity() ?: ctx
                WebView(activityContext).apply {
                    webViewRef = this
                    setBackgroundColor(android.graphics.Color.BLACK)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setGeolocationEnabled(true)
                        
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = DESKTOP_CHROME_UA
                        
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = true
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            safeBrowsingEnabled = false
                        }
                        
                        mediaPlaybackRequiresUserGesture = false
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    // REGISTER BLOB BRIDGE
                    addJavascriptInterface(BlobDownloadInterface(activityContext), "AndroidBlob")

                    // MASTER ORIGIN HANDSHAKE: Treat WebView like a browser
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                        try {
                            val request = DownloadManager.Request(Uri.parse(url))
                            request.setMimeType(mimetype)
                            
                            // PASS HEADERS: Crucial for portals that check origin/referer
                            request.addRequestHeader("User-Agent", userAgent)
                            request.addRequestHeader("Referer", "https://streamlux-67a84.web.app/")
                            
                            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
                            request.setDescription("Downloading $fileName")
                            request.setTitle(fileName)
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                            
                            val dm = activityContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                            
                            Toast.makeText(activityContext, "Download started: $fileName", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Log.e("StreamLuxPlayer", "Download failed", e)
                            Toast.makeText(activityContext, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }

                    // AUDIT: Hardware Acceleration & Layers
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            Log.d("StreamLuxPlayer", "CONSOLE: ${consoleMessage?.message()} (Line: ${consoleMessage?.lineNumber()})")
                            return true
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message?
                        ): Boolean {
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            val context = view?.context ?: return false
                            
                            // HELPER: Create a transient WebView to capture the new URL and redirect it to the MAIN view
                            val newWebView = WebView(context)
                            newWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: ""
                                    Log.d("StreamLuxPlayer", "REDIRECT: Handling new tab request -> $url")
                                    webViewRef?.loadUrl(url)
                                    return true
                                }
                            }
                            
                            transport?.webView = newWebView
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url?.toString() ?: return null
                            
                            // CLOAKING: Remove X-Requested-With for mirrors to prevent app-blocking
                            val isPortal = url.contains("vidvault", true) || 
                                           url.contains("videasy", true) || 
                                           url.contains("dl.", true) ||
                                           url.contains("storage", true)

                            if (isPortal && request.method == "GET") {
                                try {
                                    val client = OkHttpClient.Builder()
                                        .followRedirects(true)
                                        .build()

                                    val reqBuilder = Request.Builder().url(url)
                                    request.requestHeaders.forEach { (k, v) ->
                                        if (!k.equals("X-Requested-With", true)) {
                                            reqBuilder.addHeader(k, v)
                                        }
                                    }
                                    
                                    val response = client.newCall(reqBuilder.build()).execute()
                                    val body = response.body
                                    if (body != null) {
                                        val contentType = response.header("Content-Type", "text/html") ?: "text/html"
                                        return WebResourceResponse(
                                            contentType.split(";")[0],
                                            response.header("Content-Encoding", "UTF-8"),
                                            body.byteStream()
                                        )
                                    }
                                } catch (e: Exception) { }
                            }
                            return null
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            // FULL FREEDOM: No longer blocking domains for non-live/sports content
                            // This allows portals to redirect freely across different domains
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                             super.onPageStarted(view, url, favicon)
                             if (url?.contains("youtube") == true || viewModel.mediaType == "live") {
                                 isLoaded = true
                             }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            
                            // 1. UNIVERSAL DOWNLOAD BRIDGE (Intercepts Blobs, Data, and Clicks)
                            val downloadBridge = """
                                (function() {
                                    if (window.lxBridgeActive) return;
                                    window.lxBridgeActive = true;
                                    document.addEventListener('click', function(e) {
                                        var target = e.target.closest('a');
                                        if (!target) return;
                                        var href = target.href;
                                        if (href && (href.startsWith('blob:') || href.startsWith('data:'))) {
                                            e.preventDefault();
                                            var fileName = target.getAttribute('download') || 'downloaded_file';
                                            var xhr = new XMLHttpRequest();
                                            xhr.open('GET', href, true);
                                            xhr.responseType = 'blob';
                                            xhr.onload = function() {
                                                if (this.status == 200) {
                                                    var b = this.response;
                                                    var r = new FileReader();
                                                    r.onloadend = function() { window.AndroidBlob.downloadBlob(r.result, fileName, b.type); };
                                                    r.readAsDataURL(b);
                                                }
                                            };
                                            xhr.send();
                                        }
                                    }, true);
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(downloadBridge, null)

                            // 2. CONDITIONAL CLOAKING: Only hide UI on our own web app, leave portals fully interactive
                            val isOurApp = url?.contains("streamlux-67a84.web.app") == true
                            if (isOurApp) {
                                val cloakScript = """
                                    (function() {
                                        var style = document.createElement('style');
                                        style.innerHTML = `
                                            .fixed.top-0, .fixed.bottom-0, .z-\\[110\\], .z-\\[10002\\], 
                                            button[class*='z-[10002]'] { display: none !important; }
                                            body, html { overflow: hidden !important; background-color: black !important; }
                                        `;
                                        document.head.appendChild(style);
                                    })()
                                """.trimIndent()
                                view?.evaluateJavascript(cloakScript, null)
                            }
                            
                            isLoaded = true
                        }
                    }
                }
            },
            update = { }
        )
    }

        // Only show loading for standard Movies/TV. 
        // Live channels bypass this to prevent showing the loading loop.
        val isLiveBypass = viewModel.mediaType == "live" || viewModel.mediaType == "sports"

        if (!isLoaded && !isLiveBypass && !isPiPMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(color = PrimaryOrange)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(12.dp))
                    Text(
                        text = "Loading...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        if (!isPiPMode) {
            AndroidView(
                factory = { ctx ->
                    try {
                        MediaRouteButton(ctx).apply {
                            CastButtonFactory.setUpMediaRouteButton(ctx.applicationContext, this)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Space(ctx)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )

            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 40.dp, start = 12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}
