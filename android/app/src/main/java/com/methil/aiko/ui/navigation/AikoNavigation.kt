package com.methil.aiko.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.methil.aiko.ui.screens.MessageScreen
import com.methil.aiko.ui.screens.OnboardingScreen
import com.methil.aiko.ui.screens.SplashScreen

@Composable
fun AikoNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen(onSplashFinished = {
                navController.navigate("onboarding") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("onboarding") {
            OnboardingScreen(onStartClick = {
                navController.navigate("message") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("message") {
            MessageScreen()
        }
    }
}
