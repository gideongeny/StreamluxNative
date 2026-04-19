package com.streamlux.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
                    isSelected = selectedTab == "Movies",
                    onClick = { selectedTab = "Movies" }
                )
                Spacer(modifier = Modifier.width(24.dp))
                TabButton(
                    title = "TV Shows",
                    isSelected = selectedTab == "TV Shows",
                    onClick = { selectedTab = "TV Shows" }
                )
            }

            // Category Feeds
            val currentData = if (selectedTab == "Movies") moviesData else tvData
            val mediaType = if (selectedTab == "Movies") "movie" else "tv"
            
            val trendingSection = currentData.find { it.title.contains("Trending", ignoreCase = true) }
            val scrollData = currentData.filter { it.title != trendingSection?.title }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    if (trendingSection != null && trendingSection.items.isNotEmpty()) {
                        HeroBanner(items = trendingSection.items, onMovieClick = { id -> onNavigateToDetail(id, mediaType) })
                    }
                }
                
                items(scrollData) { section ->
                    HomeSectionRow(
                        section = section,
                        mediaType = mediaType,
                        onMovieClick = { id -> onNavigateToDetail(id, mediaType) }
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
fun TabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
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

@Composable
fun HomeSectionRow(
    section: HomeSection,
    mediaType: String,
    onMovieClick: (Int) -> Unit
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
                            .clickable { onMovieClick(item.id) }
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
fun HeroBanner(items: List<TmdbItem>, onMovieClick: (Int) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { minOf(items.size, 5) })
    
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
                            colors = listOf(Color.Transparent, BackgroundDark),
                            startY = 600f
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
