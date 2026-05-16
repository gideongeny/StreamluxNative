package com.streamlux.app.ui.screens.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
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
    LegalBaseScreen(title = "Copyright & Compliance", onNavigateBack = onNavigateBack) {
        LegalSection(title = "Legal Framework (Kenya)") {
            Text(
                "StreamLux operates in strict adherence to the Copyright and Related Rights Act (Kenya). " +
                "We are committed to the protection of intellectual property in accordance with " +
                "Articles 11(2)(c) and 40(5) of the Constitution of Kenya.\n\n" +
                "As an Online Intermediary (Section 81), we act as a neutral metadata portal. " +
                "We do not host or modify the content provided by third-party aggregators.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        LegalSection(title = "Takedown Policy (Section 82)") {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Text(
                "In compliance with Section 82 of the Copyright Bill, we provide a structured " +
                "mechanism for rights holders to report infringement.\n\n" +
                "Upon receipt of a valid notice, we will disable access to infringing links " +
                "within 48 hours.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { uriHandler.openUri("https://streamlux-67a84.web.app/copyright#takedown") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("REPORT CONTENT / TAKEDOWN", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Alternatively, you may contact dmca@streamlux.app with the required documentation.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        LegalSection(title = "Repeat Infringer Policy") {
            Text(
                "Pursuant to Section 81(2)(e), StreamLux maintains a strict Repeat Infringer Policy. " +
                "Sources found to be consistently providing infringing links will be permanently " +
                "banned from our indexing service.",
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
