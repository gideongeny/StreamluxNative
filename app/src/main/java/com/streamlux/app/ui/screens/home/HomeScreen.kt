package com.streamlux.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import coil.compose.AsyncImage
import com.streamlux.app.data.model.HomeSection
import com.streamlux.app.data.model.TmdbItem
import com.streamlux.app.ui.theme.BackgroundDark
import com.streamlux.app.ui.theme.PrimaryOrange
import com.streamlux.app.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetail: (Int, String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val moviesData by viewModel.moviesData.collectAsState()
    val tvData by viewModel.tvData.collectAsState()

    var selectedTab by remember { mutableStateOf("Movies") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("STREAM", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black)
                        Text("LUX", color = PrimaryOrange, fontWeight = FontWeight.Black)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        val currentUser by viewModel.currentUser.collectAsState()
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (currentUser?.photoUrl != null) {
                                AsyncImage(
                                    model = currentUser?.photoUrl.toString(),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dual-Tier Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TabButton(
                    title = "Movies",
                    icon = "\uD83C\uDFAC",
                    isSelected = selectedTab == "Movies",
                    onClick = { selectedTab = "Movies" }
                )
                Spacer(modifier = Modifier.width(16.dp))
                TabButton(
                    title = "TV Shows",
                    icon = "\uD83D\uDCFA",
                    isSelected = selectedTab == "TV Shows",
                    onClick = { selectedTab = "TV Shows" }
                )
            }

            // Category Feeds
            val currentData = if (selectedTab == "Movies") moviesData else tvData
            val mediaType = if (selectedTab == "Movies") "movie" else "tv"
            
            val trendingSection = currentData.find { it.title.contains("Trending", ignoreCase = true) }
            val scrollData = currentData.filter { it.title != trendingSection?.title }

            val shortsData by viewModel.shortsData.collectAsState()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    if (trendingSection != null && trendingSection.items.isNotEmpty()) {
                        HeroBanner(
                            items = trendingSection.items, 
                            onMovieClick = { id -> onNavigateToDetail(id, mediaType) },
                            onPrefetch = { id -> viewModel.prefetchMedia(id, mediaType) }
                        )
                    }
                }

                // WORLD-CLASS: Vertical Shorts Feed (Aggregated via SerpApi)
                if (shortsData.isNotEmpty()) {
                    item {
                        ShortsSectionRow(shortsData)
                    }
                }
                
                items(scrollData) { section ->
                    HomeSectionRow(
                        section = section,
                        mediaType = mediaType,
                        onMovieClick = { id -> onNavigateToDetail(id, mediaType) },
                        onPrefetch = { id -> viewModel.prefetchMedia(id, mediaType) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(110.dp)) // Correct Clearance for Floating Nav
                }
            }
        }
    }
}

@Composable
fun ShortsSectionRow(shorts: List<com.streamlux.app.data.model.ShortVideoItem>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Discover Shorts",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "WORLDWIDE",
                color = PrimaryOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(shorts) { short ->
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(short.link))
                            context.startActivity(intent)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .height(240.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray)
                    ) {
                        AsyncImage(
                            model = short.thumbnail,
                            contentDescription = short.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Source Badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        ) {
                            Text(
                                text = short.source.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Play Icon Overlay
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(40.dp).align(Alignment.Center)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = short.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(title: String, icon: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) PrimaryOrange.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeSectionRow(
    section: HomeSection,
    mediaType: String,
    onMovieClick: (Int) -> Unit,
    onPrefetch: (Int) -> Unit
) {
    if (section.items.isNotEmpty()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(section.items) { item ->
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .height(175.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                            .combinedClickable(
                                onClick = { onMovieClick(item.id) },
                                onLongClick = { onPrefetch(item.id) }
                            )
                    ) {
                        AsyncImage(
                            model = item.fullPosterUrl,
                            contentDescription = item.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun HeroBanner(items: List<TmdbItem>, onMovieClick: (Int) -> Unit, onPrefetch: (Int) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { minOf(items.size, 5) })
    
    // Automatically prefetch the item that is currently centered in the Hero Banner
    LaunchedEffect(pagerState.currentPage) {
        if (items.isNotEmpty()) {
            onPrefetch(items[pagerState.currentPage].id)
        }
    }
    
    LaunchedEffect(pagerState) {
        while (true) {
            kotlinx.coroutines.delay(12000)
            if (pagerState.pageCount > 0) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .padding(bottom = 16.dp)
    ) { page ->
        val item = items[page]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onMovieClick(item.id) }
        ) {
            // Ken Burns Effect
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(15000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            AsyncImage(
                model = "https://image.tmdb.org/t/p/w1280${item.backdropPath ?: item.posterPath}",
                contentDescription = item.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().scale(scale)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f), BackgroundDark),
                            startY = 200f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, // Keeping white for hero as it sits over backdrop
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("STREAMLUX EXCLUSIVE", color = PrimaryOrange, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}
