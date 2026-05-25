package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = Color.Black,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    tertiary = AlertAmber,
    background = DarkNavy,
    surface = DarkCharcoal,
    onBackground = PureWhite,
    onSurface = PureWhite,
    surfaceVariant = CardSlate,
    onSurfaceVariant = GreyText,
    error = ErrorCrimson
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme for the absolute gaming feel
    dynamicColor: Boolean = false, // Disable dynamic colors to maintain dark brand identity
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
