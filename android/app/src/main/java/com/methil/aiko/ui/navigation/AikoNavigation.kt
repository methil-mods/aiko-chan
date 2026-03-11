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
            SplashScreen(onSplashFinished = { validatedToken ->
                if (validatedToken != null) {
                    navController.navigate("main/$validatedToken") {
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
            MainScreen(
                sessionToken = token,
                onCharacterSelect = { character ->
                    // For now we just go to /message/{token}, but we should probably pass the character ID too
                    // Let's keep it simple as requested: "afficher la page de chat mais avec elle"
                    navController.navigate("message/$token")
                }
            )
        }
        composable(
            "message/{token}",
            arguments = listOf(
                androidx.navigation.navArgument("token") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            MessageScreen(
                sessionToken = token,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
