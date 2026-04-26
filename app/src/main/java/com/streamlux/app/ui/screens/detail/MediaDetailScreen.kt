package com.streamlux.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.hilt.navigation.compose.hiltViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import com.streamlux.app.utils.findActivity
import com.streamlux.app.utils.BlobDownloadInterface
import com.streamlux.app.utils.BrowserConstants
import okhttp3.OkHttpClient
import okhttp3.Request
import coil.compose.AsyncImage
import com.streamlux.app.ui.components.StarRating
import com.streamlux.app.ui.theme.BackgroundDark
import com.streamlux.app.ui.theme.PrimaryOrange
import com.streamlux.app.data.model.TmdbItem
import com.streamlux.app.data.model.Episode
import androidx.palette.graphics.Palette
import coil.request.ImageRequest

@Composable
fun DetailActions(
    viewModel: MediaDetailViewModel,
    item: TmdbItem,
    isAvailableOffline: Boolean,
    downloadedItem: com.streamlux.app.data.local.LibraryEntity?,
    isBookmarked: Boolean,
    onNavigateToPlayer: (String) -> Unit,
    onShowPortal: (String) -> Unit,
    selectedSeason: Int
) {
    val context = LocalContext.current
    
    Column {
        Button(
            onClick = { 
                val encodedTitle = Uri.encode(item.displayTitle)
                val encodedPoster = Uri.encode(item.posterPath ?: "null")
                onNavigateToPlayer(
                    "player/${viewModel.mediaType}/${viewModel.mediaId}?season=$selectedSeason&episode=1&title=$encodedTitle&poster=$encodedPoster"
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAvailableOffline) Color(0xFF4CAF50) else PrimaryOrange
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = if (isAvailableOffline) Icons.Default.PlayCircle else Icons.Default.PlayArrow, 
                contentDescription = null, 
                tint = Color.White, 
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (isAvailableOffline) "WATCH OFFLINE" else "PLAY NOW", 
                color = Color.White, 
                fontWeight = FontWeight.Black, 
                fontSize = 18.sp, 
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trailer Button
            Button(
                onClick = {
                    val trailerUrl = "https://www.youtube.com/results?search_query=${Uri.encode(item.displayTitle + " trailer")}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.height(44.dp).weight(1.5f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryOrange)
                Spacer(Modifier.width(6.dp))
                Text("TRAILER", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            // Bookmark
            Surface(
                onClick = { viewModel.toggleBookmark() },
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if(isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if(isBookmarked) PrimaryOrange else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Download
            val downloadStatus = downloadedItem?.downloadStatus
            Surface(
                onClick = { 
                    if (downloadStatus != "completed") {
                        val url = if (viewModel.mediaType == "tv")
                            "https://dl.vidsrc.vip/tv/${viewModel.mediaId}"
                        else
                            "https://dl.vidsrc.vip/movie/${viewModel.mediaId}"
                        onShowPortal(url)
                    } else {
                        Toast.makeText(context, "Already downloaded", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(44.dp),
                color = if (downloadStatus == "completed") Color(0xFF4CAF50).copy(alpha = 0.15f) else PrimaryOrange.copy(alpha = 0.15f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (downloadStatus == "completed") Color(0xFF4CAF50).copy(alpha = 0.5f) else PrimaryOrange.copy(alpha = 0.3f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (downloadStatus) {
                        "completed" -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                        "downloading" -> CircularProgressIndicator(progress = (downloadedItem?.downloadProgress ?: 0) / 100f, modifier = Modifier.size(24.dp), color = PrimaryOrange, strokeWidth = 2.dp)
                        else -> Icon(Icons.Default.Download, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonAndEpisodes(
    viewModel: MediaDetailViewModel,
    item: TmdbItem,
    selectedSeason: Int,
    seasonEpisodes: List<Episode>,
    downloadedEpisodes: List<com.streamlux.app.data.local.LibraryEntity>,
    onNavigateToPlayer: (String) -> Unit,
    onShowPortal: (String) -> Unit
) {
    if (!item.seasons.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "SEASONS",
            color = PrimaryOrange,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(item.seasons.filter { it.seasonNumber > 0 }) { season ->
                val isSelected = selectedSeason == season.seasonNumber
                TextButton(
                    onClick = { viewModel.selectSeason(season.seasonNumber) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                        contentColor = if (isSelected) Color.White else Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(text = season.name, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (ep in seasonEpisodes) {
                val episodeStatus = downloadedEpisodes.find { 
                    it.seasonNumber == selectedSeason && it.episodeNumber == ep.episodeNumber 
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val encodedTitle = Uri.encode(item.displayTitle)
                        val encodedPoster = Uri.encode(item.posterPath ?: "null")
                        onNavigateToPlayer(
                            "player/${viewModel.mediaType}/${viewModel.mediaId}?season=$selectedSeason&episode=${ep.episodeNumber}&title=$encodedTitle&poster=$encodedPoster"
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(140.dp).height(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                        AsyncImage(
                            model = ep.fullStillUrl,
                            contentDescription = ep.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${ep.episodeNumber}. ${ep.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = ep.overview ?: "", color = Color.Gray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    
                    IconButton(
                        onClick = {
                            if (episodeStatus?.downloadStatus != "completed") {
                                onShowPortal("https://dl.vidsrc.vip/tv/${viewModel.mediaId}/$selectedSeason/${ep.episodeNumber}")
                            }
                        }
                    ) {
                        when (episodeStatus?.downloadStatus) {
                            "completed" -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                            "downloading" -> CircularProgressIndicator(progress = (episodeStatus.downloadProgress ?: 0) / 100f, modifier = Modifier.size(20.dp), color = PrimaryOrange, strokeWidth = 2.dp)
                            else -> Icon(Icons.Default.Download, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToDetail: (Int, String) -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: MediaDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val filmInfo by viewModel.filmInfo.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val seasonEpisodes by viewModel.seasonEpisodes.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val downloadedItem by viewModel.downloadedItem.collectAsState()
    val downloadedEpisodes by viewModel.downloadedEpisodes.collectAsState()
    
    val isAvailableOffline = downloadedItem?.downloadStatus == "completed" && downloadedItem?.localUri != null

    if (filmInfo == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PrimaryOrange)
                Spacer(Modifier.height(16.dp))
                Text("Loading details...", color = Color.Gray)
                androidx.compose.material3.TextButton(onClick = { onNavigateBack() }) {
                    Text("Go Back", color = PrimaryOrange)
                }
            }
        }
        return
    }

    val item = filmInfo!!.detail
    var commentText by remember { mutableStateOf("") }
    var showVidVault by remember { mutableStateOf(false) }
    var portalUrl by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = showVidVault) {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            showVidVault = false
        }
    }

    var dominantColor by remember { mutableStateOf<Color?>(null) }
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (dominantColor != null) {
                    Brush.verticalGradient(
                        colors = listOf(dominantColor!!.copy(alpha = 0.3f), bgColor, bgColor),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                } else {
                    androidx.compose.ui.graphics.SolidColor(bgColor)
                }
            )
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isTablet = configuration.screenWidthDp > 600

        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp)) {
                // LEFT COLUMN
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f/3f).clip(RoundedCornerShape(16.dp))) {
                        val imageRequest = ImageRequest.Builder(LocalContext.current)
                            .data("https://image.tmdb.org/t/p/w780${item.posterPath}")
                            .crossfade(true)
                            .allowHardware(false)
                            .listener(
                                onSuccess = { _, result ->
                                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                    bitmap?.let { b ->
                                        Palette.from(b).generate { palette ->
                                            palette?.dominantSwatch?.rgb?.let { colorValue ->
                                                dominantColor = Color(colorValue)
                                            }
                                        }
                                    }
                                }
                            )
                            .build()

                        AsyncImage(
                            model = imageRequest,
                            contentDescription = item.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = item.displayTitle, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black)
                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StarRating(rating = item.voteAverage ?: 0.0)
                        item.runtime?.let { runtime ->
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "${runtime / 60}h ${runtime % 60}m", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    DetailActions(
                        viewModel = viewModel, item = item, isAvailableOffline = isAvailableOffline,
                        downloadedItem = downloadedItem, isBookmarked = isBookmarked,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onShowPortal = { url -> portalUrl = url; showVidVault = true },
                        selectedSeason = selectedSeason
                    )
                }
                // RIGHT COLUMN
                Column(
                    modifier = Modifier.weight(1.5f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    Text(text = "OVERVIEW", color = PrimaryOrange, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.overview ?: "No overview available.", color = Color.LightGray, style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp)
                    
                    if (viewModel.mediaType == "tv") {
                        SeasonAndEpisodes(
                            viewModel = viewModel, item = item, selectedSeason = selectedSeason,
                            seasonEpisodes = seasonEpisodes, downloadedEpisodes = downloadedEpisodes,
                            onNavigateToPlayer = onNavigateToPlayer,
                            onShowPortal = { url -> portalUrl = url; showVidVault = true }
                        )
                    }
                }
            }
        } else {
            // MOBILE LAYOUT
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 60.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    val imageRequestMobile = ImageRequest.Builder(LocalContext.current)
                        .data("https://image.tmdb.org/t/p/w1280${item.backdropPath ?: item.posterPath}")
                        .crossfade(true)
                        .allowHardware(false)
                        .listener(
                            onSuccess = { _, result ->
                                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                bitmap?.let { b ->
                                    Palette.from(b).generate { palette ->
                                        palette?.dominantSwatch?.rgb?.let { colorValue ->
                                            dominantColor = Color(colorValue)
                                        }
                                    }
                                }
                            }
                        )
                        .build()

                    AsyncImage(
                        model = imageRequestMobile,
                        contentDescription = item.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background), startY = 100f)))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp)) {
                    Text(text = item.displayTitle, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black)
                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StarRating(rating = item.voteAverage ?: 0.0)
                        item.runtime?.let { runtime ->
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "${runtime / 60}h ${runtime % 60}m", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    DetailActions(
                        viewModel = viewModel, item = item, isAvailableOffline = isAvailableOffline,
                        downloadedItem = downloadedItem, isBookmarked = isBookmarked,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onShowPortal = { url -> portalUrl = url; showVidVault = true },
                        selectedSeason = selectedSeason
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "OVERVIEW", color = PrimaryOrange, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.overview ?: "No overview available.", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                    
                    if (viewModel.mediaType == "tv") {
                        SeasonAndEpisodes(
                            viewModel = viewModel, item = item, selectedSeason = selectedSeason,
                            seasonEpisodes = seasonEpisodes, downloadedEpisodes = downloadedEpisodes,
                            onNavigateToPlayer = onNavigateToPlayer,
                            onShowPortal = { url -> portalUrl = url; showVidVault = true }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    // Social / Comments
                    SocialSection(viewModel, comments, onNavigateToAuth)
                }
            }
        }

        // Overlay Elements
        IconButton(onClick = onNavigateBack, modifier = Modifier.padding(top = 40.dp, start = 12.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        if (showVidVault) {
            DownloadPortal(portalUrl, viewModel, { showVidVault = false }, { webViewRef = it })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialSection(viewModel: MediaDetailViewModel, comments: List<com.streamlux.app.data.model.Comment>, onNavigateToAuth: () -> Unit) {
    var commentText by remember { mutableStateOf("") }
    var ratingValue by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(text = "REVIEWS & COMMENTS", color = PrimaryOrange, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (viewModel.isUserLoggedIn()) {
            Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                TextField(
                    value = commentText, onValueChange = { commentText = it },
                    placeholder = { Text("Share your thoughts...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(containerColor = Color.Transparent, focusedIndicatorColor = PrimaryOrange, unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        (1..5).forEach { i ->
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = if (i <= ratingValue) PrimaryOrange else Color.Gray, modifier = Modifier.size(24.dp).clickable { ratingValue = i.toFloat() })
                        }
                    }
                    Button(onClick = { if (commentText.isNotBlank()) { viewModel.postComment(commentText, ratingValue); commentText = "" } }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange), shape = RoundedCornerShape(8.dp)) {
                        Text("Post")
                    }
                }
            }
        } else {
            Button(onClick = onNavigateToAuth, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp)) {
                Text("Sign in to leave a comment", color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        for (comment in comments) {
            Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp)).padding(16.dp).padding(bottom = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = comment.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row {
                        (1..5).forEach { i ->
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = if (i <= comment.rating) PrimaryOrange else Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = comment.content, color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun DownloadPortal(initialUrl: String, viewModel: MediaDetailViewModel, onClose: () -> Unit, onWebViewReady: (WebView) -> Unit) {
    val dlUrl = initialUrl.ifEmpty { if (viewModel.mediaType == "tv") "https://dl.vidsrc.vip/tv/${viewModel.mediaId}" else "https://dl.vidsrc.vip/movie/${viewModel.mediaId}" }
    var webViewRefLocal by remember { mutableStateOf<WebView?>(null) }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f))) {
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(top = 48.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRefLocal = this
                    onWebViewReady(this)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = BrowserConstants.DESKTOP_CHROME_UA
                    }
                    addJavascriptInterface(BlobDownloadInterface(ctx), "AndroidBlob")
                    setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                        try {
                            val request = DownloadManager.Request(Uri.parse(url))
                            request.setMimeType(mimetype)
                            request.addRequestHeader("User-Agent", userAgent)
                            request.addRequestHeader("Referer", dlUrl)
                            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
                            request.setDescription("Downloading $fileName")
                            request.setTitle(fileName)
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val downloadId = dm.enqueue(request)
                            val urlPath = Uri.parse(url).pathSegments
                            val s = urlPath.getOrNull(urlPath.size - 2)?.toIntOrNull() ?: 1
                            val e = urlPath.lastOrNull()?.toIntOrNull() ?: 1
                            viewModel.onDownloadStarted(systemDownloadId = downloadId, quality = if (fileName.contains("1080")) "1080p" else "720p", season = s, episode = e)
                            Toast.makeText(ctx, "Download started", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) { Log.e("StreamLuxPortal", "Download failed", e) }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(v: WebView?, u: String?) {
                            val bridge = "(function(){ if(window.lxActive) return; window.lxActive=true; document.addEventListener('click', function(e){ var t=e.target.closest('a'); if(!t) return; var h=t.href; if(h&&(h.startsWith('blob:')||h.startsWith('data:'))){ e.preventDefault(); var xhr=new XMLHttpRequest(); xhr.open('GET', h, true); xhr.responseType='blob'; xhr.onload=function(){ if(this.status==200){ var b=this.response; var r=new FileReader(); r.onloadend=function(){ window.AndroidBlob.downloadBlob(r.result, t.getAttribute('download')||'file', b.type); }; r.readAsDataURL(b); } }; xhr.send(); } }, true); })();"
                            v?.evaluateJavascript(bridge, null)
                        }
                    }
                    loadUrl(dlUrl)
                }
            }
        )
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⬇ Download Portal", color = PrimaryOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { webViewRefLocal?.loadUrl(dlUrl) }) { Icon(Icons.Default.Home, null, tint = Color.White) }
                IconButton(onClick = { webViewRefLocal?.reload() }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        }
    }
}
