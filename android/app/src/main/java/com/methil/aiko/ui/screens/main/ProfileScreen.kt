package com.methil.aiko.ui.screens.main

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.methil.aiko.ui.components.*
import com.methil.aiko.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    sessionToken: String,
    onLogout: () -> Unit = {}
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Edit states
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authService = remember { AuthService(AikoConfig.BASE_URL) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        isLoading = true
                        val fileName = "avatar_${System.currentTimeMillis()}.jpg"
                        authService.uploadAvatar(sessionToken, fileName, bytes).onSuccess { newUrl ->
                            profile = profile?.copy(avatar_url = newUrl)
                            isLoading = false
                        }.onFailure { err ->
                            errorMessage = "Upload failed: ${err.message}"
                            isLoading = false
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "Error reading file: ${e.message}"
                }
            }
        }
    }

    LaunchedEffect(sessionToken) {
        authService.getProfile(sessionToken).onSuccess {
            profile = it
            editName = it.name
            editAge = it.age.toString()
            isLoading = false
        }.onFailure {
            Log.e("ProfileScreen", "Failed to fetch profile", it)
            errorMessage = it.message
            isLoading = false
        }
    }

    fun handleUpdate() {
        val ageInt = editAge.toIntOrNull() ?: 20
        scope.launch {
            isLoading = true
            authService.updateProfile(sessionToken, editName, ageInt).onSuccess { updated ->
                profile = updated
                isEditing = false
                isLoading = false
            }.onFailure { err ->
                errorMessage = "Update failed: ${err.message}"
                isLoading = false
            }
        }
    }

    AikoBackground {
        if (isLoading && profile == null) {
            CircularProgressIndicator(color = DarkPurple)
        } else {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Card with Square styling
                AikoCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = DarkPurple
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Avatar area
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color.Black)
                                .border(2.dp, DarkPurple)
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile?.avatar_url != null) {
                                val url = if (profile!!.avatar_url!!.startsWith("http")) {
                                    profile!!.avatar_url!!
                                } else {
                                    "${AikoConfig.BASE_URL}${profile!!.avatar_url}"
                                }
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // Overlay indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = "EDIT",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isEditing) {
                            AikoTextField(label = "NAME", value = editName, onValueChange = { editName = it })
                            Spacer(modifier = Modifier.height(16.dp))
                            AikoTextField(label = "AGE", value = editAge, onValueChange = { editAge = it })
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            AikoButton(text = "SAVE", onClick = { handleUpdate() })
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { isEditing = false }) {
                                Text("CANCEL", color = DarkPurple, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            AikoHeader(
                                text = profile?.username?.uppercase() ?: "",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${profile?.name}, ${profile?.age} Yrs",
                                color = DarkPurple.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            AikoButton(text = "EDIT PROFILE", onClick = { isEditing = true })
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Logout Button
                            AikoButton(
                                text = "LOGOUT",
                                onClick = onLogout,
                                containerColor = Color(0xFFE91E63)
                            )
                        }
                    }
                }
            }
        }
    }

        // Error message overlay
        errorMessage?.let { error ->
            Snackbar(
                action = {
                    TextButton(onClick = { errorMessage = null }) {
                        Text("DISMISS", color = Color.White)
                    }
                },
                modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter),
                containerColor = Color.Red,
                shape = RectangleShape
            ) {
                Text(error)
            }
        }
    }
}

