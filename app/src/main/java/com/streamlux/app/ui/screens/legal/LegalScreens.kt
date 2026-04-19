package com.streamlux.app.ui.screens.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamlux.app.ui.theme.BackgroundDark
import com.streamlux.app.ui.theme.PrimaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyrightScreen(onNavigateBack: () -> Unit) {
    LegalBaseScreen(title = "Copyright & Legal", onNavigateBack = onNavigateBack) {
        LegalSection(title = "Copyright Notice") {
            Text(
                "© 2024 StreamLux. All rights reserved.\n\n" +
                "StreamLux (\"we\", \"us\", or \"our\") is a streaming platform that provides " +
                "access to movies, TV shows, and sports content. All content displayed " +
                "on this website is the property of their respective owners.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        LegalSection(title = "Website Ownership") {
            Text(
                "StreamLux is owned and operated by Gideon Cheruiyot (Gideon Geny) - Multi-Award Winning Tech Innovator.\n\n" +
                "Contact Information:\n" +
                "• Email: gideongeng@gmail.com\n" +
                "• Facebook: facebook.com/gideo.cheruiyot.2025\n" +
                "• Instagram: instagram.com/gideo.cheruiyo\n\n" +
                "Support StreamLux: If you encounter issues, please contact the developer directly.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        LegalSection(title = "Content Disclaimer") {
            Text(
                "StreamLux does not host, store, or distribute any video content. " +
                "We provide links to external streaming sources and aggregators. " +
                "All movies, TV shows, and sports content are the property of their " +
                "respective copyright holders.\n\n" +
                "We respect intellectual property rights and comply with applicable " +
                "copyright laws. If you believe that any content on our website " +
                "infringes your copyright, please contact us immediately.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclaimerScreen(onNavigateBack: () -> Unit) {
    LegalBaseScreen(title = "Disclaimer", onNavigateBack = onNavigateBack) {
        Surface(
            color = Color.Red.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Important Notice", color = Color.Red, fontWeight = FontWeight.Bold)
                Text(
                    "All content on this page is provided for testing and demonstration purposes only.",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        LegalSection(title = "Content Disclaimer") {
            Text(
                "StreamLux does not host, store, or upload any video content, movies, TV shows, or " +
                "sports streams. All video streams, images, and texts displayed on this website are " +
                "sourced from publicly available websites and third-party sources.\n\n" +
                "We do not claim ownership of any content displayed on our platform. All copyrights " +
                "and intellectual property rights belong to their respective owners.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalBaseScreen(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            content()
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun LegalSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            color = PrimaryOrange,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}
