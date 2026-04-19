package com.streamlux.app.ui.screens.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamlux.app.ui.theme.BackgroundDark
import com.streamlux.app.ui.theme.PrimaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            item {
                Column(content = content)
            }
        }
    }
}

@Composable
fun LegalSection(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, color = PrimaryOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(body, color = Color.LightGray, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
fun PrivacyScreen(onNavigateBack: () -> Unit) {
    InfoScreen("Privacy Policy", onNavigateBack) {
        LegalSection("1. Information We Collect", "StreamLux collects minimal information necessary to provide our streaming services, including account usage, device info, and preferences.")
        LegalSection("2. How We Use Data", "Data is used to personalize your experience, provide support, and ensure security across our premium streaming network.")
        LegalSection("3. Data Security", "We implement industry-standard encryption and security protocols to safeguard your personal data and digital identity.")
    }
}

@Composable
fun TermsScreen(onNavigateBack: () -> Unit) {
    InfoScreen("Terms of Service", onNavigateBack) {
        LegalSection("1. Acceptance", "By using StreamLux, you agree to follow our service guidelines and digital community standards.")
        LegalSection("2. Proper Use", "The platform is for personal, non-commercial use. Users must not attempt to scrape or reverse-engineer our core algorithms.")
        LegalSection("3. Content Liability", "All content is provided for demonstration and testing purposes. We do not host or store any media assets on our own infrastructure.")
    }
}

@Composable
fun MissionScreen(onNavigateBack: () -> Unit) {
    InfoScreen("Our Mission", onNavigateBack) {
        LegalSection("The Vision", "StreamLux was built to bridge the gap between global media fragments—creating a single, unified discovery point for movies, sports, and music.")
        LegalSection("Our Legacy", "We believe in the power of centralized discovery. Our mission is to preserve the ease of access to digital culture for generations to come.")
        LegalSection("Elite Experience", "Everything we build is designed with an 'Elite-First' mentality, ensuring premium aesthetics and rapid performance for every user.")
    }
}

@Composable
fun SecurityScreen(onNavigateBack: () -> Unit) {
    InfoScreen("Security Protocols", onNavigateBack) {
        LegalSection("End-to-End Encryption", "Your search history and profile metadata are encrypted both in transit and at rest using military-grade standards.")
        LegalSection("Ad-Guard Technology", "Our integrated ad engine is sandboxed to prevent tracking and malicious scripts from interfering with your viewing experience.")
        LegalSection("Zero-Log Policy", "We do not sell your personal data. StreamLux operates on a transparency-first model with regular security audits.")
    }
}
