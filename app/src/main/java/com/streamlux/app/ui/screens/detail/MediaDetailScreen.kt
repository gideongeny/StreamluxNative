package com.streamlux.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import com.streamlux.app.ads.AdManager

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
                val route = "player/${viewModel.mediaType}/${viewModel.mediaId}?season=$selectedSeason&episode=1&title=$encodedTitle&poster=$encodedPoster"
                val activity = context.findActivity()
                if (activity != null) AdManager.showInterstitial(activity) { onNavigateToPlayer(route) }
                else onNavigateToPlayer(route)
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
                        val activity = context.findActivity()
                        if (activity != null) AdManager.showRewardedAd(activity) { onShowPortal(url) }
                        else onShowPortal(url)
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
fun CastSection(cast: List<com.streamlux.app.data.model.CastItem>) {
    if (cast.isNotEmpty()) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "CAST",
            color = PrimaryOrange,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            cast.forEach { member ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(80.dp)
                ) {
                    AsyncImage(
                        model = member.fullProfileUrl.takeIf { it.isNotBlank() },
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.DarkGray)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = member.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SimilarSection(
    similar: List<TmdbItem>,
    onNavigateToDetail: (Int, String) -> Unit
) {
    if (similar.isNotEmpty()) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "SIMILAR CONTENT",
            color = PrimaryOrange,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            similar.forEach { item ->
                val itemType = item.mediaType?.takeIf { it == "movie" || it == "tv" } ?: "movie"
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { onNavigateToDetail(item.id, itemType) }
                ) {
                    AsyncImage(
                        model = item.fullPosterUrl.takeIf { it.isNotBlank() },
                        contentDescription = item.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = item.displayTitle,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    val context = LocalContext.current
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
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item.seasons.filter { it.seasonNumber > 0 }.forEach { season ->
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
        
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (ep in seasonEpisodes) {
                val episodeStatus = downloadedEpisodes.find { 
                    it.seasonNumber == selectedSeason && it.episodeNumber == ep.episodeNumber 
                }
                
                val isUnreleased = ep.airDate != null && ep.airDate.isNotBlank() && ep.airDate > currentDate
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isUnreleased) {
                        val encodedTitle = Uri.encode(item.displayTitle)
                        val encodedPoster = Uri.encode(item.posterPath ?: "null")
                        val route = "player/${viewModel.mediaType}/${viewModel.mediaId}?season=$selectedSeason&episode=${ep.episodeNumber}&title=$encodedTitle&poster=$encodedPoster"
                        val activity = context.findActivity()
                        if (activity != null) AdManager.showInterstitial(activity) { onNavigateToPlayer(route) }
                        else onNavigateToPlayer(route)
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
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isUnreleased) 0.6f else 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isUnreleased) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${ep.episodeNumber}. ${ep.name}", 
                            color = if (isUnreleased) Color.Gray else Color.White, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp
                        )
                        Text(
                            text = ep.overview?.takeIf { it.isNotBlank() } ?: if (isUnreleased) "Coming soon." else "No overview available.", 
                            color = Color.Gray, 
                            fontSize = 12.sp, 
                            maxLines = 2, 
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (isUnreleased) {
                        Surface(
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SOON",
                                color = PrimaryOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (episodeStatus?.downloadStatus != "completed") {
                                    val url = "https://dl.vidsrc.vip/tv/${viewModel.mediaId}/$selectedSeason/${ep.episodeNumber}"
                                    val activity = context.findActivity()
                                    if (activity != null) AdManager.showRewardedAd(activity) { onShowPortal(url) }
                                    else onShowPortal(url)
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
    val loadError by viewModel.loadError.collectAsState()
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
                if (loadError != null) {
                    Text(loadError!!, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.retryDetail() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                } else {
                    CircularProgressIndicator(color = PrimaryOrange)
                    Spacer(Modifier.height(16.dp))
                    Text("Loading details...", color = Color.Gray)
                }
                androidx.compose.material3.TextButton(onClick = { onNavigateBack() }) {
                    Text("Go Back", color = PrimaryOrange)
                }
            }
        }
        return
    }

    val item = filmInfo!!.detail
    var commentText by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showVidVaultWebView by remember { mutableStateOf(false) }
    var portalUrl by remember { mutableStateOf("") }
    var downloadSeason by remember { mutableStateOf<Int?>(null) }
    var downloadEpisode by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = showDownloadDialog || showVidVaultWebView) {
        when {
            showVidVaultWebView -> showVidVaultWebView = false
            showDownloadDialog -> showDownloadDialog = false
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
                // LEFT COLUMN: Poster
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
                }
                // RIGHT COLUMN: Info & Actions
                Column(
                    modifier = Modifier.weight(1.5f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    Text(
                        text = item.displayTitle, 
                        style = MaterialTheme.typography.headlineLarge, 
                        color = MaterialTheme.colorScheme.onBackground, 
                        fontWeight = FontWeight.Black,
                        fontSize = 42.sp,
                        lineHeight = 48.sp
                    )
                    
                    Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        StarRating(rating = item.voteAverage ?: 0.0)
                        Spacer(modifier = Modifier.width(16.dp))
                        val releaseYear = (item.releaseDate ?: item.firstAirDate)?.split("-")?.firstOrNull() ?: ""
                        if (releaseYear.isNotEmpty()) {
                            Text(text = releaseYear, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        item.runtime?.let { runtime ->
                            Text(text = "${runtime / 60}h ${runtime % 60}m", color = Color.Gray, fontSize = 14.sp)
                        }
                    }

                    if (!item.genres.isNullOrEmpty()) {
                        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item.genres.take(3).forEach { genre ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    Text(
                                        text = genre.name.uppercase(),
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    DetailActions(
                        viewModel = viewModel, item = item, isAvailableOffline = isAvailableOffline,
                        downloadedItem = downloadedItem, isBookmarked = isBookmarked,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onShowPortal = { url -> portalUrl = url; showDownloadDialog = true },
                        selectedSeason = selectedSeason
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                    Text(text = "OVERVIEW", color = PrimaryOrange, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.overview ?: "No overview available.", color = Color.LightGray, style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp)
                    
                    CastSection(cast = filmInfo!!.credits)

                    if (viewModel.mediaType == "tv") {
                        SeasonAndEpisodes(
                            viewModel = viewModel, item = item, selectedSeason = selectedSeason,
                            seasonEpisodes = seasonEpisodes, downloadedEpisodes = downloadedEpisodes,
                            onNavigateToPlayer = onNavigateToPlayer,
                            onShowPortal = { url -> portalUrl = url; showDownloadDialog = true }
                        )
                    }

                    SimilarSection(similar = filmInfo!!.similar, onNavigateToDetail = onNavigateToDetail)
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    SocialSection(viewModel, comments, onNavigateToAuth)
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
                        onShowPortal = { url -> portalUrl = url; showDownloadDialog = true },
                        selectedSeason = selectedSeason
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "OVERVIEW", color = PrimaryOrange, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.overview ?: "No overview available.", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                    
                    CastSection(cast = filmInfo!!.credits)

                    if (viewModel.mediaType == "tv") {
                        SeasonAndEpisodes(
                            viewModel = viewModel, item = item, selectedSeason = selectedSeason,
                            seasonEpisodes = seasonEpisodes, downloadedEpisodes = downloadedEpisodes,
                            onNavigateToPlayer = onNavigateToPlayer,
                            onShowPortal = { url -> portalUrl = url; showDownloadDialog = true }
                        )
                    }

                    SimilarSection(similar = filmInfo!!.similar, onNavigateToDetail = onNavigateToDetail)
                    
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

        if (showDownloadDialog) {
            val uri = try { Uri.parse(portalUrl) } catch (_: Exception) { Uri.EMPTY }
            val pathSegments = uri.pathSegments
            val isTv = portalUrl.contains("/tv/")
            val s = if (isTv && pathSegments.size >= 4) pathSegments[2].toIntOrNull() else null
            val e = if (isTv && pathSegments.size >= 4) pathSegments[3].toIntOrNull() else null
            downloadSeason = s
            downloadEpisode = e

            val epItem = if (s != null && e != null) {
                seasonEpisodes.find { it.episodeNumber == e }
            } else null

            VylaDownloadDialog(
                viewModel = viewModel,
                season = s,
                episode = e,
                episodeName = epItem?.name,
                episodeStillPath = epItem?.stillPath,
                onClose = { showDownloadDialog = false },
                onOpenVidVault = {
                    viewModel.registerDownloadIntent(s, e, epItem?.name, epItem?.stillPath)
                    showDownloadDialog = false
                    showVidVaultWebView = true
                }
            )
        }

        if (showVidVaultWebView) {
            val epItem = if (downloadSeason != null && downloadEpisode != null) {
                seasonEpisodes.find { it.episodeNumber == downloadEpisode }
            } else null

            com.streamlux.app.ui.components.VidVaultWebViewOverlay(
                url = com.streamlux.app.utils.VidVaultUrlBuilder.build(
                    mediaType = viewModel.mediaType,
                    tmdbId = viewModel.mediaId,
                    season = downloadSeason,
                    episode = downloadEpisode
                ),
                tmdbId = viewModel.mediaId,
                title = item.displayTitle,
                onDownloadStarted = { systemDownloadId ->
                    viewModel.onDownloadStarted(
                        systemDownloadId = systemDownloadId,
                        quality = "VidVault",
                        season = downloadSeason,
                        episode = downloadEpisode,
                        episodeName = epItem?.name,
                        episodeStillPath = epItem?.stillPath
                    )
                },
                onClose = { showVidVaultWebView = false }
            )
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
fun VylaDownloadDialog(
    viewModel: MediaDetailViewModel,
    season: Int?,
    episode: Int?,
    episodeName: String?,
    episodeStillPath: String?,
    onClose: () -> Unit,
    onOpenVidVault: () -> Unit
) {
    val context = LocalContext.current
    val links by viewModel.downloadLinks.collectAsState()
    val isLoading by viewModel.vylaLoading.collectAsState()
    val error by viewModel.vylaError.collectAsState()

    androidx.compose.runtime.LaunchedEffect(season, episode) {
        try {
            viewModel.registerDownloadIntent(season, episode, episodeName, episodeStillPath)
            viewModel.fetchDownloadLinks(season, episode)
        } catch (e: Exception) {
            Log.e("VylaDownloadDialog", "Failed to load download links: ${e.message}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .clickable(enabled = false) {}, // Prevent closing when clicking card
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OPTION 1 — DIRECT CDN",
                            color = PrimaryOrange,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (season != null && episode != null) {
                                "Season $season, Episode $episode${episodeName?.let { ": $it" } ?: ""}"
                            } else {
                                viewModel.filmInfo.value?.detail?.displayTitle ?: "High-speed Direct CDN"
                            },
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryOrange,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Scanning direct CDN servers...",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                } else if (error != null || links.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (error != null) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = "Status",
                            tint = if (error != null) Color.Red else Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error ?: "No direct download channels found for this title.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your library entry is still saved. Try the VidVault portal inside the app.",
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenVidVault,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Option 2 — Open VidVault Portal",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(links) { link ->
                            val badgeBg = when (link.quality.lowercase()) {
                                "4k", "2160p" -> Color(0xFF8A2BE2) // Glowing Purple
                                "1080p" -> PrimaryOrange // Theme Primary Orange
                                "720p" -> Color(0xFF03A9F4) // Cool Ice Blue
                                else -> Color(0xFF555555) // Charcoal Gray
                            }

                            Surface(
                                onClick = {
                                    try {
                                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                        val request = DownloadManager.Request(Uri.parse(link.url)).apply {
                                            setMimeType("video/mp4")
                                            val displayName = viewModel.filmInfo.value?.detail?.displayTitle ?: "Media"
                                            val finalTitle = if (season != null && episode != null) {
                                                "$displayName S${season}E${episode}"
                                            } else {
                                                displayName
                                            }
                                            setTitle(finalTitle)
                                            setDescription("Downloading ${link.quality} from Server ${link.server}")
                                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            
                                            val extension = if (link.format.lowercase().contains("mkv")) "mkv" else "mp4"
                                            val cleanTitle = finalTitle.replace("[^a-zA-Z0-9]".toRegex(), "_")
                                            val fileName = "${cleanTitle}_${link.quality}_Server${link.server}.$extension"
                                            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                        }
                                        val systemDownloadId = dm.enqueue(request)
                                        
                                        viewModel.onDownloadStarted(
                                            systemDownloadId = systemDownloadId,
                                            quality = link.quality,
                                            season = season,
                                            episode = episode,
                                            episodeName = episodeName,
                                            episodeStillPath = episodeStillPath
                                        )
                                        Toast.makeText(context, "Direct download enqueued successfully!", Toast.LENGTH_SHORT).show()
                                        onClose()
                                    } catch (ex: Exception) {
                                        Toast.makeText(context, "Failed to start download: ${ex.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.03f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = badgeBg
                                        ) {
                                            Text(
                                                text = link.quality.uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Direct Server #${link.server}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Format: ${link.format.uppercase()}",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = link.size ?: "Unknown Size",
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (!isLoading && error == null && links.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "OPTION 2 — VIDVAULT PORTAL",
                            color = Color.Gray,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onOpenVidVault,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Open VidVault in App",
                                color = PrimaryOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
