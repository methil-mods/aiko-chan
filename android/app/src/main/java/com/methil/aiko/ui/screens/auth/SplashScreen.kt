package com.methil.aiko.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.methil.aiko.R
import com.methil.aiko.ui.theme.LightViolet
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Animation stages
    // Stage 1: Initial scale 0.6 -> 0.75 (0-800ms)
    // Stage 2: Hold at 0.75 (800-1200ms)
    // Stage 3: Slide down and scale to 0.3 (1200-2000ms)
    // Stage 4: Show destructed logo with bounce (2000-3200ms)
    // Stage 5: Dither slide transition (3200-4000ms)
    
    var stage by remember { mutableStateOf(1) }
    
    // Initial scale animation (0.6 -> 0.75)
    val initialScale by animateFloatAsState(
        targetValue = if (stage >= 1) 0.75f else 0.6f,
        animationSpec = tween(durationMillis = 800, easing = EaseIn),
        label = "InitialScale"
    )
    
    // Calculate screen height for proper positioning
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    val logoSizeDp = 200 * 0.3f // 30% of 200dp = 60dp
    // Calculate offset from center to reach 12% margin from bottom
    val centerToBottomDistance = (screenHeightDp / 2f) - logoSizeDp
    val marginFromBottom = screenHeightDp * 0.12f
    val targetOffsetYDp = centerToBottomDistance - marginFromBottom
    val targetOffsetY = targetOffsetYDp.dp
    
    // Slide down offset (starts at stage 2)
    val slideOffsetY by animateDpAsState(
        targetValue = when {
            stage >= 3 -> targetOffsetY
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = 800, easing = EaseIn),
        label = "SlideOffsetY"
    )
    
    // Scale down during slide (0.75 -> 0.3)
    val slideScale by animateFloatAsState(
        targetValue = when {
            stage >= 3 -> 0.3f
            else -> initialScale
        },
        animationSpec = tween(durationMillis = 800, easing = EaseIn),
        label = "SlideScale"
    )
    
    // Destructed logo bounce scale
    val destructedScale by animateFloatAsState(
        targetValue = when (stage) {
            4 -> 1f
            else -> 0.5f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DestructedScale"
    )
    
    // Dither background slide animation
    var showDither by remember { mutableStateOf(false) }
    val ditherAlpha by animateFloatAsState(
        targetValue = if (showDither) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseIn),
        label = "DitherAlpha"
    )

    LaunchedEffect(Unit) {
        stage = 1
        delay(1200) // Wait 1.2s before sliding
        stage = 2
        delay(100) // Brief pause
        stage = 3 // Start slide down
        delay(800) // Wait for slide to complete
        stage = 4 // Show destructed logo with bounce
        delay(2000) // Display destructed logo
        stage = 5 // Start dither transition
        showDither = true
        delay(800) // Wait for dither to slide
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightViolet)
    ) {
        // Dither background sliding from bottom
        AnimatedVisibility(
            visible = showDither,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 800, easing = EaseIn)
            )
        ) {
            Image(
                painter = painterResource(R.drawable.dither_bg_dark),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = ditherAlpha }
            )
        }
        
        // Destructed Logo with bounce animation at center (appears after slide)
        if (stage >= 4) {
            Image(
                painter = painterResource(R.drawable.aikochan_logo_destructed),
                contentDescription = "Aiko-chan Logo",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(250.dp)
                    .scale(destructedScale)
            )
        }
        
        // Methil Logo - slides down from center and stays visible at bottom
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = slideOffsetY)
        ) {
            Image(
                painter = painterResource(R.drawable.logo_methil_jap),
                contentDescription = "Methil Logo",
                modifier = Modifier
                    .size(200.dp)
                    .scale(slideScale)
            )
        }
    }
}
