package com.streamlux.app.ui.screens.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.streamlux.app.data.model.SportsFixture
import com.streamlux.app.data.model.SportsHighlight
import com.streamlux.app.ui.theme.BackgroundDark
import com.streamlux.app.ui.theme.PrimaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsScreen(
    viewModel: SportsViewModel = hiltViewModel(),
    onNavigateToPlayer: (String, String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val liveMatches by viewModel.liveMatches.collectAsState()
    val upcomingMatches by viewModel.upcomingMatches.collectAsState()
    val finishedMatches by viewModel.finishedMatches.collectAsState()
    val highlights by viewModel.highlights.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SPORTS",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "LIVE ARENA",
                            color = PrimaryOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSportsData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Scores",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        if (currentUser?.photoUrl != null) {
                            AsyncImage(
                                model = currentUser.photoUrl.toString(),
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                modifier = Modifier.size(32.dp)
                            ) {
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = PrimaryOrange,
                    trackColor = MaterialTheme.colorScheme.background
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp, top = if (isLoading) 8.dp else 0.dp)
            ) {
            // Live Fixtures Section (Vertical Mode)
            if (liveMatches.isNotEmpty()) {
                item {
                    SportsSectionHeader("Live Matches", icon = Icons.Default.LiveTv, color = Color.Red)
                }
                items(liveMatches) { fixture ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        LiveFixtureCard(fixture, onClick = {
                            // Navigation to Sports TV Hub
                            onNavigateToPlayer("sports", "hub")
                        })
                    }
                }
            }

            // Highlights Section (Vertical Mode)
            if (highlights.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SportsSectionHeader("Video Highlights", icon = Icons.Default.Star)
                }
                items(highlights) { highlight ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        HighlightCard(highlight, onClick = {
                            viewModel.getHighlightUrl(highlight)?.let { url ->
                                onNavigateToPlayer("sports", java.net.URLEncoder.encode(url, "UTF-8"))
                            }
                        })
                    }
                }
            }

            // Completed Matches Section
            if (finishedMatches.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SportsSectionHeader("Completed Matches", icon = Icons.Default.CheckCircle, color = Color.Gray)
                }
                items(finishedMatches) { fixture ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        LiveFixtureCard(fixture, onClick = {
                            // Can navigate to a detail screen if you implement post-match breakdown later
                        })
                    }
                }
            }

            // Upcoming Fixtures Section
            if (upcomingMatches.isNotEmpty()) {
                item {
                    SportsSectionHeader("Upcoming Fixtures", icon = Icons.Default.Schedule)
                }
                items(upcomingMatches) { fixture ->
                    UpcomingFixtureRow(fixture, onClick = {
                        val url = viewModel.getStreamUrl(fixture)
                        onNavigateToPlayer("sports", java.net.URLEncoder.encode(url, "UTF-8"))
                    })
                }
            }
        }
    }
}
}

@Composable
fun SportsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = PrimaryOrange) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun LiveFixtureCard(fixture: SportsFixture, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // Changed to fillMaxWidth for vertical mode
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(fixture.leagueName, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Red))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(fixture.minute ?: "LIVE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamColumn(fixture.homeTeam, fixture.homeTeamLogo, fixture.homeScore ?: "0")
                Text("VS", color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 14.sp)
                TeamColumn(fixture.awayTeam, fixture.awayTeamLogo, fixture.awayScore ?: "0")
            }
        }
    }
}

@Composable
fun TeamColumn(name: String, logo: String?, score: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = logo ?: "https://images.unsplash.com/photo-1574629810360-7efbbe195018",
            contentDescription = name,
            modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(score, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun HighlightCard(highlight: SportsHighlight, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) { // Changed to fillMaxWidth for vertical mode
        Box(
            modifier = Modifier
                .height(130.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
        ) {
            AsyncImage(
                model = highlight.thumbnail,
                contentDescription = highlight.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp).align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(highlight.title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
        Text(highlight.competition, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun UpcomingFixtureRow(fixture: SportsFixture, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(fixture.leagueName, color = PrimaryOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${fixture.homeTeam} vs ${fixture.awayTeam}", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (fixture.countdown != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Green, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Starts in ${fixture.countdown}", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(fixture.kickoffTime.split("T").firstOrNull() ?: fixture.kickoffTime, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(fixture.venue ?: "Official Stadium", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 10.sp)
        }
    }
}
