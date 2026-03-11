package com.methil.aiko.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val SquareShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = PinkAccent,
    secondary = LightViolet,
    tertiary = LightestPink,
    background = DarkPurple,
    surface = DarkPurple,
    onPrimary = Color.White,
    onSecondary = DarkPurple,
    onTertiary = DarkPurple,
    onBackground = LightestPink,
    onSurface = LightestPink
)

private val LightColorScheme = lightColorScheme(
    primary = PinkAccent,
    secondary = DarkPurple,
    tertiary = LightestPink,
    background = LightViolet,
    surface = LightestPink,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = DarkPurple,
    onBackground = DarkVioletText,
    onSurface = DarkVioletText
)

@Composable
fun AikoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to maintain Figma design consistency
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as Activity).window
            window.navigationBarColor = LightViolet.toArgb()
            window.statusBarColor = LightViolet.toArgb()
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SquareShapes,
        content = content
    )
}
