package com.methil.aiko.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.methil.aiko.ui.screens.auth.AuthScreen
import com.methil.aiko.ui.screens.main.CharactersScreen
import com.methil.aiko.ui.screens.main.MainScreen
import com.methil.aiko.domain.Character
import com.methil.aiko.ui.screens.chat.MessageScreen
import com.methil.aiko.ui.screens.auth.OnboardingScreen
import com.methil.aiko.ui.screens.auth.SplashScreen
import com.methil.aiko.ui.viewmodels.MainViewModel
import com.methil.aiko.ui.viewmodels.MessageViewModel
import com.methil.aiko.ui.viewmodels.ProjectViewModelFactory

import com.methil.aiko.data.TokenManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AikoNavigation(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

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
                navArgument("token") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            MainScreen(
                sessionToken = token,
                mainViewModel = mainViewModel,
                onCharacterSelect = { character ->
                    navController.navigate("message/$token/${character.id}")
                },
                onLogout = {
                    tokenManager.clearToken()
                    navController.navigate("auth") {
                        popUpTo("main/$token") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "message/{token}/{characterId}",
            arguments = listOf(
                navArgument("token") { type = NavType.StringType },
                navArgument("characterId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            val characterId = backStackEntry.arguments?.getInt("characterId") ?: 0
            val currentContext = LocalContext.current
            val messageViewModel: MessageViewModel = viewModel(
                factory = ProjectViewModelFactory(currentContext)
            )
            MessageScreen(
                sessionToken = token,
                characterId = characterId,
                onBack = { navController.popBackStack() },
                viewModel = messageViewModel
            )
        }
    }
}
