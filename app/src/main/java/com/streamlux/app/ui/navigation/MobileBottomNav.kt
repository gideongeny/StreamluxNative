package com.streamlux.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.streamlux.app.ui.theme.PrimaryOrange

@Composable
fun MobileBottomNav(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    // Floating capsule structure mimicking StreamLux PWA
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp), // Hover above bottom
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) // Theme-aware glassmorphism
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isActive = currentRoute == Screen.Home.route,
                onClick = { navController.navigate(Screen.Home.route) }
            )
            
            NavItem(
                icon = Icons.Default.SportsSoccer,
                label = "Sports",
                isActive = currentRoute == Screen.Sports.route,
                onClick = { navController.navigate(Screen.Sports.route) }
            )

            NavItem(
                icon = Icons.Default.LiveTv,
                label = "Live TV",
                isActive = currentRoute == Screen.LiveTv.route,
                onClick = { navController.navigate(Screen.LiveTv.route) }
            )
            
            NavItem(
                icon = Icons.Default.LibraryBooks,
                label = "Library",
                isActive = currentRoute?.startsWith("library") == true,
                onClick = { navController.navigate(Screen.Library.route) }
            )
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (isActive) PrimaryOrange else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        if (isActive) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                color = color,
                fontSize = 9.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
