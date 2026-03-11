package com.methil.aiko.ui.screens.auth

import androidx.compose.animation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import com.methil.aiko.R
import com.methil.aiko.ui.components.AikoCustomKeyboard
import com.methil.aiko.ui.components.XpWindow
import com.methil.aiko.ui.theme.DarkPurple
import com.methil.aiko.ui.theme.LightViolet
import com.methil.aiko.ui.theme.LightestPink
import com.methil.aiko.domain.LoginRequest
import com.methil.aiko.domain.RegisterRequest
import androidx.compose.foundation.BorderStroke
import com.methil.aiko.service.AuthService
import com.methil.aiko.bridge.AikoConfig
import com.methil.aiko.data.TokenManager
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") } // Pseudo pour l'inscription
    var password by remember { mutableStateOf("") }
    var activeField by remember { mutableStateOf(0) } // 0: username, 1: password, 2: name
    var isKeyboardOpen by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val authService = remember { AuthService(AikoConfig.BASE_URL) }
    
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

        Box(modifier = Modifier.fillMaxSize()) {
            XpWindow(
                title = if (isLogin) "ログイン - Login" else "登録 - Register",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Center)
                    .offset(y = (-60).dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isLogin) "Bon retour !" else "Bienvenue !",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPurple
                    )

                    // Input Fields
                    AuthInputField(
                        label = "Username",
                        value = username,
                        isActive = activeField == 0 && isKeyboardOpen,
                        cursorAlpha = cursorAlpha,
                        onClick = { 
                            activeField = 0
                            isKeyboardOpen = true
                        }
                    )

                    if (!isLogin) {
                        AuthInputField(
                            label = "Nom / Pseudo",
                            value = name,
                            isActive = activeField == 2 && isKeyboardOpen,
                            cursorAlpha = cursorAlpha,
                            onClick = { 
                                activeField = 2
                                isKeyboardOpen = true
                            }
                        )
                    }

                    AuthInputField(
                        label = "Password",
                        value = password,
                        isActive = activeField == 1 && isKeyboardOpen,
                        cursorAlpha = cursorAlpha,
                        isPassword = true,
                        onClick = { 
                            activeField = 1
                            isKeyboardOpen = true
                        }
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Button
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) return@Button
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                if (isLogin) {
                                    val result = authService.login(LoginRequest(username, password))
                                    result.onSuccess {
                                        tokenManager.saveToken(it.token)
                                        onAuthSuccess(it.token)
                                    }.onFailure {
                                        errorMessage = "Login failed: ${it.message}"
                                    }
                                } else {
                                    val result = authService.register(RegisterRequest(username, name, password))
                                    result.onSuccess {
                                        val loginResult = authService.login(LoginRequest(username, password))
                                        loginResult.onSuccess { authRes ->
                                            tokenManager.saveToken(authRes.token)
                                            onAuthSuccess(authRes.token)
                                        }.onFailure { loginErr ->
                                            errorMessage = "Login failed: ${loginErr.message}"
                                        }
                                    }.onFailure {
                                        errorMessage = "Registration failed: ${it.message}"
                                    }
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading && username.isNotBlank() && password.isNotBlank() && (isLogin || name.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(containerColor = LightestPink),
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(2.dp, DarkPurple)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DarkPurple)
                        } else {
                            Text(
                                text = if (isLogin) "Se connecter" else "S'enregistrer",
                                color = DarkPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = if (isLogin) "Pas de compte ? S'enregistrer" else "Déjà un compte ? Se connecter",
                        modifier = Modifier.clickable { 
                            isLogin = !isLogin
                            errorMessage = null
                        },
                        fontSize = 14.sp,
                        color = DarkPurple.copy(alpha = 0.7f)
                    )
                }
            }

            // Keyboard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                AnimatedVisibility(
                    visible = isKeyboardOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    AikoCustomKeyboard(
                        onKeyClick = { key ->
                            when (activeField) {
                                0 -> username += key
                                1 -> password += key
                                2 -> name += key
                            }
                        },
                        onDelete = {
                            when (activeField) {
                                0 -> if (username.isNotEmpty()) username = username.dropLast(1)
                                1 -> if (password.isNotEmpty()) password = password.dropLast(1)
                                2 -> if (name.isNotEmpty()) name = name.dropLast(1)
                            }
                        },
                        onEnter = {
                            when (activeField) {
                                0 -> activeField = if (isLogin) 1 else 2
                                2 -> activeField = 1
                                1 -> isKeyboardOpen = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthInputField(
    label: String,
    value: String,
    isActive: Boolean,
    cursorAlpha: Float,
    isPassword: Boolean = false,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 14.sp, color = DarkPurple)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(2.dp, if (isActive) DarkPurple else LightViolet, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayText = if (isPassword) "•".repeat(value.length) else value
                Text(
                    text = displayText,
                    color = DarkPurple,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(width = 2.dp, height = 20.dp)
                            .background(DarkPurple)
                            .graphicsLayer { alpha = cursorAlpha }
                    )
                }
            }
        }
    }
}
