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
    
    // Add logic to check if we can play offline
    val isAvailableOffline = downloadedItem?.downloadStatus == "completed" && downloadedItem?.localUri != null

    if (filmInfo == null) {
        // LOADING OR OFFLINE STATE
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator(color = PrimaryOrange)
                Spacer(Modifier.height(16.dp))
                Text("Loading details...", color = Color.Gray)
                Text("(Check connection if this takes too long)", color = Color.DarkGray, fontSize = 12.sp)
                
                Spacer(Modifier.height(24.dp))
                androidx.compose.material3.TextButton(onClick = { onNavigateBack() }) {
                    Text("Go Back", color = PrimaryOrange)
                }
            }
        }
        return
    }

    val item = filmInfo!!.detail // Guaranteed non-null after check
    
    var commentText by remember { mutableStateOf("") }
    var showVidVault by remember { mutableStateOf(false) }
    var portalUrl by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // UNIVERSAL BACK NAVIGATION: Handle hardware back button for portal
    BackHandler(enabled = showVidVault) {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            showVidVault = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (filmInfo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryOrange)
            }
        } else {
            val item = filmInfo!!.detail
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 60.dp)
            ) {
                // Backdrop Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w1280${item.backdropPath ?: item.posterPath}",
                        contentDescription = item.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                    startY = 100f
                                )
                            )
                    )
                }

                // Content
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp)) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black
                    )
                    
                    // Metadata Row
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StarRating(rating = item.voteAverage ?: 0.0)
                        
                        item.runtime?.let { runtime ->
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "${runtime / 60}h ${runtime % 60}m", color = Color.Gray, fontSize = 14.sp)
                        }
                        
                        val date = item.releaseDate ?: item.firstAirDate
                        if (!date.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = date.substring(0, 4), color = Color.Gray, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Row 1: Play Now (Full Width)
                    Button(
                        onClick = { 
                            val encodedTitle = android.net.Uri.encode(item.displayTitle)
                            val encodedPoster = android.net.Uri.encode(item.posterPath ?: "null")
                            
                            // If it's already downloaded, play it immediately
                            // The ID for the player is mediaId (unified for online/offline lookup)
                            onNavigateToPlayer(
                                "player/${viewModel.mediaType}/${viewModel.mediaId}?season=$selectedSeason&episode=1&title=$encodedTitle&poster=$encodedPoster"
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAvailableOffline) Color(0xFF4CAF50) else PrimaryOrange // Green for offline availability
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

                    // Row 2: Trailer, Bookmark, Download
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Trailer Button (Native Redirect)
                        if (filmInfo?.trailerKey != null) {
                            Button(
                                onClick = {
                                    val trailerUrl = "https://www.youtube.com/watch?v=${filmInfo?.trailerKey}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback if no app can handle intent
                                        onNavigateToPlayer("player/youtube/${filmInfo?.trailerKey}?season=1&episode=1")
                                    }
                                },
                                modifier = Modifier.height(44.dp).weight(1.5f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryOrange)
                                Spacer(Modifier.width(6.dp))
                                Text("TRAILER", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        // Bookmark Icon Button
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

                        // Download / Status Icon
                        val downloadStatus = downloadedItem?.downloadStatus
                        Surface(
                            onClick = { 
                                if (downloadStatus == "completed") {
                                    Toast.makeText(context, "Already downloaded", Toast.LENGTH_SHORT).show()
                                } else {
                                    portalUrl = if (viewModel.mediaType == "tv")
                                        "https://dl.vidsrc.vip/tv/${viewModel.mediaId}"
                                    else
                                        "https://dl.vidsrc.vip/movie/${viewModel.mediaId}"
                                    showVidVault = true 
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            color = when (downloadStatus) {
                                "completed" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                "downloading" -> PrimaryOrange.copy(alpha = 0.25f)
                                else -> PrimaryOrange.copy(alpha = 0.15f)
                            },
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (downloadStatus == "completed") Color(0xFF4CAF50).copy(alpha = 0.5f) else PrimaryOrange.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (downloadStatus) {
                                    "completed" -> Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    "downloading" -> {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            progress = (downloadedItem?.downloadProgress ?: 0) / 100f,
                                            modifier = Modifier.size(24.dp),
                                            color = PrimaryOrange,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    else -> Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = PrimaryOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "OVERVIEW",
                        color = PrimaryOrange,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.overview ?: "No overview available.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )

                    // TV SHOW EPISODE PICKER Logic
                    if (viewModel.mediaType == "tv" && !item.seasons.isNullOrEmpty()) {
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
                            seasonEpisodes.forEach { ep ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val encodedTitle = android.net.Uri.encode(item.displayTitle)
                                        val encodedPoster = android.net.Uri.encode(item.posterPath ?: "null")
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
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = ep.overview ?: "",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    // Individual Episode Download Status
                                    val episodeStatus = downloadedEpisodes.find { 
                                        it.seasonNumber == selectedSeason && it.episodeNumber == ep.episodeNumber 
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            if (episodeStatus?.downloadStatus != "completed") {
                                                portalUrl = "https://dl.vidsrc.vip/tv/${viewModel.mediaId}/$selectedSeason/${ep.episodeNumber}"
                                                showVidVault = true
                                            } else {
                                                Toast.makeText(context, "Episode downloaded", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        when (episodeStatus?.downloadStatus) {
                                            "completed" -> Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Downloaded",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            "downloading" -> {
                                                androidx.compose.material3.CircularProgressIndicator(
                                                    progress = (episodeStatus.downloadProgress ?: 0) / 100f,
                                                    modifier = Modifier.size(20.dp),
                                                    color = PrimaryOrange,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                            else -> Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download Episode",
                                                tint = PrimaryOrange,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Cast Row
                    if (filmInfo!!.credits.isNotEmpty()) {
                        Text(
                            text = "TOP CAST",
                            color = PrimaryOrange,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(filmInfo!!.credits) { cast ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                    AsyncImage(
                                        model = cast.fullProfileUrl,
                                        contentDescription = cast.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.DarkGray)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = cast.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = cast.character ?: "",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Recommendations Row
                    if (filmInfo!!.similar.isNotEmpty()) {
                        Text(
                            text = "SIMILAR TITLES",
                            color = PrimaryOrange,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filmInfo!!.similar.filter { it.posterPath != null }) { rec ->
                                Box(
                                    modifier = Modifier
                                        .width(115.dp)
                                        .height(175.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                        .clickable { onNavigateToDetail(rec.id, viewModel.mediaType) }
                                ) {
                                    AsyncImage(
                                        model = rec.fullPosterUrl,
                                        contentDescription = rec.displayTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                     }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Social Layer (Comments & Ratings)
                    Text(
                        text = "REVIEWS & COMMENTS",
                        color = PrimaryOrange,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val commentsList = comments
                    var ratingValue by remember { mutableStateOf(0f) }
                    
                    if (viewModel.isUserLoggedIn()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            TextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("Share your thoughts...", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.textFieldColors(
                                    containerColor = Color.Transparent,
                                    focusedIndicatorColor = PrimaryOrange,
                                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Simple star rating (text based for now, but premium feel)
                                Row {
                                    (1..5).forEach { i ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i <= ratingValue) PrimaryOrange else Color.Gray,
                                            modifier = Modifier.size(24.dp).clickable { ratingValue = i.toFloat() }
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (commentText.isNotBlank()) {
                                            viewModel.postComment(commentText, ratingValue)
                                            commentText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Post")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onNavigateToAuth,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Sign in to leave a comment", color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        commentsList.forEach { comment ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = comment.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row {
                                        (1..5).forEach { i ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (i <= comment.rating) PrimaryOrange else Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = comment.content, color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Top App Bar Overlaid
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(top = 40.dp, start = 12.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // Download Modal — uses dl.vidsrc.vip (same as React app smartRedirect)
        if (showVidVault) {
            val dlUrl = portalUrl.ifEmpty { 
                if (viewModel.mediaType == "tv")
                    "https://dl.vidsrc.vip/tv/${viewModel.mediaId}"
                else
                    "https://dl.vidsrc.vip/movie/${viewModel.mediaId}"
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f))) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp),
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            webViewRef = this
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = BrowserConstants.DESKTOP_CHROME_UA
                                setSupportMultipleWindows(true)
                                javaScriptCanOpenWindowsAutomatically = true
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

                                    // RECORD FOR LIBRARY
                                    // The DL portal URL is path-based: /tv/{mediaId}/{season}/{episode}
                                    // NOT query-param based — must parse from path segments
                                    val currentWebUrl = webViewRef?.url ?: portalUrl
                                    val urlPath = Uri.parse(currentWebUrl).pathSegments
                                    // Path: ["tv", mediaId, season, episode] or similar
                                    val s = urlPath.getOrNull(urlPath.size - 2)?.toIntOrNull() ?: selectedSeason
                                    val e = urlPath.lastOrNull()?.toIntOrNull() ?: 1
                                    
                                    val epInfo = if (viewModel.mediaType == "tv") seasonEpisodes.find { it.episodeNumber == e } else null
                                    
                                    viewModel.onDownloadStarted(
                                        systemDownloadId = downloadId,
                                        quality = if (fileName.contains("1080")) "1080p" else if (fileName.contains("720")) "720p" else "480p",
                                        season = if (viewModel.mediaType == "tv") s else null,
                                        episode = if (viewModel.mediaType == "tv") e else null,
                                        episodeName = epInfo?.name,
                                        episodeStillPath = epInfo?.stillPath
                                    )

                                    Toast.makeText(ctx, "Download started: ${if (viewModel.mediaType == "tv") "S${s}E${e}" else fileName}", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Log.e("StreamLuxPortal", "Download failed", e)
                                    Toast.makeText(ctx, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }

                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                     val url = request?.url?.toString() ?: return null
                                     
                                     // IMMUNITY: Never intercept top-level navigation (Main Frame)
                                     // to preserve native browser fingerprints on the portal.
                                     if (request.isForMainFrame) return null

                                     val isMirror = BrowserConstants.MIRROR_DOMAINS.any { url.contains(it, true) }
                                    
                                    if (isMirror && request.method == "GET") {
                                        try {
                                            val client = OkHttpClient.Builder().followRedirects(true).build()
                                            val reqBuilder = Request.Builder().url(url)
                                            request.requestHeaders.forEach { (k, v) ->
                                                if (!k.equals("X-Requested-With", true)) {
                                                    reqBuilder.addHeader(k, v)
                                                }
                                            }
                                            val resp = client.newCall(reqBuilder.build()).execute()
                                            val body = resp.body
                                            if (body != null) {
                                                val contentType = resp.header("Content-Type", "text/html") ?: "text/html"
                                                return WebResourceResponse(
                                                    contentType.split(";")[0],
                                                    resp.header("Content-Encoding", "UTF-8"),
                                                    body.byteStream()
                                                )
                                            }
                                        } catch (e: Exception) { }
                                    }
                                    return null
                                }

                                override fun onPageFinished(v: WebView?, u: String?) {
                                    super.onPageFinished(v, u)
                                    val bridge = "(function(){ if(window.lxActive) return; window.lxActive=true; document.addEventListener('click', function(e){ var t=e.target.closest('a'); if(!t) return; var h=t.href; if(h&&(h.startsWith('blob:')||h.startsWith('data:'))){ e.preventDefault(); var xhr=new XMLHttpRequest(); xhr.open('GET', h, true); xhr.responseType='blob'; xhr.onload=function(){ if(this.status==200){ var b=this.response; var r=new FileReader(); r.onloadend=function(){ window.AndroidBlob.downloadBlob(r.result, t.getAttribute('download')||'file', b.type); }; r.readAsDataURL(b); } }; xhr.send(); } }, true); })();"
                                    v?.evaluateJavascript(bridge, null)
                                }
                            }
                            loadUrl(dlUrl)
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .background(Color(0xFF111111))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⬇ Download Portal", color = PrimaryOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            webViewRef?.loadUrl(dlUrl)
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        
                        IconButton(onClick = { 
                            webViewRef?.reload() 
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = { showVidVault = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
