package com.streamlux.app.ui.screens.player

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.streamlux.app.ui.theme.PrimaryOrange
import com.streamlux.app.utils.LiveStreamUrl

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLivePlayer(
    videoId: String,
    channelName: String = "Live TV",
    modifier: Modifier = Modifier,
    onReady: () -> Unit = {}
) {
    val context = LocalContext.current
    var reloadKey by remember { mutableIntStateOf(0) }
    var showEmbedFallback by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun openInYouTube() {
        val watchUrl = LiveStreamUrl.youtubeWatchUrl(videoId)
        val youtubeApp = Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl)).apply {
            setPackage("com.google.android.youtube")
        }
        if (youtubeApp.resolveActivity(context.packageManager) != null) {
            context.startActivity(youtubeApp)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl)))
        }
    }

    fun scheduleEmbedErrorCheck(webView: WebView) {
        mainHandler.postDelayed({
            webView.evaluateJavascript(
                """
                (function() {
                  var t = document.body ? document.body.innerText : '';
                  if (/unavailable|error code:\s*15/i.test(t)) return 'error';
                  return 'ok';
                })();
                """.trimIndent()
            ) { result ->
                if (result?.contains("error") == true) {
                    showEmbedFallback = true
                } else {
                    onReady()
                }
            }
        }, 2500L)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (!showEmbedFallback) {
                key(videoId, reloadKey) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.BLACK)
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        view?.let { scheduleEmbedErrorCheck(it) }
                                    }
                                }
                                loadDataWithBaseURL(
                                    LiveStreamUrl.YOUTUBE_EMBED_ORIGIN,
                                    LiveStreamUrl.youtubeLiveEmbedHtml(videoId),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Stream blocked in embed mode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "$channelName may require opening in the YouTube app on your device.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { openInYouTube() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open on YouTube", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                showEmbedFallback = false
                reloadKey++
            }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("RELOAD STREAM", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            TextButton(onClick = { openInYouTube() }) {
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("YOUTUBE APP", color = PrimaryOrange, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
