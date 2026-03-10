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
                // TODO: Store token in DataStore or similar
                navController.navigate("name_input") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable("name_input") {
            com.methil.aiko.ui.screens.NameInputScreen(onNameSubmitted = { name ->
                navController.navigate("message/$name") {
                    popUpTo("name_input") { inclusive = true }
                }
            })
        }
        composable(
            "message/{userName}",
            arguments = listOf(androidx.navigation.navArgument("userName") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: "Utilisateur"
            MessageScreen(userName = userName)
        }
    }
}
