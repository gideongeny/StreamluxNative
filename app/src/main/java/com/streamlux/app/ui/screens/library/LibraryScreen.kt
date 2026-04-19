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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    geniusViewModel: GeniusAIViewModel = hiltViewModel(viewModelStoreOwner = LocalContext.current as ComponentActivity),
    onNavigateToDetail: (String, String) -> Unit,
    initialTab: String = "Watchlist"
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
                        onNavigateToDetail = onNavigateToDetail
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
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentList) { item ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(8.dp))
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
    onNavigateToDetail: (String, String) -> Unit
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
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(downloads, key = { it.mediaId }) { item ->
                DownloadItemCard(
                    item = item,
                    onDelete = onDelete,
                    onClick = { onNavigateToDetail(item.mediaId, item.mediaType) }
                )
            }
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

    val statusColor = when (item.downloadStatus) {
        "completed" -> Color(0xFF4CAF50)
        "downloading" -> PrimaryOrange
        "paused" -> Color(0xFFFFEB3B)
        "failed" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    val statusIcon = when (item.downloadStatus) {
        "completed" -> Icons.Default.CheckCircle
        "downloading" -> Icons.Default.Refresh
        "paused" -> Icons.Default.Pause
        "failed" -> Icons.Default.Warning
        else -> Icons.Default.HourglassEmpty
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w185${item.posterPath}",
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Status row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (item.downloadStatus?.replaceFirstChar { it.uppercase() } ?: "Unknown") +
                                if (item.downloadQuality != null) " • ${item.downloadQuality}" else "",
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Progress bar (only for active/paused)
                if (item.downloadStatus != "completed" && item.downloadStatus != "failed") {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = statusColor,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.downloadProgress}%",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete button
            IconButton(
                onClick = { onDelete(item) },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove download",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
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
