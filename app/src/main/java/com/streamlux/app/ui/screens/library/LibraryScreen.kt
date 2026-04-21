package com.streamlux.app.ui.screens.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import com.streamlux.app.data.local.LibraryEntity
import com.streamlux.app.ui.components.GeniusAIViewModel
import coil.compose.AsyncImage
import com.streamlux.app.ui.components.GeniusAIChatbot
import com.streamlux.app.ui.theme.BackgroundDark
import com.streamlux.app.ui.theme.PrimaryOrange
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.mutableStateMapOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToDetail: (String, String) -> Unit,
    onNavigateToPlayer: (String, String, Int, Int, String, String) -> Unit,
    initialTab: String = "Watchlist",
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val watchlist by viewModel.watchlist.collectAsState()
    val history by viewModel.history.collectAsState()
    val bookmarked by viewModel.bookmarked.collectAsState()
    val downloads by viewModel.downloads.collectAsState()

    var selectedTab by remember { mutableStateOf(initialTab) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My Library", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TabItem(
                    title = "Watchlist",
                    isSelected = selectedTab == "Watchlist",
                    onClick = { selectedTab = "Watchlist" }
                )
                TabItem(
                    title = "History",
                    isSelected = selectedTab == "History",
                    onClick = { selectedTab = "History" }
                )
                TabItem(
                    title = "Bookmarked",
                    isSelected = selectedTab == "Bookmarked",
                    onClick = { selectedTab = "Bookmarked" }
                )
                TabItem(
                    title = "Downloads",
                    isSelected = selectedTab == "Downloads",
                    badge = downloads.count { it.downloadStatus == "downloading" || it.downloadStatus == "queued" }.takeIf { it > 0 },
                    onClick = { selectedTab = "Downloads" }
                )
            }

            when (selectedTab) {
                "Downloads" -> {
                    DownloadsTab(
                        downloads = downloads,
                        onDelete = { viewModel.deleteDownload(it) },
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToPlayer = onNavigateToPlayer
                    )
                }
                else -> {
                    val currentList = when (selectedTab) {
                        "Watchlist" -> watchlist
                        "History" -> history
                        else -> bookmarked
                    }

                    if (currentList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No items found in $selectedTab.", color = Color.Gray)
                        }
                    } else {
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val isTablet = configuration.screenWidthDp > 600

                        LazyVerticalGrid(
                            columns = if (isTablet) GridCells.Adaptive(minSize = 130.dp) else GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentList) { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                        .clickable { onNavigateToDetail(item.mediaId, item.mediaType) }
                                ) {
                                    AsyncImage(
                                        model = "https://image.tmdb.org/t/p/w500${item.posterPath}",
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsTab(
    downloads: List<LibraryEntity>,
    onDelete: (LibraryEntity) -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    onNavigateToPlayer: (String, String, Int, Int, String, String) -> Unit
) {
    if (downloads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No downloads yet", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Start a download in the web portal", color = Color.Gray.copy(alpha = 0.6f), fontSize = 14.sp)
            }
        }
    } else {
        // Group by parentId (TV shows) or id (Movies)
        val groups = remember(downloads) {
            downloads.groupBy { it.parentId ?: it.id }
        }
        
        val expandedIds = remember { mutableStateMapOf<String, Boolean>() }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            groups.forEach { (groupId, items) ->
                val firstItem = items.first()
                val isSeries = items.size > 1 || firstItem.mediaType == "tv"
                
                item(key = groupId) {
                    if (isSeries) {
                        SeriesDownloadAccordion(
                            seriesId = groupId,
                            title = firstItem.seriesTitle ?: firstItem.title,
                            posterPath = firstItem.posterPath,
                            episodes = items,
                            isExpanded = expandedIds[groupId] == true,
                            onToggleExpand = { expandedIds[groupId] = !(expandedIds[groupId] ?: false) },
                            onDeleteEpisode = onDelete,
                            onPlayEpisode = { 
                                if (it.downloadStatus == "completed") {
                                    onNavigateToPlayer(it.mediaId, it.mediaType, it.seasonNumber ?: 1, it.episodeNumber ?: 1, it.seriesTitle ?: it.title, it.posterPath ?: "")
                                } else {
                                    onNavigateToDetail(it.mediaId, it.mediaType) 
                                }
                            }
                        )
                    } else {
                        DownloadItemCard(
                            item = firstItem,
                            onDelete = onDelete,
                            onClick = { 
                                if (firstItem.downloadStatus == "completed") {
                                    onNavigateToPlayer(firstItem.mediaId, firstItem.mediaType, firstItem.seasonNumber ?: 1, firstItem.episodeNumber ?: 1, firstItem.title, firstItem.posterPath ?: "")
                                } else {
                                    onNavigateToDetail(firstItem.mediaId, firstItem.mediaType) 
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesDownloadAccordion(
    seriesId: String,
    title: String,
    posterPath: String?,
    episodes: List<LibraryEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteEpisode: (LibraryEntity) -> Unit,
    onPlayEpisode: (LibraryEntity) -> Unit
) {
    val completedCount = episodes.count { it.downloadStatus == "completed" }
    val allCompleted = completedCount == episodes.size
    val avgProgress = if (!allCompleted) episodes.map { it.downloadProgress }.average().toInt() else 100

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column {
            // Series header — full-width banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clickable { onToggleExpand() }
            ) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w780$posterPath",
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xF0000000)),
                            startY = 40f
                        )
                    )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(PrimaryOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("TV SERIES", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        if (allCompleted) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("$completedCount episodes ready", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Text("$completedCount/${episodes.size} downloaded", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    if (!allCompleted) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { avgProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)),
                            color = PrimaryOrange,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
                // Expand/collapse chevron
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .padding(4.dp)
                )
            }

            if (isExpanded) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    episodes
                        .sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
                        .forEach { episode ->
                            DownloadEpisodeRow(
                                episode = episode,
                                onDelete = { onDeleteEpisode(episode) },
                                onClick = { onPlayEpisode(episode) }
                            )
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                }
            }
        }
    }
}


@Composable
fun DownloadEpisodeRow(
    episode: LibraryEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val isCompleted = episode.downloadStatus == "completed"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isCompleted) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Episode thumbnail placeholder
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A3E)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w227_and_h127_bestv2${episode.episodeStillPath ?: episode.posterPath}",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        // Episode info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "S${episode.seasonNumber} · E${episode.episodeNumber}",
                color = PrimaryOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = episode.episodeName ?: episode.title,
                color = if (isCompleted) Color.White else Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.downloadQuality != null && isCompleted) {
                Text(episode.downloadQuality!!, color = Color.Gray, fontSize = 11.sp)
            } else if (!isCompleted) {
                Text(
                    "${episode.downloadProgress}% · ${episode.downloadStatus}",
                    color = PrimaryOrange,
                    fontSize = 11.sp
                )
            }
        }
        // Delete
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}


@Composable
fun DownloadItemCard(
    item: LibraryEntity,
    onDelete: (LibraryEntity) -> Unit,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = item.downloadProgress / 100f,
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )
    val isCompleted = item.downloadStatus == "completed"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            // Backdrop/poster as full-width background
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w780${item.posterPath}",
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Dark gradient overlay for text readability
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xE6000000)),
                        startY = 60f
                    )
                )
            )
            // Top-right delete button
            IconButton(
                onClick = { onDelete(item) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            // Bottom content — title, badges, play button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                // Quality & type badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.downloadQuality != null) {
                        Box(
                            modifier = Modifier
                                .background(PrimaryOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(item.downloadQuality!!, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (item.mediaType == "tv") "TV SERIES" else "MOVIE",
                            color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                // Title
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                if (isCompleted) {
                    // Play button
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    // Progress bar for active downloads
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                item.downloadStatus?.replaceFirstChar { it.uppercase() } ?: "",
                                color = PrimaryOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium
                            )
                            Text("${item.downloadProgress}%", color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = PrimaryOrange,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabItem(title: String, isSelected: Boolean, badge: Int? = null, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box {
            Text(
                text = title,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
            )
            // Active download badge
            if (badge != null && badge > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(PrimaryOrange, CircleShape)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.toString(),
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(40.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(PrimaryOrange)
            )
        }
    }
}
