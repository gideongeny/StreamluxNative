package com.streamlux.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamlux.app.ui.components.BannerAd
import com.streamlux.app.ui.components.GeniusAIChatbot
import com.streamlux.app.ui.screens.detail.MediaDetailScreen
import com.streamlux.app.ui.screens.explore.ExploreScreen
import com.streamlux.app.ui.screens.home.HomeScreen
import com.streamlux.app.ui.screens.profile.ProfileScreen
import com.streamlux.app.ui.screens.sports.SportsScreen

enum class Screen(val route: String) {
    Home("home"),
    Sports("sports"),
    LiveTv("live_tv"),
    Music("music"),
    Auth("auth"),
    Search("search"),
    Copyright("copyright"),
    Disclaimer("disclaimer"),
    Profile("profile"),
    Library("library?tab={tab}")
}

@Composable
fun StreamLuxApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide chatbot & banner on full-screen player screen
    val isPlayerScreen = currentRoute?.startsWith("player") == true

    Scaffold(
        bottomBar = {
            if (!isPlayerScreen) {
                Column {
                    BannerAd()
                    MobileBottomNav(navController = navController)
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToDetail = { id, type -> navController.navigate("detail/$type/$id") },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                    )
                }

                composable(Screen.Sports.route) {
                    SportsScreen(
                        onNavigateToPlayer = { type, id -> navController.navigate("player/$type/$id") },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                    )
                }

                composable(Screen.LiveTv.route) { 
                    ExploreScreen(
                        onNavigateToPlayer = { url, name ->
                            navController.navigate("player/live/${java.net.URLEncoder.encode(url, "UTF-8")}?season=1&episode=1")
                        },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                    ) 
                }
                composable(Screen.Music.route) { 
                    com.streamlux.app.ui.screens.music.MusicScreen(
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                    ) 
                }

                composable("profile") {
                    ProfileScreen(
                        onNavigateToLegal = { route -> navController.navigate(route) },
                        onNavigateToLibrary = { tab -> navController.navigate("library?tab=$tab") }
                    )
                }

                composable(
                    route = "library?tab={tab}",
                    arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "Watchlist" })
                ) { backStackEntry ->
                    val tab = backStackEntry.arguments?.getString("tab") ?: "Watchlist"
                    com.streamlux.app.ui.screens.library.LibraryScreen(
                        onNavigateToDetail = { id, type -> navController.navigate("detail/$type/$id") },
                        initialTab = tab
                    )
                }

                composable(Screen.Auth.route) {
                    com.streamlux.app.ui.screens.auth.AuthScreen(
                        onAuthSuccess = { navController.popBackStack() }
                    )
                }

                composable(Screen.Search.route) {
                    com.streamlux.app.ui.screens.search.SearchScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDetail = { id, type -> navController.navigate("detail/$type/$id") }
                    )
                }

                composable(Screen.Copyright.route) {
                    com.streamlux.app.ui.screens.legal.CopyrightScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("ownership") {
                    com.streamlux.app.ui.screens.legal.CopyrightScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Disclaimer.route) {
                    com.streamlux.app.ui.screens.legal.DisclaimerScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("mission") {
                    com.streamlux.app.ui.screens.legal.MissionScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("security") {
                    com.streamlux.app.ui.screens.legal.SecurityScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("privacy") {
                    com.streamlux.app.ui.screens.legal.PrivacyScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("terms") {
                    com.streamlux.app.ui.screens.legal.TermsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("detail/{type}/{id}") {
                    MediaDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = { route ->
                            navController.navigate(route)
                        },
                        onNavigateToDetail = { id, type -> navController.navigate("detail/$type/$id") },
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) }
                    )
                }

                composable(
                    route = "player/{type}/{id}?season={season}&episode={episode}&title={title}&poster={poster}",
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("id") { type = NavType.StringType },
                        navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                        navArgument("episode") { type = NavType.IntType; defaultValue = 1 },
                        navArgument("title") { type = NavType.StringType; defaultValue = "Unknown" },
                        navArgument("poster") { type = NavType.StringType; defaultValue = "" }
                    )
                ) {
                    com.streamlux.app.ui.screens.player.VideoPlayerScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            // Global AI Chatbot (controlled by shared GeniusAIViewModel)

        }
    }
}
