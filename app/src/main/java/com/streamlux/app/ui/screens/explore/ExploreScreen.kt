package com.streamlux.app.ui.screens.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamlux.app.ui.theme.BackgroundDark
import com.streamlux.app.ui.theme.PrimaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: LiveTVViewModel = hiltViewModel(),
    onNavigateToPlayer: (String, String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit
) {
    val activeCategory by viewModel.activeCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val allChannels by viewModel.allChannels.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val filteredChannels = allChannels.filter { channel ->
        val matchesCategory = activeCategory == "All" || channel.category == activeCategory
        val matchesSearch = channel.name.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 0.dp, bottom = 0.dp) // Removed extra bottom padding to reduce gap to navbar
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STREAMING", // Shortened per user stylistic request
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            
            IconButton(onClick = onNavigateToProfile) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                    modifier = Modifier.size(36.dp)
                ) {
                    val photoUrl = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl
                    if (photoUrl != null) {
                        coil.compose.AsyncImage(
                            model = photoUrl.toString(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search 50+ Premium Channels...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                focusedBorderColor = PrimaryOrange.copy(alpha = 0.5f),
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            shape = RoundedCornerShape(16.dp)
        )

        // Categories Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            items(categories) { cat ->
                val isSelected = activeCategory == cat
                TextButton(
                    onClick = { viewModel.setCategory(cat) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                        contentColor = if (isSelected) Color.White else Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = cat, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Channels Grid
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryOrange)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
            items(filteredChannels) { channel ->
                val context = androidx.compose.ui.platform.LocalContext.current
                val logoResId = remember(channel.logo) {
                    if (channel.logo != null) {
                        context.resources.getIdentifier(channel.logo, "drawable", context.packageName)
                    } else 0
                }

                Box(
                    modifier = Modifier
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        .clickable {
                            // Navigate to player with direct URL
                            onNavigateToPlayer(channel.url, channel.name)
                        }
                ) {
                    if (logoResId != 0) {
                        Image(
                            painter = painterResource(id = logoResId),
                            contentDescription = channel.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = channel.category,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                text = if (channel.type == "hls") "HD HLS" else "LIVE WEB",
                                color = if (channel.type == "hls") Color(0xFF60A5FA) else Color(0xFF4ADE80),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = channel.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Live Now", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (logoResId == 0) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
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
}

