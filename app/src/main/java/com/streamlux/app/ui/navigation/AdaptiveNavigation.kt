package com.streamlux.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SportsSoccer
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
fun StreamLuxNavigation(
    navController: NavController,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (isExpanded) {
        Row(modifier = modifier.fillMaxSize()) {
            StreamLuxNavigationRail(navController = navController)
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            // Bottom bar is handled in Scaffold in NavGraph for compact screens
        }
    }
}

@Composable
fun StreamLuxNavigationRail(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = PrimaryOrange,
        header = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp)) {
                Text("SL", color = PrimaryOrange, style = MaterialTheme.typography.titleLarge)
            }
        },
        modifier = Modifier.fillMaxHeight().width(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RailItem(
                icon = Icons.Default.Home,
                label = "Home",
                isActive = currentRoute == Screen.Home.route,
                onClick = { navController.navigate(Screen.Home.route) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            RailItem(
                icon = Icons.Default.SportsSoccer,
                label = "Sports",
                isActive = currentRoute == Screen.Sports.route,
                onClick = { navController.navigate(Screen.Sports.route) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            RailItem(
                icon = Icons.Default.LiveTv,
                label = "Live",
                isActive = currentRoute == Screen.LiveTv.route,
                onClick = { navController.navigate(Screen.LiveTv.route) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            RailItem(
                icon = Icons.Default.LibraryBooks,
                label = "Library",
                isActive = currentRoute?.startsWith("library") == true,
                onClick = { navController.navigate(Screen.Library.route) }
            )
        }
    }
}

@Composable
fun RailItem(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    val color = if (isActive) PrimaryOrange else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}
