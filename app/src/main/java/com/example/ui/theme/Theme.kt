package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VoiceFinderColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = ElectricCyan,
    secondary = ElectricBlue,
    onSecondary = TextPrimary,
    secondaryContainer = Navy600,
    onSecondaryContainer = TextPrimary,
    tertiary = SuccessGreen,
    onTertiary = Navy900,
    background = Navy900,
    onBackground = TextPrimary,
    surface = Navy800,
    onSurface = TextPrimary,
    surfaceVariant = Navy700,
    onSurfaceVariant = TextSecondary,
    outline = Navy600,
    error = AlertRed,
    onError = TextPrimary
)

@Composable
fun VoiceFinderLockTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Navy900.toArgb()
                window.navigationBarColor = Navy900.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = VoiceFinderColorScheme,
        typography = Typography,
        content = content
    )
}
