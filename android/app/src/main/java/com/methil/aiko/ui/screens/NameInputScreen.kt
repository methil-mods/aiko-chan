package com.methil.aiko.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.methil.aiko.R
import com.methil.aiko.ui.components.AikoCustomKeyboard
import com.methil.aiko.ui.components.XpWindow
import com.methil.aiko.ui.theme.DarkPurple
import com.methil.aiko.ui.theme.LightViolet
import com.methil.aiko.ui.theme.LightestPink

@Composable
fun NameInputScreen(
    onNameSubmitted: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isKeyboardOpen by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "CursorTransition")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.7f at 500
                1f at 501
                1f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "CursorAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightViolet)
    ) {
        // Chat Background
        Image(
            painter = painterResource(id = R.drawable.chat_bg),
            contentDescription = "background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Name Input Dialog Window (Centered)
            XpWindow(
                title = "名前を入力", // Enter name
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Center)
                    // Offset slightly up so it's not hidden by the keyboard
                    .offset(y = (-40).dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Quel est ton prénom ?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPurple
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(2.dp, LightViolet, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isKeyboardOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (name.isEmpty() && !isKeyboardOpen) "Ton prénom..." else name,
                                color = if (name.isEmpty() && !isKeyboardOpen) Color.Gray else DarkPurple,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isKeyboardOpen) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 2.dp)
                                        .size(width = 2.dp, height = 24.dp)
                                        .background(DarkPurple)
                                        .graphicsLayer { alpha = cursorAlpha }
                                )
                            }
                        }
                    }

                    // Confirm Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable(
                                enabled = name.isNotBlank(),
                                onClick = { onNameSubmitted(name.trim()) }
                            ),
                        color = if (name.isNotBlank()) LightestPink else Color.LightGray,
                        border = BorderStroke(2.dp, if (name.isNotBlank()) Color.Black else Color.Gray)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Continuer",
                                color = if (name.isNotBlank()) DarkPurple else Color.DarkGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Custom Keyboard Layer (Bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                AnimatedVisibility(
                    visible = isKeyboardOpen,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 300)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 300)
                    )
                ) {
                    AikoCustomKeyboard(
                        onKeyClick = { key ->
                            name += key
                        },
                        onDelete = {
                            if (name.isNotEmpty()) {
                                name = name.dropLast(1)
                            }
                        },
                        onEnter = {
                            if (name.isNotBlank()) {
                                onNameSubmitted(name.trim())
                            } else {
                                isKeyboardOpen = false
                            }
                        }
                    )
                }
            }
        }
    }
}
