package com.streamlux.app.ui.screens.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.streamlux.app.data.model.SportsFixture
import com.streamlux.app.data.model.SportsHighlight
import com.streamlux.app.data.model.TVChannel
import com.streamlux.app.ui.theme.PrimaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsScreen(
    viewModel: SportsViewModel = hiltViewModel(),
    onNavigateToPlayer: (String, String) -> Unit,
    onNavigateToChannel: (TVChannel) -> Unit = {},
    onNavigateToProfile: () -> Unit
) {
    val liveMatches by viewModel.liveMatches.collectAsState()
    val upcomingMatches by viewModel.upcomingMatches.collectAsState()
    val finishedMatches by viewModel.finishedMatches.collectAsState()
    val highlights by viewModel.highlights.collectAsState()
    val sportChannels by viewModel.sportChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val uriHandler = LocalUriHandler.current

    fun openArena(fixture: SportsFixture) {
        onNavigateToPlayer("sports", fixture.id)
    }

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
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        if (currentUser?.photoUrl != null) {
                            AsyncImage(
                                model = currentUser.photoUrl.toString(),
                                contentDescription = "Profile",
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(6.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading && liveMatches.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = PrimaryOrange)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp, top = if (isLoading) 8.dp else 0.dp)
            ) {
                if (sportChannels.isNotEmpty()) {
                    item {
                        SportsSectionHeader("Explore Channels", icon = Icons.Default.LiveTv, subtitle = "LIVE 24/7")
                        SportChannelsRow(channels = sportChannels, onChannelClick = onNavigateToChannel)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (liveMatches.isNotEmpty()) {
                    item { SportsSectionHeader("Live Matches", icon = Icons.Default.EmojiEvents, color = Color.Red) }
                    items(liveMatches.chunked(2)) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { fixture ->
                                PremiumSportsMatchCard(
                                    fixture = fixture,
                                    modifier = Modifier.weight(1f),
                                    onClick = { openArena(fixture) }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                if (highlights.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SportsSectionHeader("Video Highlights", icon = Icons.Default.Star)
                    }
                    items(highlights) { highlight ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            HighlightCard(highlight) {
                                viewModel.getHighlightUrl(highlight)?.let { url ->
                                    onNavigateToPlayer("sports", java.net.URLEncoder.encode(url, "UTF-8"))
                                }
                            }
                        }
                    }
                }

                if (upcomingMatches.isNotEmpty()) {
                    item { SportsSectionHeader("Upcoming Fixtures", icon = Icons.Default.Schedule) }
                    items(upcomingMatches.chunked(2)) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { fixture ->
                                PremiumSportsMatchCard(
                                    fixture = fixture,
                                    modifier = Modifier.weight(1f),
                                    onClick = { openArena(fixture) }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                if (finishedMatches.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SportsSectionHeader("Completed", icon = Icons.Default.CheckCircle, color = Color.Gray)
                    }
                    items(finishedMatches.take(6).chunked(2)) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { fixture ->
                                PremiumSportsMatchCard(
                                    fixture = fixture,
                                    modifier = Modifier.weight(1f),
                                    onClick = { openArena(fixture) }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SportsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = PrimaryOrange,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title.uppercase(), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 16.sp)
        subtitle?.let {
            Spacer(Modifier.width(8.dp))
            Surface(color = PrimaryOrange.copy(alpha = 0.15f), shape = RoundedCornerShape(999.dp)) {
                Text(it, color = PrimaryOrange, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun SportChannelsRow(channels: List<TVChannel>, onChannelClick: (TVChannel) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        channels.forEach { channel ->
            val logoUrl = remember(channel.logo) {
                when {
                    channel.logo.isNullOrBlank() -> null
                    channel.logo.startsWith("http") -> channel.logo
                    else -> "https://streamlux-67a84.web.app/assets/logos/${channel.logo}.png"
                }
            }
            Column(
                modifier = Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(teamColor(channel.name).copy(alpha = 0.25f))
                    .clickable { onChannelClick(channel) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("📺", fontSize = 28.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(channel.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun PremiumSportsMatchCard(fixture: SportsFixture, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val homeColor = teamColor(fixture.homeTeam)
    val awayColor = teamColor(fixture.awayTeam)
    val sportLabel = (fixture.sport ?: fixture.leagueName).replaceFirstChar { it.uppercase() }

    Box(
        modifier = modifier
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(homeColor.copy(alpha = 0.85f), awayColor.copy(alpha = 0.85f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.75f))))
        )

        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(8.dp)) {
                    Text(sportLabel, color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                if (fixture.isLive) {
                    Surface(color = Color(0xFFDC2626), shape = RoundedCornerShape(8.dp)) {
                        Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (fixture.isVsMatch) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamBadgeColumn(fixture.homeTeam, fixture.homeTeamLogo, fixture.homeScore)
                    Text("VS", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    TeamBadgeColumn(fixture.awayTeam, fixture.awayTeamLogo, fixture.awayScore)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TeamBadge(fixture.homeTeamLogo, fixture.homeTeam, 52.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        fixture.homeTeam,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${fixture.displayStreamCount.coerceAtLeast(1)} streams",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("Watch →", color = Color(0xFF60A5FA), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun TeamBadgeColumn(name: String, logo: String?, score: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 72.dp)) {
        TeamBadge(logo, name, 44.dp)
        Spacer(Modifier.height(4.dp))
        Text(name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        score?.let {
            Text(it, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun TeamBadge(logo: String?, name: String, size: androidx.compose.ui.unit.Dp) {
    val model = logo?.takeIf { it.isNotBlank() }
        ?: "https://ui-avatars.com/api/?name=${java.net.URLEncoder.encode(name, "UTF-8")}&background=random&size=128"
    AsyncImage(
        model = model,
        contentDescription = name,
        modifier = Modifier.size(size).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).padding(6.dp),
        contentScale = ContentScale.Fit
    )
}

private fun teamColor(name: String): Color {
    val hash = name.fold(0) { acc, c -> acc + c.code }
    val palette = listOf(
        Color(0xFF2563EB), Color(0xFFDC2626), Color(0xFF16A34A),
        Color(0xFF9333EA), Color(0xFFEA580C), Color(0xFF0891B2)
    )
    return palette[hash % palette.size]
}

@Composable
fun HighlightCard(highlight: SportsHighlight, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Box(modifier = Modifier.height(130.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
            AsyncImage(model = highlight.thumbnail, contentDescription = highlight.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp).align(Alignment.Center))
        }
        Spacer(Modifier.height(8.dp))
        Text(highlight.title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
        Text(highlight.competition, color = Color.Gray, fontSize = 12.sp)
    }
}
