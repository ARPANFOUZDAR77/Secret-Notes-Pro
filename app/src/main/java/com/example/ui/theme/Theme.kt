package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = SurfaceGray,
    tertiary = NeonBlue,
    background = DeepBlack,
    surface = DarkGray,
    onPrimary = DeepBlack,
    onSecondary = LightText,
    onTertiary = DeepBlack,
    onBackground = LightText,
    onSurface = LightText,
    error = ErrorRed,
    onError = DeepBlack
)

private val BrightDarkColorScheme = darkColorScheme(
    primary = BrightDarkPrimary,
    secondary = BrightDarkSurface,
    tertiary = BrightDarkPrimary,
    background = BrightDarkBackground,
    surface = BrightDarkSurface,
    onPrimary = BrightDarkOnPrimary,
    onSecondary = BrightDarkText,
    onTertiary = BrightDarkOnPrimary,
    onBackground = BrightDarkText,
    onSurface = BrightDarkText,
    error = ErrorRed,
    onError = DeepBlack
)

@Composable
fun MyApplicationTheme(
    appTheme: String = "Cyber",
    darkTheme: Boolean = true, // Force dark theme
    dynamicColor: Boolean = false, // Disable dynamic colors
    content: @Composable () -> Unit,
) {
    val colorScheme = when(appTheme) {
        "BrightDark" -> BrightDarkColorScheme
        else -> CyberColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
