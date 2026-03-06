package com.methil.aiko.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.methil.aiko.ui.theme.DarkPurple
import com.methil.aiko.ui.theme.LightViolet
import com.methil.aiko.ui.theme.LightestPink

@Composable
fun XpWindow(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shadowColor = DarkPurple
    val shadowOffset = 4.dp

    Box(modifier = modifier) {
        // Hard shadow (solid offset rectangle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor)
        )

        Surface(
            modifier = Modifier
                .background(LightestPink)
                .border(BorderStroke(3.dp, LightViolet)),
            color = Color.White
        ) {
            Column {
                // Title Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(LightViolet)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        color = DarkPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Min/Close icons
                        AsyncImage(
                            model = "http://localhost:3845/assets/ddaaf6a99f97e2bbf4739b5c2746a49dbdd8636c.svg",
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        AsyncImage(
                            model = "http://localhost:3845/assets/7c85398c646e1d8c43dece198ea8c2f864130d4f.svg",
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // Content
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }
}