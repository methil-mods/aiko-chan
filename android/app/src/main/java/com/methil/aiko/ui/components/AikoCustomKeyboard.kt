package com.methil.aiko.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.methil.aiko.ui.theme.DarkPurple
import com.methil.aiko.ui.theme.LightViolet
import com.methil.aiko.ui.theme.LightestPink

@Composable
fun AikoCustomKeyboard(
    onKeyClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    val keys = listOf(
        "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
        "A", "S", "D", "F", "G", "H", "J", "K", "L",
        "Z", "X", "C", "V", "B", "N", "M", "⌫"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        color = Color(0xFFD0D0D0), // Typical keyboard gray
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 35.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(keys) { key ->
                    KeyboardKey(
                        text = key,
                        onClick = {
                            if (key == "⌫") onDelete() else onKeyClick(key)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardKey(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(45.dp)
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(4.dp),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
