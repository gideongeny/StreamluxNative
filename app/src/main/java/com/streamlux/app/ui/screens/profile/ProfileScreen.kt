package com.streamlux.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.streamlux.app.ui.theme.BackgroundDark
import com.streamlux.app.ui.theme.PrimaryOrange

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLegal: (String) -> Unit,
    onNavigateToLibrary: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val isNightMode by viewModel.isNightMode.collectAsState()
    val isAutoplay by viewModel.isAutoplay.collectAsState()
    val isBackgroundAudio by viewModel.isBackgroundAudio.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("PROFILE CENTER", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = { /* Settings popover */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Elite Header
            item {
                val displayName = user?.displayName ?: user?.email?.split("@")?.firstOrNull() ?: "Elite Member"
                EliteProfileHeader(
                    name = displayName,
                    email = user?.email ?: "Guest Access",
                    photoUrl = user?.photoUrl?.toString(),
                    id = user?.uid ?: "GUEST-ID"
                )
            }

            // Membership Section
            item {
                MembershipCard()
            }

            // Settings Section
            item {
                ProfileSectionTitle("Preferences")
                SettingsToggleRow("Night Mode", Icons.Default.NightsStay, isNightMode) {
                    viewModel.toggleNightMode(it)
                }
                SettingsToggleRow("Auto-Play Trailers", Icons.Default.PlayCircle, isAutoplay) {
                    viewModel.toggleAutoplay(it)
                }
                SettingsToggleRow("Background Playback", Icons.Default.GraphicEq, isBackgroundAudio) {
                    viewModel.toggleBackgroundAudio(it)
                }
            }

            // Web Promotion Section
            item {
                ProfileSectionTitle("Premium Web Experience")
                WebPortalCard(
                    onOpenUrl = { url ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.findActivity()?.startActivity(intent)
                    }
                )
            }


            // Legal & Info
            item {
                ProfileSectionTitle("Legacy & Legal")
                ProfileMenuRow("Our Mission", Icons.Default.Public, onClick = { onNavigateToLegal("mission") })
                ProfileMenuRow("Security & Privacy", Icons.Default.Security, onClick = { onNavigateToLegal("security") })
                ProfileMenuRow("Terms of Service", Icons.Default.Description, onClick = { onNavigateToLegal("terms") })
                ProfileMenuRow("Copyright Center", Icons.Default.Copyright, onClick = { onNavigateToLegal("copyright") })
                ProfileMenuRow("Website Ownership", Icons.Default.Info, onClick = { onNavigateToLegal("copyright") })
            }

            // Auth Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                if (user != null) {
                    Button(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = { viewModel.signInWithGoogle(context) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Sign In Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EliteProfileHeader(name: String, email: String, photoUrl: String?, id: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                .border(2.dp, PrimaryOrange, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(50.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
        
        Text(
            text = email,
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            color = PrimaryOrange.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "ID: ${id.take(12).uppercase()}",
                color = PrimaryOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun MembershipCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("STREAMLUX PREMIER", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Active Elite Status", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = PrimaryOrange,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun SettingsToggleRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryOrange, checkedTrackColor = PrimaryOrange.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun ProfileMenuRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun WebPortalCard(onOpenUrl: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Public, contentDescription = null, tint = PrimaryOrange)
                Spacer(modifier = Modifier.width(12.dp))
                Text("UNRESTRICTED ACCESS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Access 100% of our movie library and remove all TV series limits by visiting our web portals. Perfect for the ultimate desktop viewing experience.",
                color = Color.Gray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = { onOpenUrl("https://streamlux-67a84.web.app/") },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Mirror 1 (Firebase Hosting)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { onOpenUrl("https://streamlux.vercel.app/") },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Mirror 2 (Vercel Serverless)", color = PrimaryOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

