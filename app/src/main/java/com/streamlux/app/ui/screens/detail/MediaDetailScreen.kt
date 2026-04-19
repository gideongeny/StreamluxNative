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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
    
    var commentText by remember { mutableStateOf("") }
    var showVidVault by remember { mutableStateOf(false) }
    var portalUrl by remember { mutableStateOf("") }

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
                            val encodedTitle = java.net.URLEncoder.encode(item.displayTitle, "UTF-8")
                            val encodedPoster = java.net.URLEncoder.encode(item.posterPath ?: "", "UTF-8")
                            onNavigateToPlayer(
                                "player/${viewModel.mediaType}/${viewModel.mediaId}?season=$selectedSeason&episode=1&title=$encodedTitle&poster=$encodedPoster"
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("PLAY NOW", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
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
                                    val trailerUrl = "https://www.youtube.com/watch?v=${filmInfo!!.trailerKey!!}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback if no app can handle intent
                                        onNavigateToPlayer("player/youtube/${filmInfo!!.trailerKey!!}?season=1&episode=1")
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

                        // Download Circle Icon
                        Surface(
                            onClick = { 
                                portalUrl = if (viewModel.mediaType == "tv")
                                    "https://dl.vidsrc.vip/tv/${viewModel.mediaId}"
                                else
                                    "https://dl.vidsrc.vip/movie/${viewModel.mediaId}"
                                showVidVault = true 
                            },
                            modifier = Modifier.size(44.dp),
                            color = PrimaryOrange.copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
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
                                        val encodedTitle = java.net.URLEncoder.encode(item.displayTitle, "UTF-8")
                                        val encodedPoster = java.net.URLEncoder.encode(item.posterPath ?: "", "UTF-8")
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
                                    
                                    IconButton(
                                        onClick = {
                                            portalUrl = "https://dl.vidsrc.vip/tv/${viewModel.mediaId}/$selectedSeason/${ep.episodeNumber}"
                                            showVidVault = true
                                        }
                                    ) {
                                        Icon(
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
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                            webViewClient = android.webkit.WebViewClient()
                            loadUrl(dlUrl)
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, start = 12.dp, end = 12.dp)
                        .background(Color(0xFF111111))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⬇ Download Portal", color = PrimaryOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { showVidVault = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}
