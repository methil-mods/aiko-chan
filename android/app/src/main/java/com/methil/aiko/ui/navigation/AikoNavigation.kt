package com.methil.aiko.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.methil.aiko.ui.screens.AuthScreen
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
                navController.navigate("auth") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("auth") {
            AuthScreen(onAuthSuccess = { token ->
                navController.navigate("message/$token") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable(
            "message/{token}",
            arguments = listOf(
                androidx.navigation.navArgument("token") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            MessageScreen(sessionToken = token)
        }
    }
}
