package com.methil.aiko.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AikoScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Characters : AikoScreen("characters", "Characters", Icons.Default.List)
    object Profile : AikoScreen("profile", "Profile", Icons.Default.Person)
    object Settings : AikoScreen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(
            Characters,
            Profile,
            Settings
        )
    }
}
