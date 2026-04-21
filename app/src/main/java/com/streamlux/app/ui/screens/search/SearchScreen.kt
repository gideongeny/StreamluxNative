package com.streamlux.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import com.streamlux.app.data.model.MusicTrack
import com.streamlux.app.data.model.TVChannel
import com.streamlux.app.ui.components.GeniusAIViewModel
import coil.compose.AsyncImage
import com.streamlux.app.data.model.TmdbItem
import com.streamlux.app.ui.theme.BackgroundDark
import com.streamlux.app.ui.theme.PrimaryOrange
import com.streamlux.app.ui.components.GeniusAIChatbot
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int, String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    geniusViewModel: GeniusAIViewModel = hiltViewModel(viewModelStoreOwner = LocalContext.current as ComponentActivity)
) {
    var query by remember { mutableStateOf("") }
    val state by viewModel.searchResults.collectAsState()
    var lastAutoPromptedQuery by remember { mutableStateOf("") }

    val shouldShowGeniusAi = remember(state, query) {
        val results = state as? SearchState.Results
        results != null &&
            results.movies.isEmpty() &&
            results.tvShows.isEmpty() &&
            results.channels.isEmpty() &&
            results.music.isEmpty() &&
            query.isNotBlank() &&
            query.length > 2
    }

    // Trigger AI only once per zero-result query
    LaunchedEffect(state) {
        if (shouldShowGeniusAi && lastAutoPromptedQuery != query) {
            lastAutoPromptedQuery = query
            geniusViewModel.triggerAutoQuery(query)
        } else if (!shouldShowGeniusAi && lastAutoPromptedQuery == query) {
            geniusViewModel.setIsOpen(false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                }
                
                TextField(
                    value = query,
                    onValueChange = { 
                        query = it 
                        viewModel.search(it)
                    },
                    placeholder = { Text("Search Movies, TV Shows, Music...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp)
                        .height(56.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true
                )
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (val st = state) {
                    is SearchState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Search for your favorites", color = Color.Gray)
                        }
                    }
                    is SearchState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryOrange)
                        }
                    }
                    is SearchState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${st.message}", color = Color.Red)
                        }
                    }
                    is SearchState.Results -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (st.movies.isNotEmpty()) {
                                SearchSection("Movies", st.movies) { id -> onNavigateToDetail(id, "movie") }
                            }
                            if (st.tvShows.isNotEmpty()) {
                                SearchSection("TV Shows", st.tvShows) { id -> onNavigateToDetail(id, "tv") }
                            }
                            if (st.channels.isNotEmpty()) {
                                ChannelSearchSection("Live Channels", st.channels)
                            }
                            if (st.music.isNotEmpty()) {
                                MusicSearchSection("Music", st.music)
                            }
                            if (
                                st.movies.isEmpty() &&
                                st.tvShows.isEmpty() &&
                                st.channels.isEmpty() &&
                                st.music.isEmpty()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No results found for \"$query\"", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (shouldShowGeniusAi) {
            GeniusAIChatbot(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}



@Composable
fun SearchSection(title: String, items: List<TmdbItem>, onItemClick: (Int) -> Unit) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            color = PrimaryOrange,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        if (isTablet) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                modifier = Modifier.heightIn(max = 1000.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    SearchItem(item, onItemClick, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    SearchItem(item, onItemClick)
                }
            }
        }
    }
}

@Composable
fun SearchItem(item: TmdbItem, onClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 140.dp)
            .clickable { onClick(item.id) }
    ) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w500${item.posterPath}",
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = item.title ?: item.name ?: "",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ChannelSearchSection(title: String, items: List<TVChannel>) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            color = PrimaryOrange,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                Surface(
                    modifier = Modifier.width(180.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = PrimaryOrange)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.name,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.category,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicSearchSection(title: String, items: List<MusicTrack>) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            color = PrimaryOrange,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                Surface(
                    modifier = Modifier.width(220.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.thumbnail.isNotBlank()) {
                            AsyncImage(
                                model = item.thumbnail,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = PrimaryOrange)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.title,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.artist.ifBlank { item.source.uppercase() },
                                color = Color.Gray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
