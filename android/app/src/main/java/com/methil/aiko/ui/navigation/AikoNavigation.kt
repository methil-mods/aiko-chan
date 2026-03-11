package com.methil.aiko.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.methil.aiko.ui.screens.auth.AuthScreen
import com.methil.aiko.ui.screens.main.MainScreen
import com.methil.aiko.ui.screens.chat.MessageScreen
import com.methil.aiko.ui.screens.auth.OnboardingScreen
import com.methil.aiko.ui.screens.auth.SplashScreen

@Composable
fun AikoNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val tokenManager = com.methil.aiko.data.TokenManager(context)
            SplashScreen(onSplashFinished = {
                val savedToken = tokenManager.getToken()
                if (savedToken != null) {
                    navController.navigate("main/$savedToken") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("onboarding") {
                        popUpTo("splash") { inclusive = true }
                    }
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
                navController.navigate("main/$token") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable(
            "main/{token}",
            arguments = listOf(
                androidx.navigation.navArgument("token") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            MainScreen(sessionToken = token)
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
