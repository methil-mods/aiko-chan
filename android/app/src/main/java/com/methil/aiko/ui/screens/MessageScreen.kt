package com.methil.aiko.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.methil.aiko.R
import com.methil.aiko.ui.components.AikoCustomKeyboard
import com.methil.aiko.ui.components.XpWindow
import com.methil.aiko.ui.theme.*
import kotlinx.coroutines.launch

data class Message(val text: String, val isAiko: Boolean)

@Preview
@Composable
fun MessageScreen() {
    var messages by remember { mutableStateOf(listOf(
        Message("Hello there! I'm Aiko.", true),
        Message("How are you feeling today?", true)
    )) }
    
    var inputText by remember { mutableStateOf("") }
    var isKeyboardOpen by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightViolet)
    ) {
        // Chat Background
        AsyncImage(
            model = "http://localhost:3845/assets/9cf805d5a475c54a37b53c3a60a464a10f334e3e.png", // chat-bg.png
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Control Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Window: Mood/Profile
                XpWindow(title = "Mood", modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = "http://localhost:3845/assets/cdc6774281aa12d377d809fad68a2d24ef627297.png", // e-girl-emo.png
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Right Window: Stats
                XpWindow(title = "Stats", modifier = Modifier.weight(1.5f)) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatRow("LUV", "http://localhost:3845/assets/ae3b83a6522aeef54dbf03912059a8181a1e451919.svg") // heart_fill
                        StatRow("NRG", "http://localhost:3845/assets/7c85398c646e1d8c43dece198ea8c2f864130d4f.svg") // spark_fill
                    }
                }
            }

            // Chat Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }

            // Input Area
            Y2kInputArea(
                text = inputText,
                onInputClick = { isKeyboardOpen = !isKeyboardOpen },
                onSend = {
                    if (inputText.isNotBlank()) {
                        messages = messages + Message(inputText, false)
                        inputText = ""
                        isKeyboardOpen = false
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            )

            // Custom Keyboard
            if (isKeyboardOpen) {
                AikoCustomKeyboard(
                    onKeyClick = { inputText += it },
                    onDelete = { if (inputText.isNotEmpty()) inputText = inputText.dropLast(1) }
                )
            }
        }
    }
}

@Composable
fun StatRow(label: String, iconUrl: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = DarkPurple, modifier = Modifier.width(35.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val shadowColor = DarkPurple
    val shadowOffset = 4.dp

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isAiko) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        // Hard shadow for chat bubble - positioned behind the content
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor)
        )

        // Main bubble content
        Surface(
            modifier = Modifier
                .widthIn(max = 260.dp),
            color = if (message.isAiko) LightestPink else Color.White,
            border = BorderStroke(2.dp, LightViolet),
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.e_girl_pp),
                    contentDescription = null,
                    modifier = Modifier
                        .height(32.dp)
                        .aspectRatio(1F),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.text,
                    color = DarkPurple,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun Y2kInputArea(
    text: String,
    onInputClick: () -> Unit,
    onSend: () -> Unit
) {
    val shadowColor = DarkPurple
    val shadowOffset = 4.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp)
    ) {
        // Hard shadow for input area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor)
        )

        Surface(
            modifier = Modifier
                .fillMaxSize(),
            border = BorderStroke(3.dp, LightViolet),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onInputClick() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (text.isEmpty()) "Discuter..." else text,
                        color = if (text.isEmpty()) Color.Gray else DarkPurple,
                        fontSize = 16.sp
                    )
                }
                IconButton(onClick = onSend) {
                    AsyncImage(
                        model = "http://localhost:3845/assets/a979123e7798b8e8dff7fde7f48ee58a2f3db2f9.svg",
                        contentDescription = "Send",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}