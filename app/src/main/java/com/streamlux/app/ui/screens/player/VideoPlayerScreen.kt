package com.streamlux.app.ui.screens.player

import android.annotation.SuppressLint
import android.app.DownloadManager
import com.streamlux.app.ads.AdManager
import com.streamlux.app.MainActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.KeyEvent
import android.view.MotionEvent
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.streamlux.app.ui.theme.PrimaryOrange
import com.streamlux.app.utils.BlobDownloadInterface
import com.streamlux.app.utils.BrowserConstants
import com.streamlux.app.utils.findActivity
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentServer by viewModel.currentServer.collectAsState()
    val isOfflineFile by viewModel.isOfflineFile.collectAsState()
    val localUri by viewModel.localUri.collectAsState()
    val systemDownloadId by viewModel.systemDownloadId.collectAsState()
    val serverList by viewModel.serverList.collectAsState()
    var isFullscreen by rememberSaveable { mutableStateOf(true) }
    var isLoaded by remember { mutableStateOf(false) }
    var isPiPMode by remember { mutableStateOf(false) }
    
    val isAndroidTV = remember { context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) }

    // ─── OFFLINE: ExoPlayer ─────────────────────────────────────────────
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    // Track whether we have storage permission so we can re-trigger ExoPlayer
    var storagePermissionGranted by remember { mutableStateOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        else
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    ) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        if (granted) storagePermissionGranted = true
    }

    // Request storage permission as soon as we know it's an offline file
    LaunchedEffect(isOfflineFile) {
        if (!isOfflineFile) return@LaunchedEffect
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            android.Manifest.permission.READ_MEDIA_VIDEO
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(perm))
        } else {
            storagePermissionGranted = true
        }
    }

    // Initialize ExoPlayer once we have both the URI and storage permission
    LaunchedEffect(localUri, isOfflineFile, storagePermissionGranted) {
        if (!isOfflineFile || localUri == null || !storagePermissionGranted) return@LaunchedEffect

        // Resolve the best possible URI
        var playUri: Uri = Uri.parse(localUri)

        // 1st choice: DownloadManager content:// URI (bypasses Scoped Storage)
        if (systemDownloadId != null) {
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val dmUri = dm.getUriForDownloadedFile(systemDownloadId!!)
                if (dmUri != null) {
                    playUri = dmUri
                    Log.d("StreamLuxPlayer", "Playing via DownloadManager URI: $dmUri")
                }
            } catch (e: Exception) {
                Log.w("StreamLuxPlayer", "DownloadManager URI failed, falling back", e)
            }
        }

        // 2nd choice: MediaStore lookup by file path
        if (playUri.scheme == "file") {
            val path = playUri.path
            if (path != null) {
                try {
                    val projection = arrayOf(android.provider.MediaStore.Video.Media._ID)
                    val selection = "${android.provider.MediaStore.Video.Media.DATA} = ?"
                    val cursor = context.contentResolver.query(
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection, selection, arrayOf(path), null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val id = it.getLong(it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID))
                            playUri = Uri.withAppendedPath(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                            Log.d("StreamLuxPlayer", "Playing via MediaStore URI: $playUri")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("StreamLuxPlayer", "MediaStore lookup failed", e)
                }
            }
        }

        // Build & start ExoPlayer
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        exoPlayer!!.apply {
            setMediaItem(MediaItem.fromUri(playUri))
            prepare()
            playWhenReady = true
        }
        Log.d("StreamLuxPlayer", "ExoPlayer prepared with URI: $playUri")
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer?.release() }
    }

    // ─── ONLINE: WebView ─────────────────────────────────────────────────
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    androidx.compose.runtime.LaunchedEffect(currentServer?.url, webViewRef) {
        val url = currentServer?.url
        if (url != null && url.isNotBlank()) {
            isLoaded = false
            if (url.endsWith(".m3u8") || viewModel.mediaType == "live" || viewModel.mediaType == "sports") {
                // Do nothing here, it will be handled by NativeHlsPlayer below
            } else if (webViewRef != null) {
                val headers = HashMap<String, String>()
                // Spoof the origin so servers see a legitimate web browser referer
                headers["Referer"] = "https://streamlux-67a84.web.app/"
                headers["Origin"] = "https://streamlux-67a84.web.app"
                // STEALTH FINGERPRINT WIPE: Suppress Android WebView fingerprint header
                headers["X-Requested-With"] = ""
                webViewRef?.loadUrl(url, headers)
                Log.d("StreamLuxPlayer", "WebView loading (with headers): $url")
            }
        }
    }

    // ─── SYSTEM UI & NAVIGATION ──────────────────────────────────────────
    DisposableEffect(context) {
        AdManager.isVideoPlaying = true
        val activity = context.findActivity() as? ComponentActivity
        val pipReceiver = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isPiPMode = info.isInPictureInPictureMode
        }
        activity?.addOnPictureInPictureModeChangedListener(pipReceiver)
        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(pipReceiver)
            MainActivity.isAuthInProgress = false
            AdManager.isVideoPlaying = false
        }
    }

    DisposableEffect(isFullscreen) {
        val activity = context as? ComponentActivity ?: return@DisposableEffect onDispose {}
        val window = activity.window
        val ctrl = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
            ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ctrl.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ctrl.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler {
        onNavigateBack()
    }

    var showControls by remember { mutableStateOf(true) }
    var controlsTimeoutTrigger by remember { mutableIntStateOf(0) }

    // Auto-hide controls after 5 seconds
    LaunchedEffect(showControls, controlsTimeoutTrigger) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    // ─── RENDER ──────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                        if (showControls) controlsTimeoutTrigger++
                    }
                )
            }
    ) {

        val isLiveBypass = viewModel.mediaType == "live" || viewModel.mediaType == "sports"
        val targetUrl = currentServer?.url ?: ""

        if (isOfflineFile) {
            // ── OFFLINE: Native ExoPlayer ──
            if (localUri != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = true
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { view ->
                        // Bind ExoPlayer whenever it becomes available
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                        if (exoPlayer != null) isLoaded = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // File missing state — offer fallback to online
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Warning, null, tint = PrimaryOrange, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Offline File Not Found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("The downloaded file is missing. Try watching online.", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { viewModel.generateSources() }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)) {
                            Text("Watch Online")
                        }
                    }
                }
                isLoaded = true
            }
        } else {
            val streamUrl = currentServer?.url.orEmpty()
            val youtubeVideoId = when {
                viewModel.mediaType == "youtube" -> viewModel.playbackId
                else -> com.streamlux.app.utils.LiveStreamUrl.youtubeVideoId(streamUrl)
            }
            when {
                !youtubeVideoId.isNullOrBlank() -> {
                    isLoaded = true
                    YouTubeLivePlayer(
                        videoId = youtubeVideoId,
                        channelName = viewModel.mediaTitle,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                streamUrl.contains(".m3u8", ignoreCase = true) -> {
                    isLoaded = true
                    NativeHlsPlayer(
                        url = streamUrl,
                        simpleCache = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val activityCtx = ctx.findActivity() ?: ctx
                    WebView(activityCtx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        // Removed setLayerType hardware enforcement to allow native WebView HTML5 video acceleration to handle surfaces properly
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            @Suppress("DEPRECATION")
                            allowFileAccessFromFileURLs = true
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            // Must look like a real desktop browser
                            userAgentString = BrowserConstants.DESKTOP_CHROME_UA
                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = false
                            mediaPlaybackRequiresUserGesture = false
                            builtInZoomControls = false
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        // Enable first + third-party cookies (required by embed players)
                        val cm = android.webkit.CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        cm.setAcceptThirdPartyCookies(this, true)
                        // Make WebView focusable for Android TV D-pad navigation
                        isFocusable = true
                        isFocusableInTouchMode = true
                        requestFocus()

                        // Handle D-pad center to simulate a tap in the center of the WebView
                        setOnKeyListener { v, keyCode, event ->
                            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                                if (event.action == KeyEvent.ACTION_DOWN) {
                                    val x = v.width / 2f
                                    val y = v.height / 2f
                                    val downTime = SystemClock.uptimeMillis()
                                    val eventTime = SystemClock.uptimeMillis()
                                    val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
                                    v.dispatchTouchEvent(downEvent)
                                    downEvent.recycle()

                                    val upEvent = MotionEvent.obtain(downTime, eventTime + 100, MotionEvent.ACTION_UP, x, y, 0)
                                    v.dispatchTouchEvent(upEvent)
                                    upEvent.recycle()
                                }
                                return@setOnKeyListener true
                            }
                            false
                        }

                        // Capture the WebView reference so LaunchedEffect can call loadUrl with headers
                        webViewRef = this
                        addJavascriptInterface(BlobDownloadInterface(activityCtx), "AndroidBlob")
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                // Provide complete freedom for video sources to redirect/load ads to avoid black screens
                                return false
                            }
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (url == "about:blank") return
                                // Inject BEFORE page JS runs — hides all WebView fingerprints
                                val antiDetect = """
                                    (function() {
                                        try {
                                            Object.defineProperty(navigator, 'webdriver', {get: () => false});
                                            Object.defineProperty(navigator, 'plugins', {get: () => [{name:'Chrome PDF Plugin'},{name:'Chrome PDF Viewer'},{name:'Native Client'}]});
                                            Object.defineProperty(navigator, 'languages', {get: () => ['en-US','en']});
                                            if (!window.chrome) window.chrome = {runtime: {}, loadTimes: function(){}, csi: function(){}, app: {}};
                                        } catch(e) {}
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(antiDetect, null)
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url == "about:blank") return
                                // Inject CSS to hide Videasy website chrome (nav, header, footer)
                                // so only the video player iframe is visible. Only apply to Videasy.
                                if (url?.contains("videasy.net", ignoreCase = true) == true) {
                                    val cloakScript = """
                                        (function() {
                                            var style = document.createElement('style');
                                            style.innerHTML = `
                                                .fixed.top-0, .fixed.bottom-0,
                                                .z-\\[110\\], .z-\\[10002\\],
                                                button[class*='z-[10002]'],
                                                header, nav, footer { display: none !important; }
                                                body, html { overflow: hidden !important; background-color: black !important; }
                                            `;
                                            document.head.appendChild(style);
                                        })()
                                    """.trimIndent()
                                    view?.evaluateJavascript(cloakScript, null)
                                }
                                val bridge = "(function(){ if(window.lxBridgeActive) return; window.lxBridgeActive=true; document.addEventListener('click', function(e){ var t=e.target.closest('a'); if(!t) return; var h=t.href; if(h&&(h.startsWith('blob:')||h.startsWith('data:'))){ e.preventDefault(); var xhr=new XMLHttpRequest(); xhr.open('GET', h, true); xhr.responseType='blob'; xhr.onload=function(){ if(this.status==200){ var b=this.response; var r=new FileReader(); r.onloadend=function(){ window.AndroidBlob.downloadBlob(r.result, t.getAttribute('download')||'file', b.type); }; r.readAsDataURL(b); } }; xhr.send(); } }, true); })();"
                                view?.evaluateJavascript(bridge, null)
                                // AUTO-PLAY: Automatically click play buttons and start videos
                                // This is critical for Android TV where D-pad can't reach inside iframes
                                val autoPlayScript = """
                                    (function() {
                                        if (window._slAutoPlayActive) return;
                                        window._slAutoPlayActive = true;
                                        var attempts = 0;
                                        var maxAttempts = 15;
                                        function tryAutoPlay() {
                                            attempts++;
                                            if (attempts > maxAttempts) return;
                                            // Common play button selectors across embed providers
                                            var selectors = [
                                                '.play-button', '.jw-icon-display', '.vjs-big-play-button',
                                                '.plyr__control--overlaid', '[aria-label="Play"]',
                                                'button[data-plyr="play"]', '.play-btn', '.btn-play',
                                                '.icon-play', '.player-play', '.ytp-large-play-button',
                                                'svg.play', '.mejs__overlay-play', '.art-icon-playback',
                                                '[class*="play" i]', '[id*="play" i]'
                                            ];
                                            var clicked = false;
                                            // Try main document
                                            for (var i = 0; i < selectors.length; i++) {
                                                var el = document.querySelector(selectors[i]);
                                                if (el && el.offsetParent !== null) {
                                                    el.click();
                                                    clicked = true;
                                                    break;
                                                }
                                            }
                                            // Try all videos in main doc
                                            var videos = document.querySelectorAll('video');
                                            for (var v = 0; v < videos.length; v++) {
                                                try { videos[v].play(); clicked = true; } catch(e) {}
                                            }
                                            // Try inside iframes (same-origin only)
                                            var iframes = document.querySelectorAll('iframe');
                                            for (var f = 0; f < iframes.length; f++) {
                                                try {
                                                    var idoc = iframes[f].contentDocument || iframes[f].contentWindow.document;
                                                    if (!idoc) continue;
                                                    for (var j = 0; j < selectors.length; j++) {
                                                        var iel = idoc.querySelector(selectors[j]);
                                                        if (iel && iel.offsetParent !== null) {
                                                            iel.click();
                                                            clicked = true;
                                                            break;
                                                        }
                                                    }
                                                    var ivids = idoc.querySelectorAll('video');
                                                    for (var iv = 0; iv < ivids.length; iv++) {
                                                        try { ivids[iv].play(); clicked = true; } catch(e) {}
                                                    }
                                                } catch(e) {}
                                            }
                                            // Keep trying if nothing was clicked yet
                                            if (!clicked && attempts < maxAttempts) {
                                                setTimeout(tryAutoPlay, 2000);
                                            }
                                        }
                                        // Start after a short delay to let the player load
                                        setTimeout(tryAutoPlay, 1500);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(autoPlayScript, null)
                                isLoaded = true
                            }
                        }
                        // WebChromeClient is REQUIRED for HTML5 video to play:
                        // without it, onPermissionRequest is never called and video is silently blocked
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                                // Grant all permissions the embed player requests (video/audio)
                                request?.grant(request.resources)
                            }
                            override fun onShowCustomView(
                                view: android.view.View?,
                                callback: android.webkit.WebChromeClient.CustomViewCallback?
                            ) {
                                // Allow fullscreen video overlays
                                super.onShowCustomView(view, callback)
                            }
                        }
                    }
                },

                // URL is loaded via LaunchedEffect above with proper headers — update block intentionally empty
                update = { }

            )
                }
            }
        }

        // Loading spinner (hidden once content fires onPageFinished or ExoPlayer is ready)
        if (!isLoaded && !isPiPMode && !isLiveBypass) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryOrange)
            }
        }

        // Overlay controls
        if (!isPiPMode) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Cast button (safe fallback if AppCompat context is missing)
                    AndroidView(
                        factory = { ctx ->
                            try {
                                MediaRouteButton(ctx).apply {
                                    CastButtonFactory.setUpMediaRouteButton(ctx.applicationContext, this)
                                }
                            } catch (e: Throwable) {
                                android.view.View(ctx)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    )

                    // Back button
                    IconButton(
                        onClick = { onNavigateBack() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 40.dp, start = 12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    if (isAndroidTV && webViewRef != null) {
                        Button(
                            onClick = {
                                webViewRef?.let { wv ->
                                    val x = wv.width / 2f
                                    val y = wv.height / 2f
                                    val downTime = SystemClock.uptimeMillis()
                                    val eventTime = SystemClock.uptimeMillis()
                                    val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
                                    wv.dispatchTouchEvent(downEvent)
                                    downEvent.recycle()

                                    val upEvent = MotionEvent.obtain(downTime, eventTime + 100, MotionEvent.ACTION_UP, x, y, 0)
                                    wv.dispatchTouchEvent(upEvent)
                                    upEvent.recycle()
                                }
                                showControls = false
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange.copy(alpha = 0.8f))
                        ) {
                            Text("TV: Tap to Play", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
