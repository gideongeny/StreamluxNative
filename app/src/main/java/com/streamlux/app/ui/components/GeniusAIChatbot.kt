package com.streamlux.app.ui.components

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamlux.app.ui.theme.PrimaryOrange
import com.streamlux.app.utils.Constants
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeniusAIChatbot(
    modifier: Modifier = Modifier,
    viewModel: GeniusAIViewModel = hiltViewModel()
) {
    val isOpen by viewModel.isOpen.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Pulse animation for FAB
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseScale"
    )

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun sendMessage() {
        val userText = input.trim()
        if (userText.isEmpty() || isTyping) return
        input = ""
        focusManager.clearFocus()
        viewModel.sendMessage(userText)
    }

    Box(modifier = modifier) {
        // Chat panel
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .height(480.dp)
                    .padding(bottom = 80.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PrimaryOrange.copy(alpha = 0.2f), Color.Transparent)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Robot icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Genius AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Online · StreamLux AI", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.setIsOpen(false) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.05f))

                    // Messages
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages) { msg ->
                            val isUser = msg.role == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 240.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp, topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isUser) PrimaryOrange
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        color = if (isUser) Color.White else Color(0xFFE5E7EB),
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        if (isTyping) {
                            item {
                                Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(0.08f))
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Text("•••", color = Color.Gray, fontSize = 18.sp, letterSpacing = 3.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Input
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141414))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask me anything…", color = Color.Gray, fontSize = 12.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(0.06f),
                                unfocusedContainerColor = Color.White.copy(0.06f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendMessage() })
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { sendMessage() },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (input.isNotBlank()) PrimaryOrange else Color.White.copy(0.1f))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // FAB Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size((64 * pulseScale).dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(PrimaryOrange, Color(0xFF3B82F6)))
                )
                .clickable { viewModel.setIsOpen(!isOpen) },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(targetState = isOpen, label = "fab_icon") { open ->
                if (open) {
                    Text("✕", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("✨", fontSize = 28.sp)
                }
            }
        }
    }
}
