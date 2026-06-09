package com.streamlux.app.ui.components

import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.streamlux.app.ui.theme.PrimaryOrange
import com.streamlux.app.utils.BlobDownloadInterface
import com.streamlux.app.utils.BrowserConstants
import com.streamlux.app.utils.VidVaultUrlBuilder
import com.streamlux.app.utils.findActivity

@Composable
fun VidVaultWebViewOverlay(
    url: String,
    tmdbId: String,
    title: String,
    onDownloadStarted: (systemDownloadId: Long) -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ComposeColor(0xFF1A1A1A))
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = ComposeColor.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VidVault Portal",
                        color = PrimaryOrange,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Text(
                        text = title,
                        color = ComposeColor.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { webViewRef?.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = ComposeColor.White)
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryOrange,
                    trackColor = ComposeColor.DarkGray
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val activityCtx = ctx.findActivity() ?: ctx
                    WebView(activityCtx).apply {
                        setBackgroundColor(Color.BLACK)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = BrowserConstants.DESKTOP_CHROME_UA
                            mediaPlaybackRequiresUserGesture = false
                        }
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        addJavascriptInterface(BlobDownloadInterface(activityCtx), "AndroidBlob")

                        setDownloadListener { downloadUrl, _, contentDisposition, mimeType, _ ->
                            try {
                                val dm = activityCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                val fileName = URLUtil.guessFileName(
                                    downloadUrl,
                                    contentDisposition,
                                    mimeType
                                ) ?: "${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.mp4"
                                val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                                    setMimeType(mimeType ?: "video/mp4")
                                    setTitle(title)
                                    setDescription("Downloading from VidVault")
                                    setNotificationVisibility(
                                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                    )
                                    setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS,
                                        fileName
                                    )
                                }
                                val systemDownloadId = dm.enqueue(request)
                                onDownloadStarted(systemDownloadId)
                                Toast.makeText(
                                    activityCtx,
                                    "Download started — check your notifications",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Log.e("VidVaultWebView", "Download failed: ${e.message}")
                                Toast.makeText(
                                    activityCtx,
                                    "Could not start download: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            true
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val target = request?.url?.toString().orEmpty()
                                if (target.startsWith("http://") || target.startsWith("https://")) {
                                    return false
                                }
                                return true
                            }

                            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                super.onPageFinished(view, finishedUrl)
                                isLoading = false
                                view?.evaluateJavascript(VidVaultUrlBuilder.tmdbInjectionScript(tmdbId), null)
                                val bridge =
                                    "(function(){ if(window.lxBridgeActive) return; window.lxBridgeActive=true; document.addEventListener('click', function(e){ var t=e.target.closest('a'); if(!t) return; var h=t.href; if(h&&(h.startsWith('blob:')||h.startsWith('data:'))){ e.preventDefault(); var xhr=new XMLHttpRequest(); xhr.open('GET', h, true); xhr.responseType='blob'; xhr.onload=function(){ if(this.status==200){ var b=this.response; var r=new FileReader(); r.onloadend=function(){ window.AndroidBlob.downloadBlob(r.result, t.getAttribute('download')||'file', b.type); }; r.readAsDataURL(b); } }; xhr.send(); } }, true); })();"
                                view?.evaluateJavascript(bridge, null)
                            }
                        }
                        webViewRef = this
                        loadUrl(url)
                    }
                },
                update = { view ->
                    webViewRef = view
                }
            )
        }
    }
}
