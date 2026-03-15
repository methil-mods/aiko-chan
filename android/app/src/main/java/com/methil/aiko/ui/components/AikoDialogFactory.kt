package com.methil.aiko.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.methil.aiko.ui.theme.DarkPurple

object AikoDialogFactory {

    @Composable
    fun InfoDialog(
        title: String,
        message: String,
        onDismiss: () -> Unit,
        confirmText: String = "OK"
    ) {
        Dialog(onDismissRequest = onDismiss) {
            AikoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                borderColor = DarkPurple
            ) {
                Text(
                    text = title.uppercase(),
                    color = DarkPurple,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                AikoButton(
                    text = confirmText,
                    onClick = onDismiss
                )
            }
        }
    }

    @Composable
    fun ConfirmDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
        confirmText: String = "CONFIRM",
        dismissText: String = "CANCEL"
    ) {
        Dialog(onDismissRequest = onDismiss) {
            AikoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                borderColor = DarkPurple
            ) {
                Text(
                    text = title.uppercase(),
                    color = DarkPurple,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        AikoButton(
                            text = dismissText,
                            onClick = onDismiss,
                            containerColor = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        AikoButton(
                            text = confirmText,
                            onClick = onConfirm
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorDialog(
        title: String = "ERROR",
        message: String,
        onDismiss: () -> Unit
    ) {
        Dialog(onDismissRequest = onDismiss) {
            AikoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                borderColor = Color.Red
            ) {
                Text(
                    text = title.uppercase(),
                    color = Color.Red,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                AikoButton(
                    text = "DISMISS",
                    onClick = onDismiss,
                    containerColor = Color.Red
                )
            }
        }
    }
}
